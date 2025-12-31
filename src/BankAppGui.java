import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BankAppGui extends JFrame {
    private BankAccount account;
    private JTextField amountField;
    private JLabel balanceLabel;
    private JLabel statusLabel;
    private JTextArea historyArea;
    private boolean darkMode = false;
    private final Color lightBG = Color.WHITE;
    private final Color darkBG = new Color(40, 40, 40);
    private final Color lightFG = Color.BLACK;
    private final Color darkFG = Color.LIGHT_GRAY;
    private final Color brandBlue = new Color(0, 102, 204);
    private final Color brandGold = new Color(204, 153, 0);
    private List<Double> balancePoints = new ArrayList<>();
    private String username;
    private ChartPanel chartPanel;
    private boolean isAdmin;

    public BankAppGui(String username, boolean isAdmin, boolean darkMode) {
        this.username = username;
        this.isAdmin = isAdmin;
        this.darkMode = darkMode;
        double initialBalance = UserManager.getBalance(username);
        account = new BankAccount(initialBalance);

        // Add initial balance point for the chart
        balancePoints.add(initialBalance);

        setTitle("FancyBank Professional Banking - " + username + (isAdmin ? " (Administrator)" : ""));
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        loadHistoryFromFile();
        applyTheme();
        setVisible(true);
    }

    private void initComponents() {
        Container container = getContentPane();
        container.setLayout(new BorderLayout());

        // North panel: bank logo and user info
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        // Logo panel
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel logoLabel = new JLabel("FancyBank™");
        logoLabel.setFont(new Font("Serif", Font.BOLD, 24));
        logoLabel.setForeground(brandBlue);
        logoPanel.add(logoLabel);

        // User info panel
        JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel userInfo = new JLabel("👤 " + username + " | Account #: " + generateAccountNumber(username));
        userInfo.setFont(new Font("SansSerif", Font.BOLD, 14));
        userInfoPanel.add(userInfo);

        northPanel.add(logoPanel, BorderLayout.WEST);
        northPanel.add(userInfoPanel, BorderLayout.EAST);
        container.add(northPanel, BorderLayout.NORTH);

        // Center panel with balance and transaction history
        JPanel centerPanel = new JPanel(new BorderLayout());

        // Top of center: balance and status
        JPanel balancePanel = new JPanel(new BorderLayout());
        balancePanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        balanceLabel = new JLabel("Current Balance: $" + String.format("%,.2f", account.getBalance()));
        balanceLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        balanceLabel.setForeground(brandBlue);

        statusLabel = new JLabel("Welcome to FancyBank Professional Banking");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));

        balancePanel.add(balanceLabel, BorderLayout.NORTH);
        balancePanel.add(statusLabel, BorderLayout.SOUTH);
        centerPanel.add(balancePanel, BorderLayout.NORTH);

        // Middle of center: transaction history
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.setBorder(BorderFactory.createTitledBorder("Transaction History"));

        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyPanel.add(historyScroll, BorderLayout.CENTER);
        centerPanel.add(historyPanel, BorderLayout.CENTER);

        container.add(centerPanel, BorderLayout.CENTER);

        // East panel: chart and controls
        JPanel eastPanel = new JPanel(new BorderLayout());
        eastPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
        eastPanel.setPreferredSize(new Dimension(200, 600));

        JPanel chartTitlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel chartTitle = new JLabel("Balance History");
        chartTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        chartTitlePanel.add(chartTitle);

        chartPanel = new ChartPanel(balancePoints);
        chartPanel.setBorder(BorderFactory.createLoweredBevelBorder());

        eastPanel.add(chartTitlePanel, BorderLayout.NORTH);
        eastPanel.add(chartPanel, BorderLayout.CENTER);

        // Theme toggle and (optional) admin panel button
        JPanel eastBottomPanel = new JPanel(new GridLayout(0, 1, 10, 10));
        JButton toggleThemeBtn = new JButton("Toggle Dark/Light Theme");
        toggleThemeBtn.setFont(new Font("SansSerif", Font.PLAIN, 12));
        toggleThemeBtn.addActionListener(e -> {
            darkMode = !darkMode;
            applyTheme();
        });

        eastBottomPanel.add(toggleThemeBtn);

        if (isAdmin) {
            SmoothButton adminPanelBtn = new SmoothButton(
                    "Open Admin Panel",
                    brandBlue,
                    brandBlue.darker(),
                    Color.BLACK,
                    new Font("SansSerif", Font.BOLD, 14)
            );
            adminPanelBtn.addActionListener(e -> {
                new AdminPanel(username);
            });
            eastBottomPanel.add(adminPanelBtn);
        }

        eastPanel.add(eastBottomPanel, BorderLayout.SOUTH);

        container.add(eastPanel, BorderLayout.EAST);

        // South panel: transaction controls
        JPanel southPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        southPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel amountPanel = new JPanel(new BorderLayout());
        amountPanel.add(new JLabel("Transaction Amount ($):"), BorderLayout.NORTH);
        amountField = new JTextField();
        amountField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        amountPanel.add(amountField, BorderLayout.CENTER);

        ((AbstractDocument) amountField.getDocument()).setDocumentFilter(new DecimalInputFilter());

        amountPanel.add(amountField, BorderLayout.CENTER);
        SmoothButton depositBtn = new SmoothButton("Deposit Funds", brandBlue, brandBlue.darker(), Color.BLACK, new Font("SansSerif", Font.BOLD, 14));
        SmoothButton withdrawBtn = new SmoothButton("Withdraw Funds", brandGold, brandGold.darker(), Color.BLACK, new Font("SansSerif", Font.BOLD, 14));

        southPanel.add(amountPanel);
        southPanel.add(depositBtn);
        southPanel.add(withdrawBtn);

        container.add(southPanel, BorderLayout.SOUTH);

        // Button actions
        depositBtn.addActionListener(e -> handleDeposit());
        withdrawBtn.addActionListener(e -> handleWithdraw());

        // Add keyboard listener for Enter key
        amountField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleDeposit(); // Default action is deposit
                }
            }
        });
    }

    private String generateAccountNumber(String username) {
        int hash = Math.abs(username.hashCode()) % 1000000;
        return String.format("%06d", hash);
    }

    private void handleDeposit() {
        try {
            String amountText = amountField.getText().trim();
            if (amountText.isEmpty()) {
                showError("Please enter an amount to deposit.");
                return;
            }

            amountText = amountText.replace(",", "");

            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                showError("Amount must be greater than zero.");
                return;
            }
            account.deposit(amount);
            UserManager.updateBalance(username, account.getBalance());
            logTransaction("Deposit", amount);
            updateUI("Successfully deposited $" + String.format("%,.2f", amount));
        } catch (NumberFormatException e) {
            showError("Please enter a valid number.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("An error occurred: " + e.getMessage());
        }
    }

    private void handleWithdraw() {
        try {
            String amountText = amountField.getText().trim();
            if (amountText.isEmpty()) {
                showError("Please enter an amount to withdraw.");
                return;
            }

            amountText = amountText.replace(",", "");

            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                showError("Amount must be greater than zero.");
                return;
            }
            account.withdraw(amount);
            UserManager.updateBalance(username, account.getBalance());
            logTransaction("Withdrawal", amount);
            updateUI("Successfully withdrew $" + String.format("%,.2f", amount));
        } catch (NumberFormatException e) {
            showError("Please enter a valid number.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            showError("An error occurred: " + e.getMessage());
        }
    }

    private void updateUI(String message) {
        balanceLabel.setText("Current Balance: $" + String.format("%,.2f", account.getBalance()));
        statusLabel.setText(message);
        amountField.setText("");
        balancePoints.add(account.getBalance());
        if (chartPanel != null) {
            chartPanel.updateData(balancePoints);
            chartPanel.repaint();
        }
    }

    private void logTransaction(String type, double amount) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String line = String.format("[%s] %s: $%,.2f - Balance: $%,.2f\n", timestamp, type, amount, account.getBalance());
        historyArea.append(line);
        try (FileWriter fw = new FileWriter(username + "_history.txt", true)) {
            fw.write(line);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Transaction Error", JOptionPane.ERROR_MESSAGE);
    }

    // In BankAppGui.java

    private void applyTheme() {
        // 1. Use the manager to style the whole window
        ThemeManager.applyTheme(this.getContentPane(), darkMode);

        // 2. Handle special components that need brand colors
        if (balanceLabel != null) {
            balanceLabel.setForeground(darkMode ? ThemeManager.BRAND_GOLD : ThemeManager.BRAND_BLUE);
        }

        // 3. Refresh the UI
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void applyThemeToContainer(Container container, Color bg, Color fg) {
        container.setBackground(bg);
        container.setForeground(fg);

        for (Component comp : container.getComponents()) {
            if (comp instanceof JPanel) {
                comp.setBackground(bg);
                comp.setForeground(fg);
                applyThemeToContainer((Container)comp, bg, fg);
            } else if (comp instanceof JTextField || comp instanceof JTextArea) {
                comp.setBackground(darkMode ? new Color(60, 60, 60) : Color.WHITE);
                comp.setForeground(darkMode ? Color.LIGHT_GRAY : Color.BLACK);
            } else if (!(comp instanceof JButton || comp instanceof ChartPanel)) {
                // Don't change the background of buttons (they have their own styling)
                comp.setBackground(bg);
                comp.setForeground(fg);
            }

            if (comp instanceof Container && !(comp instanceof JButton)) {
                applyThemeToContainer((Container)comp, bg, fg);
            }
        }
    }

    private void loadHistoryFromFile() {
        File file = new File(username + "_history.txt");
        if(file.exists()){
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    historyArea.append(line + "\n");

                    // Extract balance from history to populate chart data
                    try {
                        String balanceStr = line.substring(line.lastIndexOf("$") + 1);
                        double balance = Double.parseDouble(balanceStr.replace(",", ""));
                        balancePoints.add(balance);
                    } catch (Exception e) {
                        // Skip if can't parse balance from this line
                    }
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}



class BankAccount {
    private double balance;

    public BankAccount(double initialBalance) {
        if(initialBalance < 0) throw new IllegalArgumentException("Negative balance not allowed.");
        balance = initialBalance;
    }

    public void deposit(double amount) {
        if(amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive.");
        balance += amount;
    }

    public void withdraw(double amount) {
        if(amount <= 0) throw new IllegalArgumentException("Withdrawal amount must be positive.");
        if(amount > balance) throw new IllegalArgumentException("Insufficient funds for this withdrawal.");
        balance -= amount;
    }

    public double getBalance() {
        return balance;
    }
}

class ChartPanel extends JPanel {
    private List<Double> balances;
    private boolean darkMode = false;
    private final Color lightGridColor = new Color(220, 220, 220);
    private final Color darkGridColor = new Color(70, 70, 70);
    private final Color lightLineColor = new Color(0, 102, 204);
    private final Color darkLineColor = new Color(51, 153, 255);
    private final Color lightPointColor = new Color(0, 51, 153);
    private final Color darkPointColor = new Color(102, 178, 255);

    public ChartPanel(List<Double> balances) {
        this.balances = new ArrayList<>(balances); // Create a copy
        setPreferredSize(new Dimension(180, 200));
    }
    
    public void updateData(List<Double> newBalances) {
        this.balances = new ArrayList<>(newBalances); // Create a copy
    }
    
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(balances == null || balances.isEmpty()) return;
        
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int w = getWidth();
        int h = getHeight();
        int padding = 20;
        int labelPadding = 20;
        
        // Background
        g2.setColor(darkMode ? new Color(30, 30, 30) : Color.WHITE);
        g2.fillRect(0, 0, w, h);
        
        // Calculate min and max values for scaling
        double minBalance = balances.stream().mapToDouble(b -> b).min().orElse(0);
        double maxBalance = balances.stream().mapToDouble(b -> b).max().orElse(1);
        
        // Ensure min and max are different to avoid division by zero
        if (maxBalance == minBalance) {
            if (maxBalance == 0) maxBalance = 1;
            else maxBalance *= 1.1;
        }
        
        int pointCount = balances.size();
        double xScale = ((double) (w - 2 * padding - labelPadding)) / (pointCount - 1);
        double yScale = ((double) (h - 2 * padding - labelPadding)) / (maxBalance - minBalance);
        
        // Draw grid lines
        g2.setColor(darkMode ? darkGridColor : lightGridColor);
        g2.setStroke(new BasicStroke(1f));
        
        // Draw horizontal grid lines
        for (int i = 0; i < 5; i++) {
            int y = padding + labelPadding + (h - 2 * padding - 2 * labelPadding) * i / 4;
            g2.drawLine(padding, y, w - padding, y);
        }
        
        // Draw vertical grid lines
        for (int i = 0; i < pointCount; i += Math.max(1, pointCount / 5)) {
            int x = padding + labelPadding + (int)(i * xScale);
            g2.drawLine(x, padding, x, h - padding - labelPadding);
        }
        
        // Draw axes
        g2.setColor(darkMode ? Color.LIGHT_GRAY : Color.DARK_GRAY);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(padding + labelPadding, padding, padding + labelPadding, h - padding - labelPadding);
        g2.drawLine(padding + labelPadding, h - padding - labelPadding, w - padding, h - padding - labelPadding);
        
        // Draw balance line
        g2.setColor(darkMode ? darkLineColor : lightLineColor);
        g2.setStroke(new BasicStroke(2f));
        
        int[] xPoints = new int[pointCount];
        int[] yPoints = new int[pointCount];
        
        for (int i = 0; i < pointCount; i++) {
            xPoints[i] = padding + labelPadding + (int)(i * xScale);
            yPoints[i] = h - padding - labelPadding - (int)((balances.get(i) - minBalance) * yScale);
        }
        
        // Draw line
        for (int i = 0; i < pointCount - 1; i++) {
            g2.drawLine(xPoints[i], yPoints[i], xPoints[i + 1], yPoints[i + 1]);
        }
        
        // Draw points
        g2.setColor(darkMode ? darkPointColor : lightPointColor);
        for (int i = 0; i < pointCount; i++) {
            g2.fillOval(xPoints[i] - 4, yPoints[i] - 4, 8, 8);
        }
        
        // Draw the minimum and maximum values
        g2.setColor(darkMode ? Color.LIGHT_GRAY : Color.DARK_GRAY);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.drawString("$" + String.format("%,.2f", minBalance), 5, h - padding - labelPadding);
        g2.drawString("$" + String.format("%,.2f", maxBalance), 5, padding + 10);
    }
}

class LoginScreen extends JFrame {
    private JTextField usernameField;
    private JPasswordField pinField;
    private boolean darkMode = true;
    private final Color brandBlue = new Color(0, 102, 204);

    public LoginScreen() {
        setTitle("FancyBank Login");
        setSize(400, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        if (darkMode) mainPanel.setBackground(new Color(40, 40, 40));
        
        // Logo at top
        JPanel logoPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        if (darkMode) logoPanel.setBackground(new Color(40, 40, 40));
        JLabel logoLabel = new JLabel("FancyBank™");
        logoLabel.setFont(new Font("Serif", Font.BOLD, 32));
        logoLabel.setForeground(brandBlue);
        JLabel tagline = new JLabel("Professional Banking Solutions");
        tagline.setFont(new Font("SansSerif", Font.ITALIC, 14));
        tagline.setForeground(Color.LIGHT_GRAY);
        
        JPanel logoPadding = new JPanel(new GridLayout(2, 1, 5, 0));
        if (darkMode) logoPadding.setBackground(new Color(40, 40, 40));
        logoPadding.add(logoLabel);
        logoPadding.add(tagline);
        logoPanel.add(logoPadding);
        
        // Login form
        JPanel formPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        if (darkMode) formPanel.setBackground(new Color(40, 40, 40));
        
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        usernameLabel.setForeground(Color.LIGHT_GRAY);
        
        JLabel pinLabel = new JLabel("PIN:");
        pinLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        pinLabel.setForeground(Color.LIGHT_GRAY);
        
        usernameField = new JTextField();
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        pinField = new JPasswordField();
        pinField.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        if (darkMode) {
            usernameField.setBackground(new Color(60, 60, 60));
            usernameField.setForeground(Color.WHITE);
            usernameField.setCaretColor(Color.WHITE);
            pinField.setBackground(new Color(60, 60, 60));
            pinField.setForeground(Color.WHITE);
            pinField.setCaretColor(Color.WHITE);
        }
        
        formPanel.add(usernameLabel);
        formPanel.add(usernameField);
        formPanel.add(pinLabel);
        formPanel.add(pinField);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 40, 20, 40));
        if (darkMode) buttonPanel.setBackground(new Color(40, 40, 40));
        
        SmoothButton loginBtn = new SmoothButton("Login", brandBlue, brandBlue, brandBlue.darker(), new Font("SansSerif", Font.BOLD, 14));
        SmoothButton registerBtn = new SmoothButton("Register", brandBlue, new Color(100, 100, 100), new Color(70, 70, 70), new Font("SansSerif", Font.PLAIN, 14));
        
        loginBtn.addActionListener(e -> handleLogin());
        registerBtn.addActionListener(e -> new RegistrationForm(darkMode));
        
        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);
        
        // Add key listeners for Enter key
        KeyAdapter enterKeyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleLogin();
                }
            }
        };
        
        usernameField.addKeyListener(enterKeyListener);
        pinField.addKeyListener(enterKeyListener);
        
        // Add components to main panel
        mainPanel.add(logoPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        setVisible(true);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String pin = new String(pinField.getPassword()).trim();
        
        if (username.isEmpty() || pin.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter both username and PIN.", 
                "Login Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (UserManager.validateUser(username, pin)) {
            boolean isAdmin = UserManager.isAdmin(username);
            new BankAppGui(username, isAdmin, darkMode);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, 
                "Invalid credentials. Please try again.", 
                "Authentication Failed", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}

class RegistrationForm extends JFrame {
    private JTextField usernameField;
    private JPasswordField pinField;
    private JPasswordField confirmPinField;
    private JCheckBox adminCheckBox;
    private boolean darkMode;
    private final Color brandBlue = new Color(0, 102, 204);

    public RegistrationForm(boolean darkMode) {
        this.darkMode = darkMode;
        setTitle("FancyBank - Register New Account");
        setSize(400, 350);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        initUI();
        setVisible(true);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        if (darkMode) mainPanel.setBackground(new Color(40, 40, 40));

        // Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        if (darkMode) titlePanel.setBackground(new Color(40, 40, 40));

        JLabel titleLabel = new JLabel("Create New Account");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 24));
        titleLabel.setForeground(darkMode ? Color.WHITE : brandBlue);
        titlePanel.add(titleLabel);

        // Form
        JPanel formPanel = new JPanel(new GridLayout(4, 2, 10, 15));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        if (darkMode) formPanel.setBackground(new Color(40, 40, 40));

        JLabel usernameLabel = new JLabel("Username:");
        JLabel pinLabel = new JLabel("PIN:");
        JLabel confirmPinLabel = new JLabel("Confirm PIN:");

        if (darkMode) {
            usernameLabel.setForeground(Color.LIGHT_GRAY);
            pinLabel.setForeground(Color.LIGHT_GRAY);
            confirmPinLabel.setForeground(Color.LIGHT_GRAY);
        }

        usernameField = new JTextField();
        pinField = new JPasswordField();
        confirmPinField = new JPasswordField();
        adminCheckBox = new JCheckBox("Register as Administrator");

        if (darkMode) {
            usernameField.setBackground(new Color(60, 60, 60));
            usernameField.setForeground(Color.WHITE);
            usernameField.setCaretColor(Color.WHITE);

            pinField.setBackground(new Color(60, 60, 60));
            pinField.setForeground(Color.WHITE);
            pinField.setCaretColor(Color.WHITE);

            confirmPinField.setBackground(new Color(60, 60, 60));
            confirmPinField.setForeground(Color.WHITE);
            confirmPinField.setCaretColor(Color.WHITE);

            adminCheckBox.setBackground(new Color(40, 40, 40));
            adminCheckBox.setForeground(Color.LIGHT_GRAY);
        }

        formPanel.add(usernameLabel);
        formPanel.add(usernameField);
        formPanel.add(pinLabel);
        formPanel.add(pinField);
        formPanel.add(confirmPinLabel);
        formPanel.add(confirmPinField);
        formPanel.add(new JLabel()); // Empty for spacing
        formPanel.add(adminCheckBox);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        if (darkMode) buttonPanel.setBackground(new Color(40, 40, 40));

        SmoothButton registerBtn = new SmoothButton("Register Account", brandBlue, brandBlue, brandBlue.darker(), new Font("SansSerif", Font.BOLD, 14));
        SmoothButton cancelBtn = new SmoothButton("Cancel", brandBlue, new Color(100, 100, 100), new Color(70, 70, 70), new Font("SansSerif", Font.PLAIN, 14));

        registerBtn.addActionListener(e -> registerUser());
        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(registerBtn);
        buttonPanel.add(cancelBtn);

        // Add everything to main panel
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void registerUser() {
        String username = usernameField.getText().trim();
        String pin = new String(pinField.getPassword()).trim();
        String confirmPin = new String(confirmPinField.getPassword()).trim();
        boolean isAdmin = adminCheckBox.isSelected();

        // Validate inputs
        if (username.isEmpty() || pin.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and PIN.",
                    "Registration Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!pin.equals(confirmPin)) {
            JOptionPane.showMessageDialog(this,
                    "PINs do not match. Please try again.",
                    "Registration Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (UserManager.userExists(username)) {
            JOptionPane.showMessageDialog(this, "Username already exists. Choose another.");
            return;
        }

        UserManager.registerUser(username, pin, isAdmin);
        JOptionPane.showMessageDialog(this, "User registered successfully!");
        dispose(); // Close the registration window
    }
}
