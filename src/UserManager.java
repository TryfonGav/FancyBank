import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserManager {
    private static final String DATA_FILE = "users.dat";
    private static Map<String, String[]> users = new HashMap<>();

    static {
        loadFromFile();
    }
    public static List<String> getAllUsers() {
        return new ArrayList<>(users.keySet());
    }

    @SuppressWarnings("unchecked")
    private static void loadFromFile() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            users = new HashMap<>();
            return;
        }
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                users = (Map<String, String[]>) obj;
            } else {
                users = new HashMap<>();
                System.err.println("Invalid data format. Starting fresh.");
            }
        } catch (IOException | ClassNotFoundException e) {
            users = new HashMap<>();
            System.err.println("Failed to load user data: " + e.getMessage());
        }
    }

    private static void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.err.println("Error saving user data: " + e.getMessage());
        }
    }

    public static synchronized boolean userExists(String username) {
        return users.containsKey(username);
    }

    public static void registerUser(String username, String pin, boolean isAdmin) {
        users.put(username, new String[]{"0.0", pin, String.valueOf(isAdmin)});
        saveUsers();
    }

    public static synchronized boolean validateUser(String username, String pin) {
        if (users.containsKey(username)) {
            String[] data = users.get(username);
            return data[1].equals(pin);
        }
        return false;
    }

    public static synchronized boolean isAdmin(String username) {
        if (users.containsKey(username)) {
            String[] data = users.get(username);
            return Boolean.parseBoolean(data[2]);
        }
        return false;
    }

    public static synchronized double getBalance(String username) {
        if (users.containsKey(username)) {
            String[] data = users.get(username);
            return Double.parseDouble(data[0]);
        }
        return 0.0;
    }

    public static synchronized void updateBalance(String username, double newBalance) {
        if (users.containsKey(username)) {
            String[] data = users.get(username);
            data[0] = String.valueOf(newBalance);
            saveUsers();
        }
    }

    public static synchronized void promoteToAdmin(String username) {
        if (users.containsKey(username)) {
            String[] data = users.get(username);
            data[2] = "true";
            saveUsers();
        }
    }

    public static synchronized Map<String, String[]> getUsers() {
        return users;
    }
}