package ticketsystem.DomainLayer.user;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "company_roles")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role_type", discriminatorType = DiscriminatorType.STRING)
public abstract class CompanyRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roleId;

    @Column(name = "company_id", nullable = false)
    protected Long companyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    protected RoleStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    protected CompanyRole() {
    }

    public CompanyRole(Long companyId) {
        this.companyId = companyId;
        this.status = RoleStatus.PENDING;
    }

    public Long getRoleId() {
        return roleId;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public RoleStatus getStatus() {
        return status;
    }

    public void setStatus(RoleStatus status) {
        this.status = status;
    }

    public void activate() {
        this.status = RoleStatus.ACTIVE;
    }

    public abstract boolean hasPermission(Permission permission);

    public boolean isActive() {
        return this.status == RoleStatus.ACTIVE;
    }

    public boolean isPending() {
        return this.status == RoleStatus.PENDING;
    }

    public boolean isCancelled() {
        return this.status == RoleStatus.CANCELLED;
    }

    public void cancel() {
        this.status = RoleStatus.CANCELLED;
    }

    Member getMember() {
        return member;
    }

    void setMember(Member member) {
        this.member = member;
    }
}
