package ticketsystem.PresentationLayer.Presenters;

import java.util.function.Function;

public class PresentationException extends RuntimeException {

    public static final String SESSION_TOKEN_EXPIRED = "פג תוקף החיבור למערכת. יש להתחבר מחדש.";
    public static final String DB_DISCONNECT_HEBREW_MSG = "השירות אינו זמין זמנית עקב ניתוק החיבור ל-DB. אנא נסו שוב עוד מספר רגעים.";

    public PresentationException(String message) {
        super(message);
    }

    public static PresentationException dispatch(Exception rawException, Function<String, String> translator) {
        if (rawException instanceof PresentationException pe) {
            return pe; 
        }
        
        // 1. Handle DB disconnections
        if (isDbDisconnect(rawException)) {
            return new PresentationException(DB_DISCONNECT_HEBREW_MSG);
        }

        if (rawException == null) {
            return new PresentationException("אירעה שגיאה.");
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

        String lower = message.toLowerCase();

        return message.equals(SESSION_TOKEN_EXPIRED) ||
            lower.contains("jwt") ||
            lower.contains("expired") ||
            lower.contains("invalid token") ||
            lower.contains("invalid session id") ||
            lower.contains("token is missing") ||
            lower.contains("session is no longer active") ||
            lower.contains("session authentication failed");
    }
    
public static boolean isDbDisconnectMessage(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        // אם זו כבר ההודעה המתורגמת שלנו ממעלה הסטאק:
        if (message.equals(DB_DISCONNECT_HEBREW_MSG)) {
            return true;
        }

        String lower = message.toLowerCase();

        return 
            // --- שכבה 1: מערכת ההפעלה וכרטיס הרשת (OS & TCP Sockets) ---
            lower.contains("connection refused") ||
            lower.contains("connection reset") ||             
            lower.contains("broken pipe") ||                  
            lower.contains("no route to host") ||
            lower.contains("noroutetohost") ||
            lower.contains("network is unreachable") ||
            lower.contains("host is down") ||
            lower.contains("sockettimeout") ||
            lower.contains("connect timed out") ||
            lower.contains("read timed out") ||

            // --- שכבה 2: הדרייבר הרשמי של פוסטגרס (Postgres JDBC Driver) ---
            lower.contains("the connection attempt failed") ||
            lower.contains("connection attempt timed out") ||
            lower.contains("an i/o error occurred") ||        
            lower.contains("server closed the connection") || 
            lower.contains("terminating connection") ||       

            // --- שכבה 3: בריכת החיבורים (HikariCP Pool) ---
            lower.contains("connection is not available") ||
            lower.contains("pool is empty") ||
            lower.contains("this connection has been closed") ||
            lower.contains("connection pool shut down") ||
            lower.contains("hikari") ||

            // --- שכבות 4+5: ה-ORM והפריימוורק (Hibernate & Spring JPA) ---
            lower.contains("cannotcreatetransaction") ||
            lower.contains("transactionsystemexception") ||
            lower.contains("jdbcconnection") ||
            lower.contains("unable to acquire jdbc") ||
            lower.contains("could not open jpa") ||
            lower.contains("dataaccessresourcefailure");
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