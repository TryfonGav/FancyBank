import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

public class AdminPanel extends JFrame {
    private JList<String> usersList;
    private JTextArea activityLog;
    private JLabel statusLabel;
    private JButton refreshButton;
    private JButton alertSettingsButton;
    private DefaultListModel<String> usersModel;
    private JPanel transactionPanel;
    private JPanel chartPanel;
    private JTabbedPane tabPane;
    private JTextField depositField;
    private JTextField withdrawalField;
    private JTextField freqCountField;
    private JTextField timeWindowField;
    private JTextField emailField;
    private Map<String, List<TransactionRecord>> allTransactions = new HashMap<>();

    private final Color brandBlue = AppUi.BRAND_BLUE;
    private final Color brandGold = AppUi.BRAND_GOLD;
    private final Color warningColor = AppUi.DANGER;

    private static BigDecimal LARGE_DEPOSIT_THRESHOLD = money("10000.00");
    private static BigDecimal LARGE_WITHDRAWAL_THRESHOLD = money("5000.00");
    private static int FREQUENT_TRANSACTION_COUNT = 5;
    private static int FREQUENT_TRANSACTION_HOURS = 24;

    public AdminPanel(String adminUsername) {
        super("FancyBank Admin Panel - " + adminUsername);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        setVisible(true);
        refreshData(false);
    }

