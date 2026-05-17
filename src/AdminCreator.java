import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

public class AdminCreator extends JFrame {
    private final JTextField usernameField = new JTextField();
    private final JPasswordField pinField = new JPasswordField();
    private final JPasswordField confirmPinField = new JPasswordField();

    public static void main(String[] args) {
        AppUi.installLookAndFeel(true);
        SwingUtilities.invokeLater(AdminCreator::new);
    }

    public AdminCreator() {
        super("FancyBank Admin Creator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(420, 300));
        setLocationRelativeTo(null);
        setContentPane(createContent());
        pack();
        setVisible(true);
    }

    private JPanel createContent() {
        JPanel panel = new JPanel(new MigLayout(
                "fill, insets 28, gap 12",
                "[grow, fill]",
                "[]16[]16[]"
        ));

        JLabel title = new JLabel("Create First Administrator");
        title.putClientProperty("FlatLaf.styleClass", "h2");
        title.setForeground(AppUi.BRAND_BLUE);

        JPanel form = new JPanel(new MigLayout("fillx, insets 0, gap 10", "[][grow, fill]", "[][][]"));
        form.add(new JLabel("Username"));
        form.add(usernameField, "growx, wrap");
        form.add(new JLabel("PIN"));
        form.add(pinField, "growx, wrap");
        form.add(new JLabel("Confirm PIN"));
        form.add(confirmPinField, "growx");

        JButton createButton = AppUi.primaryButton("Create Admin");
        createButton.addActionListener(e -> createAdmin());

        panel.add(title, "center, wrap");
        panel.add(form, "growx, wrap");
        panel.add(createButton, "right, w 150!");
        return panel;
    }

    private void createAdmin() {
        String username = usernameField.getText().trim();
        char[] pin = pinField.getPassword();
        char[] confirmPin = confirmPinField.getPassword();

        try {
            if (username.isEmpty() || pin.length == 0) {
                showError("Enter both username and PIN.");
                return;
            }
            if (!Arrays.equals(pin, confirmPin)) {
                showError("PINs do not match.");
                return;
            }

            boolean created = UserManager.bootstrapAdmin(username, pin);
            if (!created) {
                showError("An administrator already exists. Use the authenticated admin panel to promote users.");
                return;
            }

            JOptionPane.showMessageDialog(this, "Administrator created successfully.", "Admin Created", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } finally {
            Arrays.fill(pin, '\0');
            Arrays.fill(confirmPin, '\0');
            pinField.setText("");
            confirmPinField.setText("");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Admin Creator", JOptionPane.ERROR_MESSAGE);
    }
}
