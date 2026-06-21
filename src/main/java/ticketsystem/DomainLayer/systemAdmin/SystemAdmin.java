package ticketsystem.DomainLayer.systemAdmin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "system_admins")
public class SystemAdmin {

    @Id
    @Column(name = "admin_id", nullable = false, updatable = false)
    private String adminId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected SystemAdmin() {
    }

    public SystemAdmin(String adminId, String username, boolean active) {
        if (adminId == null || adminId.isBlank()) {
            throw new IllegalArgumentException("System admin ID cannot be null or blank");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("System admin username cannot be null or blank");
        }

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

    public long getVersion() {
        return version;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }
}