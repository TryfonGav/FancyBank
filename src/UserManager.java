import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class UserManager {
    private static final Path DATA_DIR = Paths.get("data").toAbsolutePath().normalize();
    private static final Path USERS_FILE = DATA_DIR.resolve("users.json").normalize();
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,32}");
    private static final Pattern PIN_PATTERN = Pattern.compile("\\d{4,12}");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PBKDF2_ITERATIONS = 310_000;
    private static final int PBKDF2_KEY_LENGTH_BITS = 256;
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    private static Map<String, UserRecord> users = new LinkedHashMap<>();

    static {
        loadFromFile();
        bootstrapAdminFromEnvironment();
    }

    private UserManager() {
    }

    public static synchronized List<String> getAllUsers() {
        List<String> names = new ArrayList<>(users.keySet());
        names.sort(Comparator.naturalOrder());
        return names;
    }

    public static synchronized boolean userExists(String username) {
        String safeUsername = validateUsername(username);
        return users.containsKey(safeUsername);
    }

    public static synchronized void registerUser(String username, char[] pin) {
        String safeUsername = validateUsername(username);
        validatePin(pin);
        if (users.containsKey(safeUsername)) {
            throw new IllegalArgumentException("Username already exists.");
        }

        users.put(safeUsername, UserRecord.create(safeUsername, pin, false));
        saveUsers();
    }

    public static synchronized boolean bootstrapAdmin(String username, char[] pin) {
        String safeUsername = validateUsername(username);
        validatePin(pin);
        if (hasAdmin()) {
            return false;
        }
        if (users.containsKey(safeUsername)) {
            throw new IllegalArgumentException("Bootstrap username already exists.");
        }

        users.put(safeUsername, UserRecord.create(safeUsername, pin, true));
        saveUsers();
        return true;
    }

    public static synchronized boolean validateUser(String username, char[] pin) {
        String safeUsername = validateUsername(username);
        UserRecord record = users.get(safeUsername);
        if (record == null || pin == null || pin.length == 0) {
            return false;
        }
        return record.matchesPin(pin);
    }

    public static synchronized boolean isAdmin(String username) {
        String safeUsername = validateUsername(username);
        UserRecord record = users.get(safeUsername);
        return record != null && record.admin;
    }

    public static synchronized BigDecimal getBalance(String username) {
        String safeUsername = validateUsername(username);
        UserRecord record = users.get(safeUsername);
        return record == null ? money(BigDecimal.ZERO) : record.balance;
    }

    public static synchronized void updateBalance(String username, BigDecimal newBalance) {
        String safeUsername = validateUsername(username);
        UserRecord record = users.get(safeUsername);
        if (record == null) {
            throw new IllegalArgumentException("Unknown user.");
        }
        record.balance = money(newBalance);
        saveUsers();
    }

    public static synchronized void promoteToAdmin(String username) {
        String safeUsername = validateUsername(username);
        UserRecord record = users.get(safeUsername);
        if (record == null) {
            throw new IllegalArgumentException("Unknown user.");
        }
        record.admin = true;
        saveUsers();
    }

    public static synchronized Map<String, UserSnapshot> getUsers() {
        Map<String, UserSnapshot> snapshots = new LinkedHashMap<>();
        for (Map.Entry<String, UserRecord> entry : users.entrySet()) {
            UserRecord record = entry.getValue();
            snapshots.put(entry.getKey(), new UserSnapshot(entry.getKey(), record.balance, record.admin));
        }
        return Collections.unmodifiableMap(snapshots);
    }

    public static Path getHistoryFile(String username) {
        String safeUsername = validateUsername(username);
        Path path = DATA_DIR.resolve(safeUsername + "_history.txt").normalize();
        if (!path.startsWith(DATA_DIR)) {
            throw new SecurityException("Resolved history path escaped the data directory.");
        }
        return path;
    }

    public static Path getDataDirectory() {
        return DATA_DIR;
    }

    public static BigDecimal money(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Money value cannot be null.");
        }
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static String validateUsername(String username) {
        if (username == null) {
            throw new IllegalArgumentException("Username is required.");
        }
        String safeUsername = username.trim();
        if (!USERNAME_PATTERN.matcher(safeUsername).matches()
                || safeUsername.contains("..")
                || safeUsername.startsWith(".")
                || safeUsername.endsWith(".")) {
            throw new IllegalArgumentException("Username must be 3-32 characters using letters, digits, dot, dash, or underscore.");
        }
        return safeUsername;
    }

    private static void validatePin(char[] pin) {
        if (pin == null || !PIN_PATTERN.matcher(CharBuffer.wrap(pin)).matches()) {
            throw new IllegalArgumentException("PIN must contain 4-12 digits.");
        }
    }

    private static boolean hasAdmin() {
        for (UserRecord record : users.values()) {
            if (record.admin) {
                return true;
            }
        }
        return false;
    }

    private static void bootstrapAdminFromEnvironment() {
        if (hasAdmin()) {
            return;
        }

        String username = firstNonBlank(
                System.getProperty("fancybank.bootstrap.admin"),
                System.getenv("FANCYBANK_BOOTSTRAP_ADMIN")
        );
        String pinValue = firstNonBlank(
                System.getProperty("fancybank.bootstrap.pin"),
                System.getenv("FANCYBANK_BOOTSTRAP_PIN")
        );

        if (username == null || pinValue == null) {
            return;
        }

        char[] pin = pinValue.toCharArray();
        try {
            bootstrapAdmin(username, pin);
        } finally {
            Arrays.fill(pin, '\0');
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first.trim();
        }
        if (second != null && !second.trim().isEmpty()) {
            return second.trim();
        }
        return null;
    }

    private static void loadFromFile() {
        users = new LinkedHashMap<>();
        if (!Files.exists(USERS_FILE)) {
            return;
        }

        try {
            String json = Files.readString(USERS_FILE, StandardCharsets.UTF_8);
            for (Map<String, String> object : parseUserObjects(json)) {
                UserRecord record = UserRecord.fromJsonObject(object);
                users.put(record.username, record);
            }
        } catch (IOException | RuntimeException ex) {
            users = new LinkedHashMap<>();
            System.err.println("Failed to load user store: " + ex.getMessage());
        }
    }

    private static void saveUsers() {
        try {
            Files.createDirectories(DATA_DIR);
            Path tempFile = Files.createTempFile(DATA_DIR, "users", ".tmp");
            Files.writeString(tempFile, toJson(), StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, USERS_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile, USERS_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Error saving user data.", ex);
        }
    }

    private static String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"version\": 1,\n");
        json.append("  \"users\": [\n");
        int index = 0;
        for (UserRecord record : users.values()) {
            if (index++ > 0) {
                json.append(",\n");
            }
            json.append("    {\n");
            json.append("      \"username\": \"").append(escapeJson(record.username)).append("\",\n");
            json.append("      \"balance\": \"").append(record.balance.toPlainString()).append("\",\n");
            json.append("      \"pinSalt\": \"").append(escapeJson(record.pinSalt)).append("\",\n");
            json.append("      \"pinHash\": \"").append(escapeJson(record.pinHash)).append("\",\n");
            json.append("      \"iterations\": ").append(record.iterations).append(",\n");
            json.append("      \"admin\": ").append(record.admin).append("\n");
            json.append("    }");
        }
        json.append("\n  ]\n");
        json.append("}\n");
        return json.toString();
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }

    private static List<Map<String, String>> parseUserObjects(String json) {
        int usersKey = json.indexOf("\"users\"");
        if (usersKey < 0) {
            return Collections.emptyList();
        }
        int arrayStart = json.indexOf('[', usersKey);
        if (arrayStart < 0) {
            throw new IllegalArgumentException("Missing users array.");
        }
        int arrayEnd = findMatching(json, arrayStart, '[', ']');
        String array = json.substring(arrayStart + 1, arrayEnd);

        List<Map<String, String>> objects = new ArrayList<>();
        int index = 0;
        while (index < array.length()) {
            char c = array.charAt(index);
            if (Character.isWhitespace(c) || c == ',') {
                index++;
                continue;
            }
            if (c != '{') {
                throw new IllegalArgumentException("Invalid users array.");
            }
            int objectEnd = findMatching(array, index, '{', '}');
            objects.add(parseFlatObject(array.substring(index + 1, objectEnd)));
            index = objectEnd + 1;
        }
        return objects;
    }

    private static Map<String, String> parseFlatObject(String object) {
        Map<String, String> values = new LinkedHashMap<>();
        int index = 0;
        while (index < object.length()) {
            index = skipWhitespaceAndCommas(object, index);
            if (index >= object.length()) {
                break;
            }
            ParsedString key = parseJsonString(object, index);
            index = skipWhitespace(object, key.nextIndex);
            if (index >= object.length() || object.charAt(index) != ':') {
                throw new IllegalArgumentException("Invalid JSON object.");
            }
            index = skipWhitespace(object, index + 1);

            String value;
            if (object.charAt(index) == '"') {
                ParsedString parsedValue = parseJsonString(object, index);
                value = parsedValue.value;
                index = parsedValue.nextIndex;
            } else {
                int start = index;
                while (index < object.length() && object.charAt(index) != ',' && object.charAt(index) != '}') {
                    index++;
                }
                value = object.substring(start, index).trim();
            }
            values.put(key.value, value);
        }
        return values;
    }

    private static ParsedString parseJsonString(String text, int start) {
        if (text.charAt(start) != '"') {
            throw new IllegalArgumentException("Expected JSON string.");
        }
        StringBuilder value = new StringBuilder();
        int index = start + 1;
        while (index < text.length()) {
            char c = text.charAt(index++);
            if (c == '"') {
                return new ParsedString(value.toString(), index);
            }
            if (c != '\\') {
                value.append(c);
                continue;
            }
            if (index >= text.length()) {
                throw new IllegalArgumentException("Invalid JSON escape.");
            }
            char escape = text.charAt(index++);
            switch (escape) {
                case '"':
                case '\\':
                case '/':
                    value.append(escape);
                    break;
                case 'b':
                    value.append('\b');
                    break;
                case 'f':
                    value.append('\f');
                    break;
                case 'n':
                    value.append('\n');
                    break;
                case 'r':
                    value.append('\r');
                    break;
                case 't':
                    value.append('\t');
                    break;
                case 'u':
                    if (index + 4 > text.length()) {
                        throw new IllegalArgumentException("Invalid unicode escape.");
                    }
                    value.append((char) Integer.parseInt(text.substring(index, index + 4), 16));
                    index += 4;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported JSON escape.");
            }
        }
        throw new IllegalArgumentException("Unterminated JSON string.");
    }

    private static int skipWhitespaceAndCommas(String text, int index) {
        while (index < text.length() && (Character.isWhitespace(text.charAt(index)) || text.charAt(index) == ',')) {
            index++;
        }
        return index;
    }

    private static int skipWhitespace(String text, int index) {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private static int findMatching(String text, int start, char open, char close) {
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        for (int index = start; index < text.length(); index++) {
            char c = text.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return index;
                }
            }
        }
        throw new IllegalArgumentException("Unbalanced JSON structure.");
    }

    public static final class UserSnapshot {
        private final String username;
        private final BigDecimal balance;
        private final boolean admin;

        private UserSnapshot(String username, BigDecimal balance, boolean admin) {
            this.username = username;
            this.balance = balance;
            this.admin = admin;
        }

        public String getUsername() {
            return username;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public boolean isAdmin() {
            return admin;
        }
    }

    private static final class UserRecord {
        private final String username;
        private BigDecimal balance;
        private final String pinSalt;
        private final String pinHash;
        private final int iterations;
        private boolean admin;

        private UserRecord(String username, BigDecimal balance, String pinSalt, String pinHash, int iterations, boolean admin) {
            this.username = validateUsername(username);
            this.balance = money(balance);
            this.pinSalt = pinSalt;
            this.pinHash = pinHash;
            this.iterations = iterations;
            this.admin = admin;
        }

        private static UserRecord create(String username, char[] pin, boolean admin) {
            byte[] salt = new byte[16];
            SECURE_RANDOM.nextBytes(salt);
            return new UserRecord(
                    username,
                    BigDecimal.ZERO,
                    Base64.getEncoder().encodeToString(salt),
                    Base64.getEncoder().encodeToString(hashPin(pin, salt, PBKDF2_ITERATIONS)),
                    PBKDF2_ITERATIONS,
                    admin
            );
        }

        private static UserRecord fromJsonObject(Map<String, String> object) {
            return new UserRecord(
                    object.get("username"),
                    new BigDecimal(object.getOrDefault("balance", "0.00")),
                    object.get("pinSalt"),
                    object.get("pinHash"),
                    Integer.parseInt(object.getOrDefault("iterations", String.valueOf(PBKDF2_ITERATIONS))),
                    Boolean.parseBoolean(object.getOrDefault("admin", "false"))
            );
        }

        private boolean matchesPin(char[] pin) {
            byte[] salt = Base64.getDecoder().decode(pinSalt);
            byte[] expected = Base64.getDecoder().decode(pinHash);
            byte[] actual = hashPin(pin, salt, iterations);
            try {
                return MessageDigest.isEqual(expected, actual);
            } finally {
                Arrays.fill(actual, (byte) 0);
            }
        }

        private static byte[] hashPin(char[] pin, byte[] salt, int iterations) {
            try {
                PBEKeySpec spec = new PBEKeySpec(pin, salt, iterations, PBKDF2_KEY_LENGTH_BITS);
                try {
                    return SecretKeyFactory.getInstance(PBKDF2_ALGORITHM).generateSecret(spec).getEncoded();
                } finally {
                    spec.clearPassword();
                }
            } catch (GeneralSecurityException ex) {
                throw new IllegalStateException("Unable to hash PIN.", ex);
            }
        }
    }

    private static final class ParsedString {
        private final String value;
        private final int nextIndex;

        private ParsedString(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }
}
