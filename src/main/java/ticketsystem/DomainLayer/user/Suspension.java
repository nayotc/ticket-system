package ticketsystem.DomainLayer.user;

import java.time.LocalDateTime;

public class Suspension {

    private final Long suspendedByAdminId;

    private final LocalDateTime startDate;

    private final LocalDateTime endDate;

    private final String reason;

    private boolean revoked;
   
    public Suspension(Long suspendedByAdminId,
                      LocalDateTime startDate,
                      LocalDateTime endDate,
                      String reason) {
        if(suspendedByAdminId == null) {
            throw new IllegalArgumentException(
                    "Suspended by admin id cannot be null");
        }

        if(startDate == null) {
            throw new IllegalArgumentException(
                    "Start date cannot be null");
        }

        if(reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Reason cannot be null or blank");
        }
        
        if(endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "End date cannot be before start date");
        }

        this.suspendedByAdminId = suspendedByAdminId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.revoked = false;
    }
    //Copy constructor
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
            return true; // permanent suspension
        }

    return LocalDateTime.now().isBefore(endDate);   
    }


    public boolean isRevoked(){
        return revoked;
    }
    void revoke(){
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