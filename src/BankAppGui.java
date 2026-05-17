import net.miginfocom.swing.MigLayout;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class BankAppGui extends JFrame {
    private final BankAccount account;
    private final List<BigDecimal> balancePoints = new ArrayList<>();
    private final String username;
    private final boolean isAdmin;
    private JTextField amountField;
    private JLabel balanceLabel;
    private JLabel statusLabel;
    private JTextArea historyArea;
    private BalanceChartPanel chartPanel;
    private boolean darkMode;

    public BankAppGui(String username, boolean isAdmin, boolean darkMode) {
        this.username = username;
        this.isAdmin = isAdmin;
        this.darkMode = darkMode;
        this.account = new BankAccount(UserManager.getBalance(username));

        setTitle("FancyBank Professional Banking - " + username + (isAdmin ? " (Administrator)" : ""));
        setSize(1120, 720);
        setMinimumSize(new Dimension(940, 620));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        loadHistoryFromFile();
        if (balancePoints.isEmpty()) {
            balancePoints.add(account.getBalance());
        }
        chartPanel.updateData(balancePoints);
        setVisible(true);
    }

    private void initComponents() {
        JPanel root = new JPanel(new MigLayout(
                "fill, insets 18, gap 14",
                "[min!][grow, fill][360::460, fill]",
                "[][grow, fill][]"
        ));
        setContentPane(root);

        root.add(createHeaderPanel(), "span 3, growx, wrap");
        root.add(createActivityPanel(), "span 2, grow, push");
        root.add(createInsightsPanel(), "grow, wrap");
        root.add(createTransactionPanel(), "span 3, growx");
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx, insets 0", "[][grow][]", "[]"));

        JLabel logoLabel = new JLabel("FancyBank");
        logoLabel.putClientProperty("FlatLaf.styleClass", "h1");
        logoLabel.setForeground(AppUi.BRAND_BLUE);

        JLabel subtitle = new JLabel("Professional banking and intelligence workspace");
        subtitle.putClientProperty("FlatLaf.styleClass", "small");

        JPanel titleStack = new JPanel(new MigLayout("insets 0, gap 0", "[grow]", "[]2[]"));
        titleStack.add(logoLabel, "wrap");
        titleStack.add(subtitle);

        JLabel userInfo = new JLabel(username + "  |  Account #: " + generateAccountNumber(username));
        userInfo.setIcon(AppUi.svgIcon(AppUi.USER_SVG, 18));
        userInfo.setIconTextGap(8);

        JButton themeButton = AppUi.secondaryButton(darkMode ? "Light Mode" : "Dark Mode");
        themeButton.addActionListener(e -> {
            darkMode = !darkMode;
            AppUi.setDarkMode(darkMode);
            themeButton.setText(darkMode ? "Light Mode" : "Dark Mode");
            chartPanel.applyChartTheme();
        });

        panel.add(titleStack);
        panel.add(userInfo, "right");
        panel.add(themeButton, "right");
        return panel;
    }

    private JPanel createActivityPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 0, gap 14", "[grow, fill]", "[][grow, fill]"));

        JPanel balancePanel = AppUi.card();
        balancePanel.setLayout(new MigLayout("fillx, insets 18", "[grow]", "[]6[]"));
        balanceLabel = new JLabel("Current Balance: " + formatCurrency(account.getBalance()));
        balanceLabel.putClientProperty("FlatLaf.styleClass", "h2");
        balanceLabel.setForeground(AppUi.BRAND_BLUE);

        statusLabel = new JLabel("Welcome to FancyBank Professional Banking");
        statusLabel.putClientProperty("FlatLaf.styleClass", "medium");

        balancePanel.add(balanceLabel, "growx, wrap");
        balancePanel.add(statusLabel, "growx");

        JPanel historyPanel = AppUi.card();
        historyPanel.setLayout(new MigLayout("fill, insets 14", "[grow, fill]", "[][grow, fill]"));
        historyPanel.add(AppUi.sectionTitle("Transaction History"), "wrap");

        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setLineWrap(false);
        historyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        historyPanel.add(new JScrollPane(historyArea), "grow");

        panel.add(balancePanel, "growx, wrap");
        panel.add(historyPanel, "grow, push");
        return panel;
    }

    private JPanel createInsightsPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 0, gap 14", "[grow, fill]", "[grow, fill][]"));

        JPanel chartCard = AppUi.card();
        chartCard.setLayout(new MigLayout("fill, insets 14", "[grow, fill]", "[][grow, fill]"));
        chartCard.add(AppUi.sectionTitle("Balance Intelligence"), "wrap");
        chartPanel = new BalanceChartPanel();
        chartCard.add(chartPanel, "grow, push");

        JPanel actionCard = AppUi.card();
        actionCard.setLayout(new MigLayout("fillx, insets 14, gap 10", "[grow, fill]", "[]"));

        if (isAdmin) {
            JButton adminPanelBtn = AppUi.primaryButton("Open Admin Panel");
            adminPanelBtn.setIcon(AppUi.svgIcon(AppUi.ADMIN_SVG, 16));
            adminPanelBtn.addActionListener(e -> new AdminPanel(username));
            actionCard.add(adminPanelBtn, "growx, wrap");
        }

        panel.add(chartCard, "grow, push, wrap");
        panel.add(actionCard, "growx");
        return panel;
    }

    private JPanel createTransactionPanel() {
        JPanel panel = AppUi.card();
        panel.setLayout(new MigLayout(
                "fillx, insets 14, gap 12",
                "[][180::260, fill][120!][120!]",
                "[]"
        ));

        JLabel amountLabel = new JLabel("Transaction Amount");
        amountField = new JTextField();
        ((AbstractDocument) amountField.getDocument()).setDocumentFilter(new DecimalInputFilter());

        JButton depositBtn = AppUi.primaryButton("Deposit");
        JButton withdrawBtn = AppUi.secondaryButton("Withdraw");

        panel.add(amountLabel);
        panel.add(amountField, "growx");
        panel.add(depositBtn, "growx");
        panel.add(withdrawBtn, "growx");

        depositBtn.addActionListener(e -> handleDeposit());
        withdrawBtn.addActionListener(e -> handleWithdraw());
        amountField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleDeposit();
                }
            }
        });

        return panel;
    }

    private String generateAccountNumber(String username) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(username.getBytes(StandardCharsets.UTF_8));
            int number = ((hash[0] & 0xFF) << 16) | ((hash[1] & 0xFF) << 8) | (hash[2] & 0xFF);
            return String.format("%06d", number % 1_000_000);
        } catch (NoSuchAlgorithmException ex) {
            return String.format("%06d", Math.floorMod(username.hashCode(), 1_000_000));
        }
    }

    private void handleDeposit() {
        try {
            BigDecimal amount = parseAmount("deposit");
            account.deposit(amount);
            UserManager.updateBalance(username, account.getBalance());
            logTransaction("Deposit", amount);
            updateBankingUi("Successfully deposited " + formatCurrency(amount));
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("An error occurred: " + ex.getMessage());
        }
    }

    private void handleWithdraw() {
        try {
            BigDecimal amount = parseAmount("withdraw");
            account.withdraw(amount);
            UserManager.updateBalance(username, account.getBalance());
            logTransaction("Withdrawal", amount);
            updateBankingUi("Successfully withdrew " + formatCurrency(amount));
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (Exception ex) {
            showError("An error occurred: " + ex.getMessage());
        }
    }

    private BigDecimal parseAmount(String action) {
        String amountText = amountField.getText().trim().replace(",", "");
        if (amountText.isEmpty()) {
            throw new IllegalArgumentException("Please enter an amount to " + action + ".");
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountText);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Please enter a valid number.");
        }

        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount cannot have more than two decimal places.");
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        return amount;
    }

    private void updateBankingUi(String message) {
        balanceLabel.setText("Current Balance: " + formatCurrency(account.getBalance()));
        statusLabel.setText(message);
        amountField.setText("");
        balancePoints.add(account.getBalance());
        chartPanel.updateData(balancePoints);
    }

    private void logTransaction(String type, BigDecimal amount) {
        TransactionRecord record = new TransactionRecord(type, amount, account.getBalance());
        String line = record.toLogLine() + System.lineSeparator();
        historyArea.append(line);

        try {
            Files.createDirectories(UserManager.getDataDirectory());
            Files.writeString(
                    UserManager.getHistoryFile(username),
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ex) {
            showError("Unable to write transaction history.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Transaction Error", JOptionPane.ERROR_MESSAGE);
    }

    private void loadHistoryFromFile() {
        Path historyFile = UserManager.getHistoryFile(username);
        if (!Files.exists(historyFile)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(historyFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                TransactionRecord record = TransactionRecord.fromString(line);
                if (record == null) {
                    continue;
                }
                historyArea.append(record.toLogLine() + System.lineSeparator());
                record.getBalanceAfter().ifPresent(balancePoints::add);
            }
        } catch (IOException ex) {
            showError("Unable to load transaction history.");
        }
    }

    private static String formatCurrency(BigDecimal amount) {
        NumberFormat currency = NumberFormat.getCurrencyInstance(Locale.US);
        currency.setMinimumFractionDigits(2);
        currency.setMaximumFractionDigits(2);
        return currency.format(UserManager.money(amount));
    }
}

class BankAccount {
    private BigDecimal balance;

    public BankAccount(BigDecimal initialBalance) {
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Negative balance not allowed.");
        }
        balance = UserManager.money(initialBalance);
    }

    public void deposit(BigDecimal amount) {
        validatePositive(amount, "Deposit amount must be positive.");
        balance = UserManager.money(balance.add(amount));
    }

    public void withdraw(BigDecimal amount) {
        validatePositive(amount, "Withdrawal amount must be positive.");
        if (amount.compareTo(balance) > 0) {
            throw new IllegalArgumentException("Insufficient funds for this withdrawal.");
        }
        balance = UserManager.money(balance.subtract(amount));
    }

    public BigDecimal getBalance() {
        return balance;
    }

    private void validatePositive(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}

class BalanceChartPanel extends JPanel {
    private static final String SERIES_NAME = "Balance";
    private final XYChart chart;
    private final XChartPanel<XYChart> chartView;
    private List<BigDecimal> balances = new ArrayList<>();

    public BalanceChartPanel() {
        setLayout(new MigLayout("fill, insets 0", "[grow, fill]", "[grow, fill]"));

        chart = new XYChartBuilder()
                .width(420)
                .height(280)
                .theme(Styler.ChartTheme.Matlab)
                .xAxisTitle("Transaction")
                .yAxisTitle("Balance")
                .build();
        configureChart();

        chartView = new XChartPanel<>(chart);
        add(chartView, "grow, push");
    }

    public void updateData(List<BigDecimal> newBalances) {
        balances = new ArrayList<>(newBalances);
        Runnable update = () -> {
            configureChart();
            List<Integer> xData = new ArrayList<>();
            List<Double> yData = new ArrayList<>();

            if (balances.isEmpty()) {
                xData.add(0);
                yData.add(0.0);
            } else {
                for (int i = 0; i < balances.size(); i++) {
                    xData.add(i + 1);
                    yData.add(balances.get(i).doubleValue());
                }
            }

            if (chart.getSeriesMap().containsKey(SERIES_NAME)) {
                chart.updateXYSeries(SERIES_NAME, xData, yData, null);
            } else {
                XYSeries series = chart.addSeries(SERIES_NAME, xData, yData);
                series.setMarker(SeriesMarkers.CIRCLE);
            }

            chartView.revalidate();
            chartView.repaint();
        };

        if (SwingUtilities.isEventDispatchThread()) {
            update.run();
        } else {
            SwingUtilities.invokeLater(update);
        }
    }

    public void applyChartTheme() {
        configureChart();
        chartView.repaint();
    }

    private void configureChart() {
        boolean dark = AppUi.isDark();
        Color bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("Label.foreground");
        Color grid = dark ? AppUi.DARK_SURFACE_LINE : AppUi.SURFACE_LINE;

        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setChartBackgroundColor(bg);
        chart.getStyler().setPlotBackgroundColor(bg);
        chart.getStyler().setPlotBorderVisible(false);
        chart.getStyler().setPlotGridLinesColor(grid);
        chart.getStyler().setChartFontColor(fg);
        chart.getStyler().setAxisTickLabelsColor(fg);
        chart.getStyler().setAxisTitleFont(UIManager.getFont("Label.font"));
        chart.getStyler().setAxisTickLabelsFont(UIManager.getFont("Label.font"));
        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setSeriesColors(new Color[]{AppUi.BRAND_BLUE});
        chart.getStyler().setMarkerSize(5);
        chart.getStyler().setXAxisDecimalPattern("0");
        chart.getStyler().setYAxisDecimalPattern("$#,##0.00");
    }
}

class LoginScreen extends JFrame {
    private JTextField usernameField;
    private JPasswordField pinField;
    private final boolean darkMode = true;

    public LoginScreen() {
        setTitle("FancyBank Login");
        setSize(420, 320);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new MigLayout(
                "fill, insets 28, gap 12",
                "[grow, fill]",
                "[]18[]18[]"
        ));

        JLabel logoLabel = new JLabel("FancyBank");
        logoLabel.putClientProperty("FlatLaf.styleClass", "h1");
        logoLabel.setForeground(AppUi.BRAND_BLUE);
        JLabel tagline = new JLabel("Professional Banking Solutions");
        tagline.putClientProperty("FlatLaf.styleClass", "medium");

        JPanel titlePanel = new JPanel(new MigLayout("insets 0, gap 0", "[center]", "[]4[]"));
        titlePanel.add(logoLabel, "wrap");
        titlePanel.add(tagline);

        JPanel formPanel = new JPanel(new MigLayout(
                "fillx, insets 0, gap 10",
                "[][grow, fill]",
                "[][]"
        ));

        usernameField = new JTextField();
        pinField = new JPasswordField();

        formPanel.add(new JLabel("Username"));
        formPanel.add(usernameField, "growx, wrap");
        formPanel.add(new JLabel("PIN"));
        formPanel.add(pinField, "growx");

        JPanel buttonPanel = new JPanel(new MigLayout("fillx, insets 0, gap 10", "[grow, fill][grow, fill]", "[]"));
        JButton loginBtn = AppUi.primaryButton("Login");
        JButton registerBtn = AppUi.secondaryButton("Register");

        loginBtn.addActionListener(e -> handleLogin());
        registerBtn.addActionListener(e -> new RegistrationForm(darkMode));

        buttonPanel.add(loginBtn, "growx");
        buttonPanel.add(registerBtn, "growx");

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

        mainPanel.add(titlePanel, "growx, wrap");
        mainPanel.add(formPanel, "growx, wrap");
        mainPanel.add(buttonPanel, "growx");
        add(mainPanel);
        setVisible(true);
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        char[] pin = pinField.getPassword();

        try {
            if (username.isEmpty() || pin.length == 0) {
                JOptionPane.showMessageDialog(this, "Please enter both username and PIN.", "Login Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (UserManager.validateUser(username, pin)) {
                boolean isAdmin = UserManager.isAdmin(username);
                new BankAppGui(username, isAdmin, darkMode);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials. Please try again.", "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(pin, '\0');
            pinField.setText("");
        }
    }
}

class RegistrationForm extends JFrame {
    private JTextField usernameField;
    private JPasswordField pinField;
    private JPasswordField confirmPinField;

    public RegistrationForm(boolean ignoredDarkMode) {
        setTitle("FancyBank - Register New Account");
        setSize(420, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        initUI();
        setVisible(true);
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new MigLayout(
                "fill, insets 28, gap 12",
                "[grow, fill]",
                "[]18[]18[]"
        ));

        JLabel titleLabel = new JLabel("Create New Account");
        titleLabel.putClientProperty("FlatLaf.styleClass", "h2");

        JPanel formPanel = new JPanel(new MigLayout("fillx, insets 0, gap 10", "[][grow, fill]", "[][][]"));

        usernameField = new JTextField();
        pinField = new JPasswordField();
        confirmPinField = new JPasswordField();

        formPanel.add(new JLabel("Username"));
        formPanel.add(usernameField, "growx, wrap");
        formPanel.add(new JLabel("PIN"));
        formPanel.add(pinField, "growx, wrap");
        formPanel.add(new JLabel("Confirm PIN"));
        formPanel.add(confirmPinField, "growx");

        JPanel buttonPanel = new JPanel(new MigLayout("fillx, insets 0, gap 10", "[grow, fill][grow, fill]", "[]"));
        JButton registerBtn = AppUi.primaryButton("Register Account");
        JButton cancelBtn = AppUi.secondaryButton("Cancel");

        registerBtn.addActionListener(e -> registerUser());
        cancelBtn.addActionListener(e -> dispose());

        buttonPanel.add(registerBtn, "growx");
        buttonPanel.add(cancelBtn, "growx");

        mainPanel.add(titleLabel, "center, wrap");
        mainPanel.add(formPanel, "growx, wrap");
        mainPanel.add(buttonPanel, "growx");
        add(mainPanel);
    }

    private void registerUser() {
        String username = usernameField.getText().trim();
        char[] pin = pinField.getPassword();
        char[] confirmPin = confirmPinField.getPassword();

        try {
            if (username.isEmpty() || pin.length == 0) {
                JOptionPane.showMessageDialog(this, "Please enter both username and PIN.", "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!Arrays.equals(pin, confirmPin)) {
                JOptionPane.showMessageDialog(this, "PINs do not match. Please try again.", "Registration Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (UserManager.userExists(username)) {
                JOptionPane.showMessageDialog(this, "Username already exists. Choose another.");
                return;
            }

            UserManager.registerUser(username, pin);
            JOptionPane.showMessageDialog(this, "User registered successfully!");
            dispose();
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(pin, '\0');
            Arrays.fill(confirmPin, '\0');
            pinField.setText("");
            confirmPinField.setText("");
        }
    }
}