    private void initComponents() {
        JPanel container = new JPanel(new MigLayout(
                "fill, insets 18, gap 14",
                "[grow, fill]",
                "[][grow, fill][]"
        ));
        setContentPane(container);

        JLabel titleLabel = new JLabel("FancyBank™ Administration");
        titleLabel.putClientProperty("FlatLaf.styleClass", "h1");
        titleLabel.setForeground(brandBlue);

        statusLabel = new JLabel("Preparing admin dashboard");
        statusLabel.putClientProperty("FlatLaf.styleClass", "medium");

        JPanel northPanel = new JPanel(new MigLayout("fillx, insets 0", "[][grow][]", "[]"));
        northPanel.add(titleLabel);
        northPanel.add(statusLabel, "right");

        tabPane = new JTabbedPane();
        tabPane.addTab("Users", null, createUsersPanel(), "Monitor registered users");
        tabPane.addTab("Activity Log", null, createActivityPanel(), "View system activity");
        tabPane.addTab("Alerts", null, createAlertsPanel(), "Configure alert thresholds");

        JPanel southPanel = new JPanel(new MigLayout("fillx, insets 0, gap 10", "[grow][][120!][120!][120!]", "[]"));

        refreshButton = AppUi.primaryButton("Refresh");
        refreshButton.addActionListener(e -> refreshData(true));

        alertSettingsButton = AppUi.secondaryButton("Alerts");
        alertSettingsButton.addActionListener(e -> showAlertSettings());

        JButton closeButton = AppUi.secondaryButton("Close");
        closeButton.addActionListener(e -> dispose());

        southPanel.add(new JLabel(), "growx");
        southPanel.add(refreshButton, "growx");
        southPanel.add(alertSettingsButton, "growx");
        southPanel.add(closeButton, "growx");

        container.add(northPanel, "growx, wrap");
        container.add(tabPane, "grow, push, wrap");
        container.add(southPanel, "growx");
    }

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "fill, insets 14, gap 14",
                "[220::280, fill][grow, fill]",
                "[grow, fill]"
        ));

        JPanel leftPanel = AppUi.card();
        leftPanel.setLayout(new MigLayout("fill, insets 14", "[grow, fill]", "[][grow, fill]"));
        JLabel usersLabel = new JLabel("Registered Users:");
        usersLabel.putClientProperty("FlatLaf.styleClass", "h4");

        usersModel = new DefaultListModel<>();
        usersList = new JList<>(usersModel);

        JScrollPane usersScrollPane = new JScrollPane(usersList);
        leftPanel.add(usersLabel, "wrap");
        leftPanel.add(usersScrollPane, "grow, push");

        JPanel rightPanel = new JPanel(new MigLayout("fill, insets 0, gap 14", "[grow, fill]", "[][grow, fill][160!]"));

        transactionPanel = AppUi.card();
        transactionPanel.setLayout(new MigLayout("fill, insets 14", "[grow, fill]", "[][grow, fill]"));
        transactionPanel.add(AppUi.sectionTitle("User Transactions"), "wrap");

        JTextArea transactionArea = new JTextArea();
        transactionArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        transactionArea.setEditable(false);
        transactionPanel.add(new JScrollPane(transactionArea), "grow, push");

        chartPanel = AppUi.card();
        chartPanel.setLayout(new MigLayout("fill, insets 14", "[grow, fill]", "[][grow, fill]"));
        chartPanel.add(AppUi.sectionTitle("Activity Snapshot"), "wrap");
        chartPanel.add(new JLabel("Select a user to inspect transaction velocity."), "grow");

        JPanel userInfoPanel = AppUi.card();
        userInfoPanel.setLayout(new MigLayout("fillx, insets 14", "[grow, fill]", "[]12[]"));
        JLabel userInfoLabel = new JLabel("Select a user to view details");
        userInfoLabel.putClientProperty("FlatLaf.styleClass", "h3");
        userInfoPanel.add(userInfoLabel, "growx, wrap");

        JPanel userStatsPanel = new JPanel(new MigLayout("fillx, insets 0, gap 10", "[][grow, right]", "[][][]"));
        JLabel balanceLabel = new JLabel("Current Balance:");
        JLabel balanceValue = new JLabel("$0.00");
        JLabel transCountLabel = new JLabel("Transaction Count:");
        JLabel transCountValue = new JLabel("0");
        JLabel lastLoginLabel = new JLabel("Last Activity:");
        JLabel lastLoginValue = new JLabel("N/A");
        balanceValue.setForeground(brandGold);

        userStatsPanel.add(balanceLabel);
        userStatsPanel.add(balanceValue, "growx, wrap");
        userStatsPanel.add(transCountLabel);
        userStatsPanel.add(transCountValue, "growx, wrap");
        userStatsPanel.add(lastLoginLabel);
        userStatsPanel.add(lastLoginValue, "growx");
        userInfoPanel.add(userStatsPanel, "growx");

        rightPanel.add(userInfoPanel, "growx, wrap");
        rightPanel.add(transactionPanel, "grow, push, wrap");
        rightPanel.add(chartPanel, "growx");

        usersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = usersList.getSelectedValue();
                if (selectedUser != null) {
                    updateUserDetails(selectedUser, transactionArea, balanceValue, transCountValue, lastLoginValue);
                }
            }
        });

        panel.add(leftPanel, "grow, push");
        panel.add(rightPanel, "grow, push");
        return panel;
    }

    private JPanel createActivityPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 14, gap 14", "[grow, fill]", "[][grow, fill][]"));

        JPanel topPanel = new JPanel(new MigLayout("fillx, insets 0", "[][grow][]", "[]"));
        JLabel activityLabel = new JLabel("System Activity Log");
        activityLabel.putClientProperty("FlatLaf.styleClass", "h3");

        String[] filterOptions = {"All Activity", "Logins", "Deposits", "Withdrawals", "Alerts Only"};
        JComboBox<String> filterCombo = new JComboBox<>(filterOptions);

        JPanel filterPanel = new JPanel(new MigLayout("insets 0, gap 8", "[][]", "[]"));
        filterPanel.add(new JLabel("Filter:"));
        filterPanel.add(filterCombo);

        topPanel.add(activityLabel);
        topPanel.add(filterPanel, "right");

        activityLog = new JTextArea();
        activityLog.setEditable(false);
        activityLog.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JPanel buttonPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow][]", "[]"));
        JButton exportButton = AppUi.secondaryButton("Export Log");
        exportButton.addActionListener(e -> exportActivityLog());
        buttonPanel.add(new JLabel(), "growx");
        buttonPanel.add(exportButton);

        panel.add(topPanel, "growx, wrap");
        panel.add(new JScrollPane(activityLog), "grow, push, wrap");
        panel.add(buttonPanel, "growx");
        return panel;
    }

    private JPanel createAlertsPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 18, gap 14", "[grow, fill]", "[][grow, fill][]"));

        JPanel settingsPanel = AppUi.card();
        settingsPanel.setLayout(new MigLayout("fillx, insets 18, gap 12", "[][grow, fill]", "[][][][][]"));

        JLabel depositLabel = new JLabel("Large Deposit Threshold ($):");
        depositField = new JTextField(LARGE_DEPOSIT_THRESHOLD.toPlainString());
        ((AbstractDocument) depositField.getDocument()).setDocumentFilter(new DecimalInputFilter());

        JLabel withdrawalLabel = new JLabel("Large Withdrawal Threshold ($):");
        withdrawalField = new JTextField(LARGE_WITHDRAWAL_THRESHOLD.toPlainString());
        ((AbstractDocument) withdrawalField.getDocument()).setDocumentFilter(new DecimalInputFilter());

        JLabel freqCountLabel = new JLabel("Frequent Transaction Count:");
        freqCountField = new JTextField(String.valueOf(FREQUENT_TRANSACTION_COUNT));
        ((AbstractDocument) freqCountField.getDocument()).setDocumentFilter(new IntegerInputFilter());

        JLabel timeWindowLabel = new JLabel("Time Window (hours):");
        timeWindowField = new JTextField(String.valueOf(FREQUENT_TRANSACTION_HOURS));
        ((AbstractDocument) timeWindowField.getDocument()).setDocumentFilter(new IntegerInputFilter());

        JLabel emailLabel = new JLabel("Email for Notifications:");
        emailField = new JTextField("admin@fancybank.com");

        settingsPanel.add(depositLabel);
        settingsPanel.add(depositField, "growx, wrap");
        settingsPanel.add(withdrawalLabel);
        settingsPanel.add(withdrawalField, "growx, wrap");
        settingsPanel.add(freqCountLabel);
        settingsPanel.add(freqCountField, "growx, wrap");
        settingsPanel.add(timeWindowLabel);
        settingsPanel.add(timeWindowField, "growx, wrap");
        settingsPanel.add(emailLabel);
        settingsPanel.add(emailField, "growx");

        JButton testButton = AppUi.secondaryButton("Test Alert Notification");
        testButton.addActionListener(e -> showTestAlert());

        JPanel buttonPanel = new JPanel(new MigLayout("fillx, insets 0, gap 10", "[grow][][140!]", "[]"));
        JButton saveButton = AppUi.primaryButton("Save Settings");
        saveButton.addActionListener(e -> saveSettingsFromFields(panel));
        buttonPanel.add(new JLabel(), "growx");
        buttonPanel.add(testButton);
        buttonPanel.add(saveButton, "growx");

        JLabel header = new JLabel("Configure Alert Thresholds", SwingConstants.CENTER);
        header.putClientProperty("FlatLaf.styleClass", "h3");

        panel.add(header, "growx, wrap");
        panel.add(settingsPanel, "growx, pushy, wrap");
        panel.add(buttonPanel, "growx");
        return panel;
    }

    private void updateUserDetails(String username, JTextArea transactionArea, JLabel balanceValue, JLabel transCountValue, JLabel lastLoginValue) {
        transactionArea.setText("");
        List<TransactionRecord> transactions = allTransactions.getOrDefault(username, new ArrayList<>());

        if (transactions.isEmpty()) {
            transactionArea.setText("No transaction history available for this user.");
        } else {
            for (TransactionRecord record : transactions) {
                transactionArea.append(record.toString() + System.lineSeparator());
            }
        }

        balanceValue.setText(formatCurrency(UserManager.getBalance(username)));
        balanceValue.setForeground(brandGold);
        transCountValue.setText(String.valueOf(transactions.size()));
        lastLoginValue.setText(transactions.isEmpty() ? "No activity" : transactions.get(0).getTimestamp());
    }

    private void refreshData(boolean notifyOnCompletion) {
        refreshButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        statusLabel.setText("Starting background refresh...");
        new DataRefreshWorker(notifyOnCompletion).execute();
    }

    private void showAlertSettings() {
        tabPane.setSelectedIndex(2);
    }

    private void showTestAlert() {
        String testAlert = "TEST ALERT: This is a test security notification.\n" +
                "If this were a real alert, details about suspicious transactions would appear here.\n" +
                "You can configure alert thresholds in the settings panel.";
        showAlertNotification(testAlert);
    }

    private void showAlertNotification(String alertText) {
        JDialog alertDialog = new JDialog(this, "Security Alert", true);
        alertDialog.setSize(540, 340);
        alertDialog.setLocationRelativeTo(this);

        JPanel alertPanel = new JPanel(new MigLayout("fill, insets 18, gap 12", "[grow, fill]", "[][grow, fill][]"));

        JLabel alertLabel = new JLabel("Suspicious Activity Detected!");
        alertLabel.setIcon(AppUi.svgIcon(AppUi.ALERT_SVG, 28));
        alertLabel.setIconTextGap(10);
        alertLabel.putClientProperty("FlatLaf.styleClass", "h3");
        alertLabel.setForeground(warningColor);

        JTextArea alertDetails = new JTextArea(alertText);
        alertDetails.setEditable(false);
        alertDetails.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JButton acknowledgeButton = AppUi.primaryButton("Acknowledge");
        acknowledgeButton.addActionListener(e -> alertDialog.dispose());

        alertPanel.add(alertLabel, "growx, wrap");
        alertPanel.add(new JScrollPane(alertDetails), "grow, push, wrap");
        alertPanel.add(acknowledgeButton, "right, w 140!");

        alertDialog.add(alertPanel);
        alertDialog.setVisible(true);
    }

    private void saveSettingsFromFields(Component parent) {
        try {
            AlertSettings settings = new AlertSettings(
                    parsePositiveMoney(depositField.getText()),
                    parsePositiveMoney(withdrawalField.getText()),
                    parsePositiveInteger(freqCountField.getText()),
                    parsePositiveInteger(timeWindowField.getText())
            );
            applyAlertSettings(settings);
            saveAlertSettings(settings);
            JOptionPane.showMessageDialog(parent, "Alert settings saved successfully.", "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException | IOException ex) {
            JOptionPane.showMessageDialog(parent, ex.getMessage(), "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportActivityLog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Activity Log");

        try {
            Files.createDirectories(UserManager.getDataDirectory());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Unable to prepare export directory.", "Export Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        fileChooser.setSelectedFile(UserManager.getDataDirectory().resolve("FancyBank_ActivityLog_" + timestamp + ".txt").toFile());

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path file = fileChooser.getSelectedFile().toPath();
            String content = "FancyBank Activity Log - Generated: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) +
                    System.lineSeparator() +
                    "---------------------------------------------------------------" +
                    System.lineSeparator() + System.lineSeparator() +
                    activityLog.getText() +
                    System.lineSeparator() + System.lineSeparator() +
                    "--- End of Activity Log ---" +
                    System.lineSeparator();
            try {
                Files.writeString(file, content, StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this, "Activity log exported successfully to:\n" + file.toAbsolutePath(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error exporting activity log: " + ex.getMessage(),
                        "Export Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static String buildAlertReport(Map<String, List<TransactionRecord>> transactionsByUser, AlertSettings settings) {
        StringBuilder alertText = new StringBuilder();

        for (Map.Entry<String, List<TransactionRecord>> entry : transactionsByUser.entrySet()) {
            String username = entry.getKey();
            List<TransactionRecord> transactions = entry.getValue();

            for (TransactionRecord record : transactions) {
                if ("Deposit".equals(record.getType()) && record.getAmount().compareTo(settings.largeDepositThreshold) >= 0) {
                    alertText.append(String.format("ALERT: Large deposit of %s by %s on %s%n",
                            formatCurrency(record.getAmount()), username, record.getTimestamp()));
                }
                if ("Withdrawal".equals(record.getType()) && record.getAmount().compareTo(settings.largeWithdrawalThreshold) >= 0) {
                    alertText.append(String.format("ALERT: Large withdrawal of %s by %s on %s%n",
                            formatCurrency(record.getAmount()), username, record.getTimestamp()));
                }
            }

            if (transactions.size() >= settings.frequentTransactionCount) {
                LocalDateTime cutoff = LocalDateTime.now().minusHours(settings.frequentTransactionHours);
                long recentCount = transactions.stream()
                        .filter(record -> record.getTimestampDate().isAfter(cutoff))
                        .count();
                if (recentCount >= settings.frequentTransactionCount) {
                    alertText.append(String.format("ALERT: Frequent activity detected - %d transactions by %s in the last %d hours%n",
                            recentCount, username, settings.frequentTransactionHours));
                }
            }
        }

        return alertText.toString();
    }

    private static AlertSettings loadAlertSettings() {
        Path settingsFile = alertSettingsPath();
        AlertSettings defaults = new AlertSettings(
                LARGE_DEPOSIT_THRESHOLD,
                LARGE_WITHDRAWAL_THRESHOLD,
                FREQUENT_TRANSACTION_COUNT,
                FREQUENT_TRANSACTION_HOURS
        );
        if (!Files.exists(settingsFile)) {
            return defaults;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(settingsFile, StandardCharsets.UTF_8)) {
            properties.load(reader);
            return new AlertSettings(
                    parsePositiveMoney(properties.getProperty("LARGE_DEPOSIT_THRESHOLD", defaults.largeDepositThreshold.toPlainString())),
                    parsePositiveMoney(properties.getProperty("LARGE_WITHDRAWAL_THRESHOLD", defaults.largeWithdrawalThreshold.toPlainString())),
                    parsePositiveInteger(properties.getProperty("FREQUENT_TRANSACTION_COUNT", String.valueOf(defaults.frequentTransactionCount))),
                    parsePositiveInteger(properties.getProperty("FREQUENT_TRANSACTION_HOURS", String.valueOf(defaults.frequentTransactionHours)))
            );
        } catch (IOException | IllegalArgumentException ex) {
            return defaults;
        }
    }

    private static void saveAlertSettings(AlertSettings settings) throws IOException {
        Files.createDirectories(UserManager.getDataDirectory());
        Properties properties = new Properties();
        properties.setProperty("LARGE_DEPOSIT_THRESHOLD", settings.largeDepositThreshold.toPlainString());
        properties.setProperty("LARGE_WITHDRAWAL_THRESHOLD", settings.largeWithdrawalThreshold.toPlainString());
        properties.setProperty("FREQUENT_TRANSACTION_COUNT", String.valueOf(settings.frequentTransactionCount));
        properties.setProperty("FREQUENT_TRANSACTION_HOURS", String.valueOf(settings.frequentTransactionHours));
        try (Writer writer = Files.newBufferedWriter(alertSettingsPath(), StandardCharsets.UTF_8)) {
            properties.store(writer, "FancyBank alert settings");
        }
    }

    private static Path alertSettingsPath() {
        Path path = UserManager.getDataDirectory().resolve("alert_settings.properties").normalize();
        if (!path.startsWith(UserManager.getDataDirectory())) {
            throw new SecurityException("Resolved settings path escaped the data directory.");
        }
        return path;
    }

    private void applyAlertSettings(AlertSettings settings) {
        LARGE_DEPOSIT_THRESHOLD = settings.largeDepositThreshold;
        LARGE_WITHDRAWAL_THRESHOLD = settings.largeWithdrawalThreshold;
        FREQUENT_TRANSACTION_COUNT = settings.frequentTransactionCount;
        FREQUENT_TRANSACTION_HOURS = settings.frequentTransactionHours;

        if (depositField != null) {
            depositField.setText(settings.largeDepositThreshold.toPlainString());
            withdrawalField.setText(settings.largeWithdrawalThreshold.toPlainString());
            freqCountField.setText(String.valueOf(settings.frequentTransactionCount));
            timeWindowField.setText(String.valueOf(settings.frequentTransactionHours));
        }
    }

    private static BigDecimal parsePositiveMoney(String text) {
        BigDecimal value;
        try {
            value = new BigDecimal(text.trim().replace(",", ""));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Money fields must be valid decimal amounts.");
        }
        if (value.scale() > 2) {
            throw new IllegalArgumentException("Money fields cannot have more than two decimal places.");
        }
        value = value.setScale(2, RoundingMode.HALF_EVEN);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Money fields must be greater than zero.");
        }
        return value;
    }

    private static int parsePositiveInteger(String text) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value <= 0) {
                throw new IllegalArgumentException("Integer fields must be greater than zero.");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Integer fields must be whole numbers.");
        }
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_EVEN);
    }

    private static String formatCurrency(BigDecimal amount) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
        currency.setMinimumFractionDigits(2);
        currency.setMaximumFractionDigits(2);
        return currency.format(amount.setScale(2, RoundingMode.HALF_EVEN));
    }

    private class DataRefreshWorker extends SwingWorker<DataRefreshWorker.RefreshResult, String> {
        private final boolean notifyOnCompletion;

        private DataRefreshWorker(boolean notifyOnCompletion) {
            this.notifyOnCompletion = notifyOnCompletion;
        }

        @Override
        protected RefreshResult doInBackground() {
            publish("Loading alert settings...");
            AlertSettings settings = loadAlertSettings();

            publish("Fetching user list...");
            List<String> loadedUsers = UserManager.getAllUsers();

            publish("Loading transaction histories...");
            Map<String, List<TransactionRecord>> loadedTransactions = new HashMap<>();
            for (String username : loadedUsers) {
                publish("Processing data for: " + username);
                List<TransactionRecord> userTransactions = new ArrayList<>();
                Path historyFile = UserManager.getHistoryFile(username);

                if (Files.exists(historyFile)) {
                    try (BufferedReader reader = Files.newBufferedReader(historyFile, StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            TransactionRecord record = TransactionRecord.fromString(line);
                            if (record != null) {
                                userTransactions.add(record);
                            }
                        }
                    } catch (IOException ex) {
                        publish("Unable to read history for: " + username);
                    }
                }

                userTransactions.sort(Comparator.comparing(TransactionRecord::getTimestampDate).reversed());
                loadedTransactions.put(username, userTransactions);
            }

            publish("Analyzing for suspicious activity...");
            String alertReport = buildAlertReport(loadedTransactions, settings);
            return new RefreshResult(loadedUsers, loadedTransactions, settings, alertReport);
        }

        @Override
        protected void process(List<String> chunks) {
            statusLabel.setText(chunks.get(chunks.size() - 1));
        }

        @Override
        protected void done() {
            try {
                RefreshResult result = get();
                applyAlertSettings(result.settings);

                usersModel.clear();
                for (String user : result.users) {
                    usersModel.addElement(user);
                }
                allTransactions = result.transactions;

                activityLog.setText("");
                activityLog.append("Data refreshed at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + System.lineSeparator());

                if (!result.alertReport.isEmpty()) {
                    activityLog.append(System.lineSeparator());
                    activityLog.append("--- SUSPICIOUS ACTIVITY REPORT ---" + System.lineSeparator());
                    activityLog.append(result.alertReport);
                    activityLog.append("--------------------------------" + System.lineSeparator());
                    showAlertNotification(result.alertReport);
                }

                statusLabel.setText("Data refreshed at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                if (notifyOnCompletion) {
                    JOptionPane.showMessageDialog(AdminPanel.this, "Data refresh complete.", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                statusLabel.setText("Error refreshing data.");
                JOptionPane.showMessageDialog(AdminPanel.this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                refreshButton.setEnabled(true);
                setCursor(Cursor.getDefaultCursor());
            }
        }

        private class RefreshResult {
            private final List<String> users;
            private final Map<String, List<TransactionRecord>> transactions;
            private final AlertSettings settings;
            private final String alertReport;

            private RefreshResult(List<String> users, Map<String, List<TransactionRecord>> transactions,
                                  AlertSettings settings, String alertReport) {
                this.users = users;
                this.transactions = transactions;
                this.settings = settings;
                this.alertReport = alertReport;
            }
        }
    }

    private static final class AlertSettings {
        private final BigDecimal largeDepositThreshold;
        private final BigDecimal largeWithdrawalThreshold;
        private final int frequentTransactionCount;
        private final int frequentTransactionHours;

        private AlertSettings(BigDecimal largeDepositThreshold, BigDecimal largeWithdrawalThreshold,
                              int frequentTransactionCount, int frequentTransactionHours) {
            this.largeDepositThreshold = largeDepositThreshold;
            this.largeWithdrawalThreshold = largeWithdrawalThreshold;
            this.frequentTransactionCount = frequentTransactionCount;
            this.frequentTransactionHours = frequentTransactionHours;
        }
    }
}
