package ticketsystem.DomainLayer.systemAdmin;

public class SystemAdmin {
    private String adminId;
    private String username;
    private boolean active;

    public SystemAdmin(String adminId, String username, boolean active) {
        this.adminId = adminId;
        this.username = username;
        this.active = active;
    }

    public String getAdminId() {
        return adminId;
    }

    public String getUsername() {
        return username;
    }

  
    public boolean isActive() {
        return active;
    }

    public void activate() {
        this.active = true;
    }
}