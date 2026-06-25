package ticketsystem.PresentationLayer.Presenters;

public class PresentationException extends RuntimeException {

    public static final String SESSION_TOKEN_EXPIRED = "פג תוקף החיבור למערכת. יש להתחבר מחדש.";
    public static final String DB_DISCONNECT_HEBREW_MSG = "השירות אינו זמין זמנית עקב ניתוק החיבור ל-DB. אנא נסו שוב עוד מספר רגעים.";

    public PresentationException(String message) {
        super(message);
    }

    public static boolean isSessionTimeoutMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        return message.contains("JWT") ||
               message.contains("Invalid token") || 
               message.contains("Invalid session") ||
               message.contains("Invalid or expired") ||
               message.contains("ID not found in token") ||
               message.contains("Token is missing or null") ||
               message.contains("Session is no longer active") ||
               message.contains("Session authentication failed");
    }

    public static boolean isDbDisconnectMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        return message.contains("Connection refused") ||
               message.contains("Communications link failure") ||
               message.contains("CannotCreateTransactionException") ||
               message.contains("JDBCConnectionException") ||
               message.contains("DataAccessResourceFailureException");
    }
}