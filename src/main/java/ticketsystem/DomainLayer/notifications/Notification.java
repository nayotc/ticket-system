package ticketsystem.DomainLayer.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
public class Notification {

    public enum Type {
        INFO,
        ACTION
    }
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "company_id")
    private Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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
