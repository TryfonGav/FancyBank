import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import javax.swing.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class AppUi {
    public static final Color BRAND_BLUE = new Color(37, 99, 235);
    public static final Color BRAND_GOLD = new Color(217, 119, 6);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color SURFACE_LINE = new Color(226, 232, 240);
    public static final Color DARK_SURFACE_LINE = new Color(51, 65, 85);

    public static final String USER_SVG = """
            <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path fill="#64748B" d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm0 2c-4.42 0-8 2.24-8 5v1h16v-1c0-2.76-3.58-5-8-5Z"/>
            </svg>
            """;

    public static final String ALERT_SVG = """
            <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path fill="#DC2626" d="M12 2 1 21h22L12 2Zm1 16h-2v-2h2v2Zm0-4h-2V8h2v6Z"/>
            </svg>
            """;

    public static final String ADMIN_SVG = """
            <svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path fill="#2563EB" d="M12 2 4 5v6c0 5.55 3.84 10.74 8 12 4.16-1.26 8-6.45 8-12V5l-8-3Zm-1 14-3-3 1.41-1.41L11 13.17l4.59-4.58L17 10l-6 6Z"/>
            </svg>
            """;

    private AppUi() {
    }

    public static void installLookAndFeel(boolean darkMode) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        FlatInterFont.install();
        UIManager.put("defaultFont", new Font("Inter", Font.PLAIN, 13));
        UIManager.put("Component.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ProgressBar.arc", 8);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TitlePane.unifiedBackground", true);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.innerFocusWidth", 0);

        if (darkMode) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
    }

    public static void setDarkMode(boolean darkMode) {
        if (darkMode) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        FlatLaf.updateUI();
    }

    public static JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.putClientProperty("FlatLaf.style", "background: #2563EB; foreground: #FFFFFF; borderWidth: 0; focusWidth: 1");
        return button;
    }

    public static JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.putClientProperty("JButton.buttonType", "roundRect");
        return button;
    }

    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.putClientProperty("FlatLaf.styleClass", "h3");
        return label;
    }

    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.putClientProperty("FlatLaf.style", "arc: 10; border: 1,1,1,1,#94A3B8,,10");
        return panel;
    }

    public static Icon svgIcon(String svg, int size) {
        ByteArrayInputStream in = new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8));
        try {
            return new FlatSVGIcon(in).derive(size, size);
        } catch (IOException ex) {
            return UIManager.getIcon("OptionPane.informationIcon");
        }
    }

    public static boolean isDark() {
        return FlatLaf.isLafDark();
    }
}
