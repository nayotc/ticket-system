package ticketsystem.DomainLayer.user;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Suspension {

    @Column(name = "suspended_by_admin_id")
    private Long suspendedByAdminId;

    @Column(name = "suspension_start_date")
    private LocalDateTime startDate;

    @Column(name = "suspension_end_date")
    private LocalDateTime endDate;

    @Column(name = "suspension_reason")
    private String reason;

    @Column(name = "suspension_revoked")
    private boolean revoked;

    protected Suspension() {
    }

    public Suspension(Long suspendedByAdminId,
                      LocalDateTime startDate,
                      LocalDateTime endDate,
                      String reason) {
        if (suspendedByAdminId == null) {
            throw new IllegalArgumentException(
                    "Suspended by admin id cannot be null");
        }

        if (startDate == null) {
            throw new IllegalArgumentException(
                    "Start date cannot be null");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Reason cannot be null or blank");
        }

        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "you cannot enter an end time that is before the current time");
        }

        this.suspendedByAdminId = suspendedByAdminId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.revoked = false;
    }

    public Suspension(Suspension other) {
        this.suspendedByAdminId = other.suspendedByAdminId;
        this.startDate = other.startDate;
        this.endDate = other.endDate;
        this.reason = other.reason;
        this.revoked = other.revoked;
    }

    public boolean isActive() {
        if (revoked) {
            return false;
        }

        if (endDate == null) {
            return true;
        }

        return LocalDateTime.now().isBefore(endDate);
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void revoke() {
        this.revoked = true;
    }

    public Long getSuspendedByAdminId() {
        return suspendedByAdminId;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public String getReason() {
        return reason;
    }

    public boolean isPermanent() {
        return endDate == null;
    }
}
