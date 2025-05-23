import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class AdminPanel extends JFrame {
    private JList<String> usersList;
    private JTextArea activityLog;
    private JLabel statusLabel;
    private JButton refreshButton;
    private JButton alertSettingsButton;
    private DefaultListModel<String> usersModel;
    private JScrollPane activityScrollPane;
    private JPanel transactionPanel;
    private JPanel chartPanel;
    private boolean darkMode = true;
    private final Color brandBlue = new Color(0, 102, 204);
    private final Color brandGold = new Color(204, 153, 0);
    private final Color warningColor = new Color(255, 102, 102);
    private Map<String, List<TransactionRecord>> allTransactions;
    private JTabbedPane tabPane;

    // Alert thresholds
    private static double LARGE_DEPOSIT_THRESHOLD = 10000.0;
    private static double LARGE_WITHDRAWAL_THRESHOLD = 5000.0;
    private static int FREQUENT_TRANSACTION_COUNT = 5;
    private static int FREQUENT_TRANSACTION_HOURS = 24;

    private JTextField depositField;
    private JTextField withdrawalField;
    private JTextField freqCountField;
    private JTextField timeWindowField;
    private JTextField emailField;

    public AdminPanel(String adminUsername) {
        super("FancyBank Admin Panel - " + adminUsername);
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        allTransactions = new HashMap<>();

        loadAlertSettings();  // Load settings on startup before UI init
        initComponents();
        loadAllUsers();
        loadAllTransactions();
        checkForSuspiciousActivity();

        setVisible(true);
    }

    private void initComponents() {
        Container container = getContentPane();
        container.setLayout(new BorderLayout());

        // North panel: title and admin info
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));
        if (darkMode) northPanel.setBackground(new Color(40, 40, 40));

        JLabel titleLabel = new JLabel("FancyBank™ Administration");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 28));
        titleLabel.setForeground(brandBlue);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if (darkMode) titlePanel.setBackground(new Color(40, 40, 40));
        titlePanel.add(titleLabel);

        statusLabel = new JLabel("Monitoring for suspicious activity");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        if (darkMode) statusLabel.setForeground(Color.LIGHT_GRAY);

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (darkMode) statusPanel.setBackground(new Color(40, 40, 40));
        statusPanel.add(statusLabel);

        northPanel.add(titlePanel, BorderLayout.WEST);
        northPanel.add(statusPanel, BorderLayout.EAST);

        // Center panel with tabbed interface
        tabPane = new JTabbedPane();
        if (darkMode) {
            tabPane.setBackground(new Color(50, 50, 50));
            tabPane.setForeground(Color.WHITE);
        }

        // Users panel
        JPanel usersPanel = createUsersPanel();
        tabPane.addTab("Users", null, usersPanel, "Monitor registered users");

        // Activity panel
        JPanel activityPanel = createActivityPanel();
        tabPane.addTab("Activity Log", null, activityPanel, "View system activity");

        // Alerts panel
        JPanel alertsPanel = createAlertsPanel();
        tabPane.addTab("Alerts", null, alertsPanel, "Configure alert thresholds");

        // South panel: controls
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        if (darkMode) southPanel.setBackground(new Color(40, 40, 40));

        refreshButton = new SmoothButton("Refresh Data", brandBlue, brandBlue, brandBlue.darker(), new Font("SansSerif", Font.BOLD, 14));
        refreshButton.addActionListener(e -> refreshData());

        alertSettingsButton = new SmoothButton("Alert Settings", brandBlue, brandBlue, brandBlue.darker(), new Font("SansSerif", Font.BOLD, 14));
        alertSettingsButton.addActionListener(e -> showAlertSettings());

        SmoothButton closeButton = new SmoothButton("Close Panel", brandBlue, new Color(100, 100, 100), new Color(70, 70, 70), new Font("SansSerif", Font.BOLD, 14));
        closeButton.addActionListener(e -> dispose());

        southPanel.add(refreshButton);
        southPanel.add(alertSettingsButton);
        southPanel.add(closeButton);

        // Add components to container
        container.add(northPanel, BorderLayout.NORTH);
        container.add(tabPane, BorderLayout.CENTER);
        container.add(southPanel, BorderLayout.SOUTH);

        // Apply dark mode to all components
        if (darkMode) {
            applyDarkMode(container);
        }
    }

    private JPanel createUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        if (darkMode) panel.setBackground(new Color(40, 40, 40));

        // Left side: user list
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 5));
        if (darkMode) leftPanel.setBackground(new Color(40, 40, 40));

        JLabel usersLabel = new JLabel("Registered Users:");
        usersLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        if (darkMode) usersLabel.setForeground(Color.WHITE);

        usersModel = new DefaultListModel<>();
        usersList = new JList<>(usersModel);
        usersList.setFont(new Font("SansSerif", Font.PLAIN, 14));
        if (darkMode) {
            usersList.setBackground(new Color(60, 60, 60));
            usersList.setForeground(Color.WHITE);
        }

        JScrollPane usersScrollPane = new JScrollPane(usersList);
        usersScrollPane.setPreferredSize(new Dimension(200, 500));

        leftPanel.add(usersLabel, BorderLayout.NORTH);
        leftPanel.add(usersScrollPane, BorderLayout.CENTER);

        // Right side: user details and transactions
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 10));
        if (darkMode) rightPanel.setBackground(new Color(40, 40, 40));

        transactionPanel = new JPanel(new BorderLayout());
        transactionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "User Transactions", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.BOLD, 14),
                darkMode ? Color.WHITE : Color.BLACK));
        if (darkMode) transactionPanel.setBackground(new Color(40, 40, 40));

        JTextArea transactionArea = new JTextArea();
        transactionArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        transactionArea.setEditable(false);
        if (darkMode) {
            transactionArea.setBackground(new Color(60, 60, 60));
            transactionArea.setForeground(Color.WHITE);
            transactionArea.setCaretColor(Color.WHITE);
        }

        JScrollPane transactionScroll = new JScrollPane(transactionArea);
        transactionPanel.add(transactionScroll, BorderLayout.CENTER);

        chartPanel = new JPanel(new BorderLayout());
        chartPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                "Activity Chart", TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("SansSerif", Font.BOLD, 14),
                darkMode ? Color.WHITE : Color.BLACK));
        if (darkMode) chartPanel.setBackground(new Color(40, 40, 40));
        chartPanel.setPreferredSize(new Dimension(400, 200));

        JPanel userInfoPanel = new JPanel(new BorderLayout());
        if (darkMode) userInfoPanel.setBackground(new Color(40, 40, 40));

        JLabel userInfoLabel = new JLabel("Select a user to view details");
        userInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        if (darkMode) userInfoLabel.setForeground(Color.WHITE);
        userInfoPanel.add(userInfoLabel, BorderLayout.NORTH);

        JPanel userStatsPanel = new JPanel(new GridLayout(3, 2, 10, 5));
        if (darkMode) userStatsPanel.setBackground(new Color(40, 40, 40));

        JLabel balanceLabel = new JLabel("Current Balance:");
        JLabel balanceValue = new JLabel("$0.00");
        JLabel transCountLabel = new JLabel("Transaction Count:");
        JLabel transCountValue = new JLabel("0");
        JLabel lastLoginLabel = new JLabel("Last Activity:");
        JLabel lastLoginValue = new JLabel("N/A");

        if (darkMode) {
            balanceLabel.setForeground(Color.WHITE);
            balanceValue.setForeground(brandGold);
            transCountLabel.setForeground(Color.WHITE);
            transCountValue.setForeground(Color.WHITE);
            lastLoginLabel.setForeground(Color.WHITE);
            lastLoginValue.setForeground(Color.WHITE);
        }

        userStatsPanel.add(balanceLabel);
        userStatsPanel.add(balanceValue);
        userStatsPanel.add(transCountLabel);
        userStatsPanel.add(transCountValue);
        userStatsPanel.add(lastLoginLabel);
        userStatsPanel.add(lastLoginValue);

        userInfoPanel.add(userStatsPanel, BorderLayout.CENTER);

        rightPanel.add(userInfoPanel, BorderLayout.NORTH);
        rightPanel.add(transactionPanel, BorderLayout.CENTER);
        rightPanel.add(chartPanel, BorderLayout.SOUTH);

        // Add user selection listener
        usersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = usersList.getSelectedValue();
                if (selectedUser != null) {
                    updateUserDetails(selectedUser, transactionArea, balanceValue, transCountValue, lastLoginValue);
                }
            }
        });

        // Add components to main panel
        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createActivityPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        if (darkMode) panel.setBackground(new Color(40, 40, 40));

        JPanel topPanel = new JPanel(new BorderLayout());
        if (darkMode) topPanel.setBackground(new Color(40, 40, 40));

        JLabel activityLabel = new JLabel("System Activity Log");
        activityLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        if (darkMode) activityLabel.setForeground(Color.WHITE);

        String[] filterOptions = {"All Activity", "Logins", "Deposits", "Withdrawals", "Alerts Only"};
        JComboBox<String> filterCombo = new JComboBox<>(filterOptions);
        filterCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        if (darkMode) {
            filterCombo.setBackground(new Color(60, 60, 60));
            filterCombo.setForeground(Color.WHITE);
        }

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (darkMode) filterPanel.setBackground(new Color(40, 40, 40));
        filterPanel.add(new JLabel("Filter:"));
        filterPanel.add(filterCombo);

        topPanel.add(activityLabel, BorderLayout.WEST);
        topPanel.add(filterPanel, BorderLayout.EAST);

        activityLog = new JTextArea();
        activityLog.setEditable(false);
        activityLog.setFont(new Font("Monospaced", Font.PLAIN, 14));
        if (darkMode) {
            activityLog.setBackground(new Color(60, 60, 60));
            activityLog.setForeground(Color.WHITE);
            activityLog.setCaretColor(Color.WHITE);
        }

        activityScrollPane = new JScrollPane(activityLog);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (darkMode) buttonPanel.setBackground(new Color(40, 40, 40));

        SmoothButton exportButton = new SmoothButton("Export Log", brandBlue, brandBlue, brandBlue.darker(), new Font("SansSerif", Font.BOLD, 14));
        exportButton.addActionListener(e -> exportActivityLog());
        buttonPanel.add(exportButton);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(activityScrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createAlertsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        if (darkMode) panel.setBackground(new Color(40, 40, 40));

        JPanel settingsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        if (darkMode) settingsPanel.setBackground(new Color(40, 40, 40));

        // Large deposit threshold
        JLabel depositLabel = new JLabel("Large Deposit Threshold ($):");
        depositLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        if (darkMode) depositLabel.setForeground(Color.WHITE);

        depositField = new JTextField(String.valueOf(LARGE_DEPOSIT_THRESHOLD));
        depositField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        if (darkMode) {
            depositField.setBackground(new Color(60, 60, 60));
            depositField.setForeground(Color.WHITE);
            depositField.setCaretColor(Color.WHITE);
        }

        // Large withdrawal threshold
        JLabel withdrawalLabel = new JLabel("Large Withdrawal Threshold ($):");
        withdrawalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        if (darkMode) withdrawalLabel.setForeground(Color.WHITE);

        withdrawalField = new JTextField(String.valueOf(LARGE_WITHDRAWAL_THRESHOLD));
        withdrawalField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        if (darkMode) {
            withdrawalField.setBackground(new Color(60, 60, 60));
            withdrawalField.setForeground(Color.WHITE);
            withdrawalField.setCaretColor(Color.WHITE);
        }

        // Frequent transaction count
        JLabel freqCountLabel = new JLabel("Frequent Transaction Count:");
        freqCountLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        if (darkMode) freqCountLabel.setForeground(Color.WHITE);

        freqCountField = new JTextField(String.valueOf(FREQUENT_TRANSACTION_COUNT));
        freqCountField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        if (darkMode) {
            freqCountField.setBackground(new Color(60, 60, 60));
            freqCountField.setForeground(Color.WHITE);
            freqCountField.setCaretColor(Color.WHITE);
        }

        // Time period for frequent transactions
        JLabel timeWindowLabel = new JLabel("Time Window (hours):");
        timeWindowLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        if (darkMode) timeWindowLabel.setForeground(Color.WHITE);

        timeWindowField = new JTextField(String.valueOf(FREQUENT_TRANSACTION_HOURS));
        timeWindowField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        if (darkMode) {
            timeWindowField.setBackground(new Color(60, 60, 60));
            timeWindowField.setForeground(Color.WHITE);
            timeWindowField.setCaretColor(Color.WHITE);
        }

        // Email notification
        JLabel emailLabel = new JLabel("Email for Notifications:");
        emailLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        if (darkMode) emailLabel.setForeground(Color.WHITE);

        emailField = new JTextField("admin@fancybank.com");
        emailField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        if (darkMode) {
            emailField.setBackground(new Color(60, 60, 60));
            emailField.setForeground(Color.WHITE);
            emailField.setCaretColor(Color.WHITE);
        }

        // Add all components to settings panel
        settingsPanel.add(depositLabel);
        settingsPanel.add(depositField);
        settingsPanel.add(withdrawalLabel);
        settingsPanel.add(withdrawalField);
        settingsPanel.add(freqCountLabel);
        settingsPanel.add(freqCountField);
        settingsPanel.add(timeWindowLabel);
        settingsPanel.add(timeWindowField);
        settingsPanel.add(emailLabel);
        settingsPanel.add(emailField);

        // Create test alert button
        JPanel testPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        if (darkMode) testPanel.setBackground(new Color(40, 40, 40));

        SmoothButton testButton = new SmoothButton("Test Alert Notification", brandBlue, brandBlue, brandBlue.darker(), new Font("SansSerif", Font.BOLD, 14));
        testButton.addActionListener(e -> showTestAlert());
        testPanel.add(testButton);

        // Save settings button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        if (darkMode) buttonPanel.setBackground(new Color(40, 40, 40));

        SmoothButton saveButton = new SmoothButton("Save Settings", brandBlue, brandBlue, brandBlue.darker(), new Font("SansSerif", Font.BOLD, 14));
        saveButton.addActionListener(e -> {
            try {
                LARGE_DEPOSIT_THRESHOLD = Double.parseDouble(depositField.getText().trim());
                LARGE_WITHDRAWAL_THRESHOLD = Double.parseDouble(withdrawalField.getText().trim());
                FREQUENT_TRANSACTION_COUNT = Integer.parseInt(freqCountField.getText().trim());
                FREQUENT_TRANSACTION_HOURS = Integer.parseInt(timeWindowField.getText().trim());

                // Save settings to a file
                saveAlertSettings();

                JOptionPane.showMessageDialog(panel, "Alert settings saved successfully.", "Settings Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Please enter valid numbers for all thresholds.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(saveButton);

        // Layout adjustments
        JPanel centerWrapper = new JPanel();
        centerWrapper.setLayout(new BoxLayout(centerWrapper, BoxLayout.Y_AXIS));
        centerWrapper.setBackground(darkMode ? new Color(40, 40, 40) : Color.WHITE);
        centerWrapper.add(settingsPanel);
        centerWrapper.add(testPanel);

        panel.add(new JLabel("Configure Alert Thresholds", SwingConstants.CENTER), BorderLayout.NORTH);
        panel.add(centerWrapper, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateUserDetails(String username, JTextArea transactionArea, JLabel balanceValue, JLabel transCountValue, JLabel lastLoginValue) {
        System.out.println("DEBUG: Updating user details for: " + username);

        // Update transaction history
        transactionArea.setText("");
        List<TransactionRecord> transactions = allTransactions.getOrDefault(username, new ArrayList<>());

        System.out.println("DEBUG: Found " + transactions.size() + " transactions for " + username);

        if (transactions.isEmpty()) {
            transactionArea.setText("No transaction history available for this user.");
        } else {
            // Transactions sorted newest first; display newest first
            for (TransactionRecord record : transactions) {
                transactionArea.append(record.toString() + "\n");
            }
        }

        // Update user statistics
        double balance = UserManager.getBalance(username);
        System.out.println("DEBUG: Balance for " + username + ": " + balance);

        balanceValue.setText(String.format("$%,.2f", balance));
        transCountValue.setText(String.valueOf(transactions.size()));

        // Get last activity time
        if (!transactions.isEmpty()) {
            TransactionRecord lastTransaction = transactions.get(0); // first is newest
            lastLoginValue.setText(lastTransaction.getTimestamp());
        } else {
            lastLoginValue.setText("No activity");
        }
    }

    private void refreshData() {
        // Clear and reload data
        usersModel.clear();
        loadAllUsers();
        loadAllTransactions();
        checkForSuspiciousActivity();

        statusLabel.setText("Data refreshed at " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        JOptionPane.showMessageDialog(this, "Admin panel data has been refreshed.",
                "Data Refreshed", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadAllUsers() {
        System.out.println("DEBUG: Loading all users...");

        // Get all users from UserManager
        List<String> users = UserManager.getAllUsers();
        System.out.println("DEBUG: Found " + users.size() + " users: " + users);

        for (String user : users) {
            usersModel.addElement(user);
        }
    }

    private void loadAllTransactions() {
        System.out.println("DEBUG: Loading all transactions...");
        allTransactions.clear();

        // Get all users
        List<String> users = UserManager.getAllUsers();
        System.out.println("DEBUG: Loading transactions for users: " + users);

        // Load transactions for each user
        for (String username : users) {
            List<TransactionRecord> userTransactions = new ArrayList<>();

            File historyFile = new File(username + "_history.txt");
            System.out.println("DEBUG: Checking for file: " + historyFile.getAbsolutePath() + " - exists: " + historyFile.exists());

            if (historyFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(historyFile))) {
                    String line;
                    int lineCount = 0;
                    while ((line = reader.readLine()) != null) {
                        lineCount++;
                        line = line.trim();
                        if (line.isEmpty()) continue;

                        System.out.println("DEBUG: Parsing line " + lineCount + " for " + username + ": " + line);

                        // Parse transaction record from line
                        try {
                            TransactionRecord record = TransactionRecord.fromString(line);
                            if (record != null) {
                                userTransactions.add(record);
                                System.out.println("DEBUG: Successfully parsed: " + record);
                            } else {
                                System.out.println("DEBUG: Failed to parse line: " + line);
                            }
                        } catch (Exception e) {
                            System.err.println("DEBUG: Error parsing transaction line '" + line + "': " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    System.out.println("DEBUG: Loaded " + userTransactions.size() + " transactions for " + username);
                } catch (IOException e) {
                    System.err.println("Error reading transaction history for " + username + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("DEBUG: No history file found for " + username);
            }

            // Sort transactions by timestamp (newest first)
            userTransactions.sort(Comparator.comparing(TransactionRecord::getTimestampDate).reversed());

            allTransactions.put(username, userTransactions);
        }

        System.out.println("DEBUG: Total transactions loaded: " + allTransactions.size() + " users");
    }

    private void checkForSuspiciousActivity() {
        StringBuilder alertText = new StringBuilder();

        for (String username : allTransactions.keySet()) {
            List<TransactionRecord> transactions = allTransactions.get(username);

            // Check for large deposits
            for (TransactionRecord record : transactions) {
                if (record.getType().equals("Deposit") && record.getAmount() >= LARGE_DEPOSIT_THRESHOLD) {
                    String alert = String.format("ALERT: Large deposit of $%,.2f by %s on %s\n",
                            record.getAmount(), username, record.getTimestamp());
                    alertText.append(alert);
                }

                // Check for large withdrawals
                if (record.getType().equals("Withdrawal") && record.getAmount() >= LARGE_WITHDRAWAL_THRESHOLD) {
                    String alert = String.format("ALERT: Large withdrawal of $%,.2f by %s on %s\n",
                            record.getAmount(), username, record.getTimestamp());
                    alertText.append(alert);
                }
            }

            // Check for frequent transactions
            if (transactions.size() >= FREQUENT_TRANSACTION_COUNT) {
                // Get transactions in the last FREQUENT_TRANSACTION_HOURS
                LocalDateTime cutoff = LocalDateTime.now().minusHours(FREQUENT_TRANSACTION_HOURS);
                int recentCount = 0;

                for (TransactionRecord record : transactions) {
                    if (record.getTimestampDate().isAfter(cutoff)) {
                        recentCount++;
                    }
                }

                if (recentCount >= FREQUENT_TRANSACTION_COUNT) {
                    String alert = String.format("ALERT: Frequent activity detected - %d transactions by %s in the last %d hours\n",
                            recentCount, username, FREQUENT_TRANSACTION_HOURS);
                    alertText.append(alert);
                }
            }
        }

        // Display alerts in activity log
        if (alertText.length() > 0) {
            activityLog.append("--- SUSPICIOUS ACTIVITY REPORT ---\n");
            activityLog.append(alertText.toString());
            activityLog.append("--------------------------------\n\n");

            // Show notification to admin
            showAlertNotification(alertText.toString());
        }
    }

    private void showAlertNotification(String alertText) {
        JDialog alertDialog = new JDialog(this, "Security Alert", true);
        alertDialog.setSize(500, 300);
        alertDialog.setLocationRelativeTo(this);

        JPanel alertPanel = new JPanel(new BorderLayout());
        alertPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        if (darkMode) {
            alertPanel.setBackground(new Color(50, 0, 0));
        } else {
            alertPanel.setBackground(new Color(255, 240, 240));
        }

        JLabel alertIcon = new JLabel("⚠️");
        alertIcon.setFont(new Font("Dialog", Font.BOLD, 48));
        alertIcon.setForeground(warningColor);
        alertIcon.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel alertLabel = new JLabel("Suspicious Activity Detected!");
        alertLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        alertLabel.setForeground(warningColor);
        alertLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JTextArea alertDetails = new JTextArea(alertText);
        alertDetails.setEditable(false);
        alertDetails.setFont(new Font("Monospaced", Font.PLAIN, 12));
        if (darkMode) {
            alertDetails.setBackground(new Color(60, 20, 20));
            alertDetails.setForeground(Color.WHITE);
        } else {
            alertDetails.setBackground(new Color(255, 240, 240));
            alertDetails.setForeground(Color.BLACK);
        }

        JScrollPane alertScroll = new JScrollPane(alertDetails);

        JPanel northPanel = new JPanel(new BorderLayout());
        if (darkMode) northPanel.setBackground(new Color(50, 0, 0));
        else northPanel.setBackground(new Color(255, 240, 240));

        northPanel.add(alertIcon, BorderLayout.WEST);
        northPanel.add(alertLabel, BorderLayout.CENTER);

        JButton acknowledgeButton = new SmoothButton("Acknowledge Alert", brandBlue, warningColor, warningColor.darker(), new Font("SansSerif", Font.BOLD, 14));
        acknowledgeButton.addActionListener(e -> alertDialog.dispose());

        alertPanel.add(northPanel, BorderLayout.NORTH);
        alertPanel.add(alertScroll, BorderLayout.CENTER);
        alertPanel.add(acknowledgeButton, BorderLayout.SOUTH);

        alertDialog.add(alertPanel);
        alertDialog.setVisible(true);
    }

    private void showTestAlert() {
        String testAlert = "TEST ALERT: This is a test security notification.\n" +
                "If this were a real alert, details about suspicious transactions would appear here.\n" +
                "You can configure alert thresholds in the settings panel.";

        showAlertNotification(testAlert);
    }

    private void showAlertSettings() {
        tabPane.setSelectedIndex(2); // Switch to alerts tab
    }

    private void saveAlertSettings() {
        File dir = new File("data");
        if (!dir.exists()) {
            dir.mkdir();
        }
        try (PrintWriter writer = new PrintWriter("data/alert_settings.properties")) {
            writer.println("LARGE_DEPOSIT_THRESHOLD=" + LARGE_DEPOSIT_THRESHOLD);
            writer.println("LARGE_WITHDRAWAL_THRESHOLD=" + LARGE_WITHDRAWAL_THRESHOLD);
            writer.println("FREQUENT_TRANSACTION_COUNT=" + FREQUENT_TRANSACTION_COUNT);
            writer.println("FREQUENT_TRANSACTION_HOURS=" + FREQUENT_TRANSACTION_HOURS);
        } catch (IOException e) {
            System.err.println("Error saving alert settings: " + e.getMessage());
        }
    }

    private void loadAlertSettings() {
        File settingsFile = new File("data/alert_settings.properties");
        if (settingsFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(settingsFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("=");
                    if (parts.length == 2) {
                        switch (parts[0]) {
                            case "LARGE_DEPOSIT_THRESHOLD":
                                LARGE_DEPOSIT_THRESHOLD = Double.parseDouble(parts[1]);
                                break;
                            case "LARGE_WITHDRAWAL_THRESHOLD":
                                LARGE_WITHDRAWAL_THRESHOLD = Double.parseDouble(parts[1]);
                                break;
                            case "FREQUENT_TRANSACTION_COUNT":
                                FREQUENT_TRANSACTION_COUNT = Integer.parseInt(parts[1]);
                                break;
                            case "FREQUENT_TRANSACTION_HOURS":
                                FREQUENT_TRANSACTION_HOURS = Integer.parseInt(parts[1]);
                                break;
                        }
                    }
                }
            } catch (IOException | NumberFormatException e) {
                System.err.println("Error loading alert settings: " + e.getMessage());
            }
        }
    }

    private void exportActivityLog() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Activity Log");

        // Ensure data dir exists and set for default file path
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File defaultFile = new File(dataDir, "FancyBank_ActivityLog_" + timestamp + ".txt");
        fileChooser.setSelectedFile(defaultFile);

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("FancyBank Activity Log - Generated: " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println("---------------------------------------------------------------");
                writer.println();
                writer.println(activityLog.getText());
                writer.println();
                writer.println("--- End of Activity Log ---");

                JOptionPane.showMessageDialog(this,
                        "Activity log exported successfully to:\n" + file.getAbsolutePath(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error exporting activity log: " + e.getMessage(),
                        "Export Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void applyDarkMode(Container container) {
        container.setBackground(new Color(40, 40, 40));
        container.setForeground(Color.WHITE);

        for (Component comp : container.getComponents()) {
            if (comp instanceof JPanel) {
                comp.setBackground(new Color(40, 40, 40));
                comp.setForeground(Color.WHITE);
                applyDarkMode((Container) comp);
            } else if (comp instanceof JLabel) {
                comp.setForeground(Color.WHITE);
            } else if (comp instanceof JTextArea || comp instanceof JTextField) {
                comp.setBackground(new Color(60, 60, 60));
                comp.setForeground(Color.WHITE);
                if (comp instanceof JTextArea) {
                    ((JTextArea) comp).setCaretColor(Color.WHITE);
                } else if (comp instanceof JTextField) {
                    ((JTextField) comp).setCaretColor(Color.WHITE);
                }
            } else if (comp instanceof JScrollPane) {
                JScrollPane scrollPane = (JScrollPane) comp;
                Component viewportView = scrollPane.getViewport().getView();
                if (viewportView instanceof JTextArea) {
                    viewportView.setBackground(new Color(60, 60, 60));
                    viewportView.setForeground(Color.WHITE);
                } else if (viewportView instanceof JList) {
                    viewportView.setBackground(new Color(60, 60, 60));
                    viewportView.setForeground(Color.WHITE);
                }
            }
        }
    }
}