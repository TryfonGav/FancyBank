    import javax.swing.*;
    import java.awt.*;
    import java.awt.event.*;

    public class SmoothButton extends JButton {
        private Color targetColor;
        private Timer colorTransition;
        private static final int TRANSITION_DURATION = 10;

        public SmoothButton(String text, Color brandBlue, Color startColor, Color hoverColor, Font font) {
            super(text);
            setFont(font);
            setBackground(startColor);
            setForeground(Color.black);
            setFocusPainted(false);
            setOpaque(true);
            setBorder(BorderFactory.createLineBorder(startColor.darker(), 2));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    smoothTransition(hoverColor);
                }

                public void mouseExited(MouseEvent e) {
                    smoothTransition(startColor);
                }
            });
        }

        private void smoothTransition(Color newColor) {
            if (colorTransition != null && colorTransition.isRunning()) {
                colorTransition.stop();
            }
            targetColor = newColor;
            colorTransition = new Timer(TRANSITION_DURATION, evt -> {
                Color currentColor = getBackground();
                int r = (int) (currentColor.getRed() + (targetColor.getRed() - currentColor.getRed()) * 0.1);
                int g = (int) (currentColor.getGreen() + (targetColor.getGreen() - currentColor.getGreen()) * 0.1);
                int b = (int) (currentColor.getBlue() + (targetColor.getBlue() - currentColor.getBlue()) * 0.1);
                Color intermediate = new Color(r, g, b);
                setBackground(intermediate);
                if (intermediate.equals(targetColor)) {
                    colorTransition.stop();
                }
            });
            colorTransition.start();
        }
    }