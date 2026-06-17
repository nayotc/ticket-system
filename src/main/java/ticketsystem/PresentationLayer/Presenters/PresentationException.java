package ticketsystem.PresentationLayer.Presenters;

public class PresentationException extends RuntimeException {

    public PresentationException(String message) {
        super(message);
    }

    public boolean isSessionTimeout() {
        String msg = getMessage();
        if (msg == null) return false;
        
        return msg.contains("פג תוקף") || 
               msg.contains("לא פעיל") ||
               msg.contains("JWT expired") ||
               msg.contains("Invalid token") ||
               msg.contains("Invalid session ID") ||
               msg.contains("Invalid or expired") ||
               msg.contains("Token is missing or null") ||
               msg.contains("Session is no longer active");
    }
}