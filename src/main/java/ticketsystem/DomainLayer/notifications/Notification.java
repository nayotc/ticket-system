package ticketsystem.DomainLayer.notifications;


public class Notification {

    public enum Type {
        INFO,
        ACTION
    }

    private Long id;
    private String targetId;
    private String message;
    private Long companyId;
    private Type type;

    public Notification() {
    }

    public Notification(String targetId, String message, Long companyId, Type type) {
        this.targetId = targetId;
        this.message = message;
        this.companyId = companyId;
        this.type = type;
    }
    public Notification(String targetId, String message, Type type) {
        this.targetId = targetId;
        this.message = message;
        this.companyId = null;
        this.type = type;
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
    public Long getCompanyId() {
        return companyId;
    }
    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
