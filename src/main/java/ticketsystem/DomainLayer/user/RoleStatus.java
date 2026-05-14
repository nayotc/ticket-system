package ticketsystem.DomainLayer.user;

public enum RoleStatus {

    // Awaiting member approval
    PENDING,
    // Approved and fully authorized
    ACTIVE,
    // Role was cancelled or removed
    CANCELLED
    
}