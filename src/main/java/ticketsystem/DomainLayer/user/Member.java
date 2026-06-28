package ticketsystem.DomainLayer.user;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;

@Entity
@DiscriminatorValue("MEMBER")
public class Member extends User {

    @Column(name = "user_name", nullable = false, unique = true)
    private String userName;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "hashed_password")
    private String hashedPassword;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CompanyRole> companyRoles = new ArrayList<>();

    @Embedded
    private Suspension suspension;

    protected Member() {
    }

    public Member(Long memberId, String userName, String fullName, String phone, LocalDate birthDate) {
        validateBirthDate(birthDate);
        if (memberId != null) {
            setId(memberId);
        }
        this.userName = userName;
        this.fullName = fullName;
        this.phone = phone;
        this.birthDate = birthDate;
        setVersion(0);
    }

    public Member(Member other) {
        setId(other.getId());
        this.userName = other.userName;
        this.fullName = other.fullName;
        this.phone = other.phone;
        this.birthDate = other.birthDate;
        this.hashedPassword = other.hashedPassword;
        setVersion(other.getVersion());
        this.suspension = other.suspension == null ? null : new Suspension(other.suspension);
        this.companyRoles = new ArrayList<>();
        this.isActive = other.isActive;
        for (CompanyRole originalRole : other.companyRoles) {
            CompanyRole copiedRole = copyRole(originalRole);
            if (copiedRole != null) {
                copiedRole.setMember(this);
                this.companyRoles.add(copiedRole);
            }
        }
    }

    private CompanyRole copyRole(CompanyRole originalRole) {
        Long companyId = originalRole.getCompanyId();
        if (originalRole instanceof Founder founder) {
            return new Founder(founder, companyId);
        }
        if (originalRole instanceof Owner owner) {
            return new Owner(owner, companyId);
        }
        if (originalRole instanceof Manager manager) {
            return new Manager(manager, companyId);
        }
        return null;
    }

    public Long getId() {
        return super.getId();
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getFullName() {
        return this.fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }
    
    public boolean isActive() {
        return isActive;
    }

    public void deactivate() {
        this.isActive = false;
    }
    public List<CompanyRole> getAllRoles() {
        return companyRoles.stream().collect(Collectors.toList());
    }

    public CompanyRole getRoleInCompany(Long companyId) {
        return companyRoles.stream()
                .filter(role -> Objects.equals(role.getCompanyId(), companyId))
                .findFirst()
                .orElse(null);
    }

    public boolean hasPermission(Long companyId, Permission permission) {
        CompanyRole role = getRoleInCompany(companyId);
        return role != null && role.hasPermission(permission);
    }

    public boolean addManagerRole(Long companyId, Long memberId, Set<Permission> permissions) {
        if (getRoleInCompany(companyId) != null) {
            return false;
        }
        Manager newRole = new Manager(companyId, memberId, permissions);
        newRole.setMember(this);
        companyRoles.add(newRole);
        return true;
    }

    public boolean addOwnerRole(Long companyId, Long memberId) {
        if (getRoleInCompany(companyId) != null) {
            return false;
        }
        Owner newRole = new Owner(companyId, memberId);
        newRole.setMember(this);
        companyRoles.add(newRole);
        return true;
    }

    public boolean addFounderRole(Long companyId) {
        if (getRoleInCompany(companyId) != null) {
            return false;
        }
        Founder newRole = new Founder(companyId);
        newRole.setMember(this);
        companyRoles.add(newRole);
        return true;
    }

    public boolean deleteRoleInCompany(Long companyId) {
        return companyRoles.removeIf(role -> Objects.equals(role.getCompanyId(), companyId));
    }

    public void updateManagerPermissions(Long companyId, Set<Permission> newPermissions) {
        CompanyRole role = getRoleInCompany(companyId);
        if (role instanceof Manager manager) {
            manager.setPermissions(newPermissions);
        }
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date cannot be null");
        }

        if (birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Birth date cannot be in the future");
        }
    }

    public void suspendMember(Long suspendedByAdminId,
                              LocalDateTime startDate,
                              LocalDateTime endDate,
                              String reason) {
        if (isSuspended()) {
            throw new IllegalStateException("Member is already suspended");
        }
        this.suspension = new Suspension(suspendedByAdminId, startDate, endDate, reason);
    }

    public void revokeSuspension() {
        Suspension activeSuspension = getSuspension();
        if (activeSuspension == null || !activeSuspension.isActive()) {
            throw new IllegalStateException("Member is not suspended");
        }
        activeSuspension.revoke();
    }

    public boolean isSuspended() {
        if (suspension == null) {
            return false;
        }
        return suspension.isActive();
    }

    public Suspension getSuspension() {
        return suspension;
    }

    List<CompanyRole> getCompanyRolesInternal() {
        return companyRoles;
    }
}
