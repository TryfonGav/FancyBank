import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.util.Enumeration;

public class ThemeManager {
    // --- Color Palette ---
    public static final Color DARK_BG = new Color(40, 40, 40);
    public static final Color LIGHT_BG = Color.WHITE;

    public static final Color DARK_FG = Color.LIGHT_GRAY;
    public static final Color LIGHT_FG = Color.BLACK;

    public static final Color DARK_ACCENT = new Color(60, 60, 60); // For text fields/panels in dark mode
    public static final Color LIGHT_ACCENT = new Color(245, 245, 245);

    public static final Color BRAND_BLUE = new Color(0, 102, 204);
    public static final Color BRAND_GOLD = new Color(204, 153, 0);
    public static final Color WARNING_RED = new Color(255, 102, 102);

    // --- Font Settings ---
    public static final Font HEADER_FONT = new Font("Serif", Font.BOLD, 24);
    public static final Font STANDARD_FONT = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font MONO_FONT = new Font("Monospaced", Font.PLAIN, 12);

    /**
     * Applies the theme to a specific container and all its children recursively.
     */
    public static void applyTheme(Container container, boolean isDarkMode) {
        Color bg = isDarkMode ? DARK_BG : LIGHT_BG;
        Color fg = isDarkMode ? DARK_FG : LIGHT_FG;
        Color accent = isDarkMode ? DARK_ACCENT : LIGHT_ACCENT;

        // Apply to the container itself
        if (!(container instanceof JButton)) { // Let buttons handle their own style usually
            container.setBackground(bg);
            container.setForeground(fg);
        }

        for (Component comp : container.getComponents()) {
            if (comp instanceof JPanel) {
                applyTheme((Container) comp, isDarkMode);
            }
            else if (comp instanceof JTextArea || comp instanceof JTextField) {
                comp.setBackground(accent);
                comp.setForeground(isDarkMode ? Color.WHITE : Color.BLACK);
                if (comp instanceof javax.swing.text.JTextComponent) {
                    ((javax.swing.text.JTextComponent) comp).setCaretColor(isDarkMode ? Color.WHITE : Color.BLACK);
                }
            }
            else if (comp instanceof JLabel) {
                comp.setForeground(fg);
            }
            else if (comp instanceof JScrollPane) {
                JScrollPane scroll = (JScrollPane) comp;
                scroll.getViewport().setBackground(bg);
                // Recursively style the view inside the scroll pane
                if (scroll.getViewport().getView() instanceof Container) {
                    applyTheme((Container) scroll.getViewport().getView(), isDarkMode);
                }
            }
        }
    }
}