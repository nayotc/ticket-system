package ticketsystem.DomainLayer.policy;

public class PolicyResult {

    private final boolean allowed;
    private final String message;

    private PolicyResult(boolean allowed, String message) {
        this.allowed = allowed;
        this.message = message;
    }

    public static PolicyResult allowed() {
        return new PolicyResult(true, null);
    }

    public static PolicyResult denied(String message) {
        return new PolicyResult(false, message);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getMessage() {
        return message;
    }
}
