import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class TransactionRecord {
    private String type; // Deposit or Withdrawal
    private double amount;
    private LocalDateTime timestamp;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TransactionRecord(String type, double amount, LocalDateTime timestamp) {
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public TransactionRecord(String type, double amount) {
        this.type = type;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getTimestamp() {
        return timestamp.format(formatter);
    }

    public LocalDateTime getTimestampDate() {
        return timestamp;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: $%,.2f", getTimestamp(), type, amount);
    }

    public static TransactionRecord fromString(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        try {
            line = line.trim();

            // Expected format: "[2023-01-01 12:00:00] Deposit: $100.00 - Balance: $1000.00"
            // or simpler format: "[2023-01-01 12:00:00] Deposit: $100.00"

            // Find the timestamp part
            if (!line.startsWith("[")) {
                System.err.println("Line doesn't start with '[': " + line);
                return null;
            }

            int endBracket = line.indexOf("]");
            if (endBracket == -1 || endBracket <= 1) {
                System.err.println("Cannot find closing ']' in line: " + line);
                return null;
            }

            String timestampStr = line.substring(1, endBracket);
            LocalDateTime timestamp;
            try {
                timestamp = LocalDateTime.parse(timestampStr, formatter);
            } catch (DateTimeParseException e) {
                System.err.println("Cannot parse timestamp '" + timestampStr + "': " + e.getMessage());
                return null;
            }

            // Get the rest of the line after the timestamp
            String rest = line.substring(endBracket + 1).trim();

            // Find the colon that separates type from amount
            int colonIndex = rest.indexOf(":");
            if (colonIndex == -1) {
                System.err.println("Cannot find ':' in transaction part: " + rest);
                return null;
            }

            String type = rest.substring(0, colonIndex).trim();
            String amountPart = rest.substring(colonIndex + 1).trim();

            // Handle potential " - Balance: $xxx.xx" suffix
            if (amountPart.contains(" - Balance:")) {
                amountPart = amountPart.substring(0, amountPart.indexOf(" - Balance:")).trim();
            }

            // Remove $ sign and commas from amount
            if (amountPart.startsWith("$")) {
                amountPart = amountPart.substring(1);
            }
            amountPart = amountPart.replace(",", "");

            double amount;
            try {
                amount = Double.parseDouble(amountPart);
            } catch (NumberFormatException e) {
                System.err.println("Cannot parse amount '" + amountPart + "': " + e.getMessage());
                return null;
            }

            return new TransactionRecord(type, amount, timestamp);

        } catch (Exception e) {
            System.err.println("Unexpected error parsing transaction record: \"" + line + "\" - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}