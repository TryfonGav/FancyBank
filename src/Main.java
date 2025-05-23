import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Main {
    // Application constants
    public static final String APP_NAME = "FancyBank";
    public static final String APP_VERSION = "1.0.0";

    // Default application settings
    private static boolean defaultDarkMode = true;

    public static void main(String[] args) {
        // Set the look and feel to the system default
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }

        // Create application directories if they don't exist
        initializeDirectories();

        // Load any application settings
        loadSettings();

        // Set up custom fonts
        setupFonts();

        // Create and display the login screen on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            showSplashScreen();
            new LoginScreen();
        });
    }

    private static void initializeDirectories() {
        // Create a data directory if it doesn't exist
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            if (dataDir.mkdir()) {
                System.out.println("Created data directory");
            } else {
                System.err.println("Failed to create data directory");
            }
        }

        // Could also create other directories (logs, exports, etc.)
    }

    private static void loadSettings() {
        // This could load from a properties file or database
        // For now we'll just use the defaults

        // Check if a settings file exists and load settings if it does
        File settingsFile = new File("data/settings.properties");
        if (settingsFile.exists()) {
            // Load settings from file - left as an exercise
            System.out.println("Loading settings from file");
        } else {
            System.out.println("Using default settings");
        }
    }

    private static void setupFonts() {
        //Future use for custom fonts
    }

    private static void showSplashScreen() {
        // Create and display a splash screen for 2 seconds
        JWindow splashScreen = new JWindow();
        splashScreen.setSize(400, 300);
        splashScreen.setLocationRelativeTo(null);

        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(BorderFactory.createLineBorder(new Color(0, 102, 204), 2));
        content.setBackground(new Color(40, 40, 40));

        JLabel title = new JLabel(APP_NAME, SwingConstants.CENTER);
        title.setFont(new Font("Serif", Font.BOLD, 36));
        title.setForeground(new Color(0, 102, 204));

        JLabel version = new JLabel("Version " + APP_VERSION, SwingConstants.CENTER);
        version.setFont(new Font("SansSerif", Font.PLAIN, 14));
        version.setForeground(Color.LIGHT_GRAY);

        JLabel loading = new JLabel("Loading...", SwingConstants.CENTER);
        loading.setFont(new Font("SansSerif", Font.ITALIC, 12));
        loading.setForeground(Color.LIGHT_GRAY);

        JPanel centerPanel = new JPanel(new GridLayout(3, 1));
        centerPanel.setBackground(new Color(40, 40, 40));
        centerPanel.add(title);
        centerPanel.add(version);
        centerPanel.add(loading);

        content.add(centerPanel, BorderLayout.CENTER);
        splashScreen.setContentPane(content);
        splashScreen.setVisible(true);

        // Close the splash screen after 2 seconds
        Timer timer = new Timer(2000, e -> splashScreen.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    // Static utility methods that might be useful throughout the application

    /**
     * Formats a double value as currency with proper commas and decimal places
     */
    public static String formatCurrency(double amount) {
        return String.format("$%,.2f", amount);
    }

    /**
     * Shows a standardized error dialog
     */
    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                APP_NAME + " - Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Shows a standardized information dialog
     */
    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent,
                message,
                APP_NAME + " - Information",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}