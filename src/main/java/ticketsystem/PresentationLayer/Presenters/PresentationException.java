package ticketsystem.PresentationLayer.Presenters;

import java.util.function.Function;

public class PresentationException extends RuntimeException {

    public static final String SESSION_TOKEN_EXPIRED = "פג תוקף החיבור למערכת. יש להתחבר מחדש.";
    public static final String DB_DISCONNECT_HEBREW_MSG = "השירות אינו זמין זמנית עקב ניתוק החיבור ל-DB. אנא נסו שוב עוד מספר רגעים.";

    public PresentationException(String message) {
        super(message);
    }

    public static PresentationException dispatch(Exception rawException, Function<String, String> translator) {
        if (rawException == null) {
            return new PresentationException("אירעה שגיאה.");
        }

        // 1. Handle DB disconnections
        if (isDbDisconnect(rawException)) {
            return new PresentationException(DB_DISCONNECT_HEBREW_MSG);
        }

        String topMsg = rawException.getMessage();

        // 2. Handle session token expiration
        if (topMsg != null && isSessionTimeoutMessage(topMsg)) {
            return new PresentationException(SESSION_TOKEN_EXPIRED);
        }

        // 3. Apply business-specific translation logic
        String businessTranslated = translator.apply(topMsg);
        return new PresentationException(businessTranslated != null ? businessTranslated : "אירעה שגיאה.");
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

    /**
     * Extracts the deepest root-cause error message from the given exception.
     */
    public static String extractUsefulMessage(Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        if (current.getMessage() != null && !current.getMessage().isBlank()) {
            return current.getMessage();
        }

        return throwable.getMessage() != null ? throwable.getMessage() : "";
    }

    public static boolean isDbDisconnect(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        
        String deepestMsg = extractUsefulMessage(throwable);
        return isDbDisconnectMessage(deepestMsg);
    }
}