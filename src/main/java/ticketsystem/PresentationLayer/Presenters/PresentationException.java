package ticketsystem.PresentationLayer.Presenters;

public class PresentationException extends RuntimeException {

    public PresentationException(String message) {
        super(message);
    }

    public boolean isSessionTimeout() {
        String msg = getMessage();
        if (msg == null) return false;
        
        return msg.contains("פג תוקף") || 
               msg.contains("לא תקין") || 
               msg.contains("לא פעיל") ||
               msg.contains("JWT expired") || 
               msg.contains("Invalid or expired");
    }
}