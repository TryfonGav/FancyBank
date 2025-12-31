import javax.swing.text.*;
import java.awt.Toolkit;

public class IntegerInputFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        if (string.matches("\\d*")) { // Only allow digits
            super.insertString(fb, offset, string, attr);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (text.matches("\\d*")) {
            super.replace(fb, offset, length, text, attrs);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }
}