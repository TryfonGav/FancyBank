import javax.swing.text.*;
import java.awt.Toolkit;

public class DecimalInputFilter extends DocumentFilter {
    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        if (isValid(fb.getDocument().getText(0, fb.getDocument().getLength()), string, offset)) {
            super.insertString(fb, offset, string, attr);
        } else {
            Toolkit.getDefaultToolkit().beep(); // Alert user they can't type that
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        if (isValid(fb.getDocument().getText(0, fb.getDocument().getLength()), text, offset)) {
            super.replace(fb, offset, length, text, attrs);
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private boolean isValid(String currentText, String newText, int offset) {
        // Construct what the result would be
        StringBuilder result = new StringBuilder(currentText);
        result.insert(offset, newText);
        String text = result.toString();

        // Allow empty string (user deleted everything)
        if (text.isEmpty()) return true;

        // Regex: Optional minus sign, digits, optional decimal point, optional 1 or 2 digits
        // Matches: "123", "123.", "123.5", "123.55", ".5"
        return text.matches("^-?[0-9]*\\.?[0-9]{0,2}$");
    }
}