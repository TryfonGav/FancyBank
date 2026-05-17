import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import net.miginfocom.swing.MigLayout;

public class Main {
    // Application constants
    public static final String APP_NAME = "FancyBank";
    public static final String APP_VERSION = "1.0.0";

    // Default application settings
    private static boolean defaultDarkMode = true;

    public static void main(String[] args) {
        AppUi.installLookAndFeel(defaultDarkMode);

        // Create application directories if they don't exist
        initializeDirectories();

        // Load any application settings
        loadSettings();

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

    private static void showSplashScreen() {
        // Create and display a splash screen for 2 seconds
        JWindow splashScreen = new JWindow();
        JPanel content = new JPanel(new MigLayout(
                "insets 32, fill",
                "[grow, center]",
                "[]14[]10[]24[]"
        ));
        content.putClientProperty("FlatLaf.style", "arc: 14; border: 1,1,1,1,#94A3B8,,14");

        JLabel title = new JLabel(APP_NAME, SwingConstants.CENTER);
        title.putClientProperty("FlatLaf.styleClass", "h1");
        title.setForeground(AppUi.BRAND_BLUE);

        JLabel version = new JLabel("Version " + APP_VERSION, SwingConstants.CENTER);
        version.putClientProperty("FlatLaf.styleClass", "large");

        JLabel loading = new JLabel("Loading...", SwingConstants.CENTER);
        loading.putClientProperty("FlatLaf.styleClass", "small");

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);

        content.add(title, "growx, wrap");
        content.add(version, "growx, wrap");
        content.add(loading, "growx, wrap");
        content.add(progress, "w 260!, growx");

        splashScreen.setContentPane(content);
        splashScreen.pack();
        splashScreen.setLocationRelativeTo(null);
        splashScreen.setVisible(true);

        // Close the splash screen after 2 seconds
        Timer timer = new Timer(2000, e -> splashScreen.dispose());
        timer.setRepeats(false);
        timer.start();
    }

    // Static utility methods that might be useful throughout the application

    /**
     * Formats a money value as currency with proper commas and decimal places
     */
    public static String formatCurrency(BigDecimal amount) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
        currency.setMinimumFractionDigits(2);
        currency.setMaximumFractionDigits(2);
        return currency.format(UserManager.money(amount));
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
