package ticketsystem.DomainLayer.notifications;

public class Notification {

    private Long id;
    private String targetId;
    private String message;

    public Notification() {
    }

    public Notification(String targetId, String message) {
        this.targetId = targetId;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
