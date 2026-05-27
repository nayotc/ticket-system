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
    // Permanent suspension
    public Suspension(Long suspendedByAdminId,
                      LocalDateTime startDate,
                      String reason) {

        this.suspendedByAdminId = suspendedByAdminId;
        this.startDate = startDate;
        this.endDate = null;
        this.reason = reason;
        this.revoked = false;
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

}