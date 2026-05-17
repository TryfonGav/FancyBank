import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransactionRecord {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "^\\[(?<timestamp>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})]\\s+" +
                    "(?<type>Deposit|Withdrawal):\\s+\\$(?<amount>[0-9,]+(?:\\.[0-9]{1,2})?)" +
                    "(?:\\s+-\\s+Balance:\\s+\\$(?<balance>[0-9,]+(?:\\.[0-9]{1,2})?))?$"
    );

    private final String type;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    private final BigDecimal balanceAfter;

    public TransactionRecord(String type, BigDecimal amount, LocalDateTime timestamp, BigDecimal balanceAfter) {
        if (!"Deposit".equals(type) && !"Withdrawal".equals(type)) {
            throw new IllegalArgumentException("Unsupported transaction type.");
        }
        this.type = type;
        this.amount = money(amount);
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        this.balanceAfter = balanceAfter == null ? null : money(balanceAfter);
    }

    public TransactionRecord(String type, BigDecimal amount, BigDecimal balanceAfter) {
        this(type, amount, LocalDateTime.now(), balanceAfter);
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getTimestamp() {
        return timestamp.format(FORMATTER);
    }

    public LocalDateTime getTimestampDate() {
        return timestamp;
    }

    public Optional<BigDecimal> getBalanceAfter() {
        return Optional.ofNullable(balanceAfter);
    }

    public String toLogLine() {
        if (balanceAfter == null) {
            return String.format("[%s] %s: %s", getTimestamp(), type, formatMoney(amount));
        }
        return String.format("[%s] %s: %s - Balance: %s",
                getTimestamp(), type, formatMoney(amount), formatMoney(balanceAfter));
    }

    @Override
    public String toString() {
        return toLogLine();
    }

    public static TransactionRecord fromString(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        Matcher matcher = LOG_PATTERN.matcher(line.trim());
        if (!matcher.matches()) {
            return null;
        }

        try {
            LocalDateTime timestamp = LocalDateTime.parse(matcher.group("timestamp"), FORMATTER);
            BigDecimal amount = parseMoney(matcher.group("amount"));
            String balanceText = matcher.group("balance");
            BigDecimal balance = balanceText == null ? null : parseMoney(balanceText);
            return new TransactionRecord(matcher.group("type"), amount, timestamp, balance);
        } catch (DateTimeParseException | NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal parseMoney(String value) {
        return money(new BigDecimal(value.replace(",", "")));
    }

    private static BigDecimal money(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Money value cannot be null.");
        }
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    private static String formatMoney(BigDecimal value) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
        currency.setMinimumFractionDigits(2);
        currency.setMaximumFractionDigits(2);
        return currency.format(money(value));
    }
}
