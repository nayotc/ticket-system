package ticketsystem.DomainLayer.notifications;

public class Notification {

    private long id;
    private final Long recipientMemberId;
    private final String message;

    public Notification(
            Long recipientMemberId,
            String message
    ) {
        // if (recipientMemberId == null) {
        //     throw new IllegalArgumentException("Notification must have a member id");
        // }

        // if (message == null || message.isBlank()) {
        //     throw new IllegalArgumentException("Notification message cannot be blank");
        // }
        this.recipientMemberId = recipientMemberId;
        this.message = message;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getRecipientMemberId() {
        return recipientMemberId;
    }

    public String getMessage() {
        return message;
    }
}
