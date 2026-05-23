package ticketsystem.DomainLayer.notifications;

import java.time.LocalDateTime;

public class Notification {

    private long id;
    private final Long recipientMemberId;
    private final String recipientSessionId;
    private final String message;
    private final LocalDateTime createdAt;

    private NotificationStatus status;

    public Notification(
            Long recipientMemberId,
            String recipientSessionId,
            String message
    ) {
        if (recipientMemberId == null && (recipientSessionId == null || recipientSessionId.isBlank())) {
            throw new IllegalArgumentException("Notification must have either member id or session id");
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Notification message cannot be blank");
        }

        this.recipientMemberId = recipientMemberId;
        this.recipientSessionId = recipientSessionId;
        this.message = message;
        this.createdAt = LocalDateTime.now();
        this.status = NotificationStatus.PENDING;
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

    public String getRecipientSessionId() {
        return recipientSessionId;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void markPending() {
        this.status = NotificationStatus.PENDING;
    }

    public void markDelivered() {
        this.status = NotificationStatus.DELIVERED;
    }

    public void markRead() {
        this.status = NotificationStatus.READ;
    }

    public boolean isPending() {
        return status == NotificationStatus.PENDING;
    }

    public boolean isForMember(long memberId) {
        return recipientMemberId != null && recipientMemberId.equals(memberId);
    }
}
