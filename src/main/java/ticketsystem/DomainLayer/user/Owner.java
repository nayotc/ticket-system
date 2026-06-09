package ticketsystem.DomainLayer.user;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;

@Entity
@DiscriminatorValue("OWNER")
public class Owner extends CompanyRole {

    @ElementCollection
    @CollectionTable(name = "owner_appointees", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "appointee_member_id")
    private List<Long> appointeesMemberIds = new ArrayList<>();

    @Column(name = "appointed_by_member_id")
    private Long appointedByMemberId;

    protected Owner() {
    }

    public Owner(Long companyId, Long appointedByMemberId) {
        super(companyId);
        this.appointedByMemberId = appointedByMemberId;
        this.status = RoleStatus.PENDING;
    }

    public Owner(Owner other, Long companyId) {
        super(companyId);
        this.status = other.status;
        this.appointedByMemberId = other.appointedByMemberId;
        this.appointeesMemberIds = new ArrayList<>(other.appointeesMemberIds);
    }

    public Long getAppointedByMemberId() {
        return this.appointedByMemberId;
    }

    public void setAppointer(Long newAppointedByMemberId) {
        this.appointedByMemberId = newAppointedByMemberId;
    }

    public List<Long> getAppointeesMemberIds() {
        return this.appointeesMemberIds;
    }

    public void addAppointee(Long memberId) {
        this.appointeesMemberIds.add(memberId);
    }

    public void deleteAppointee(Long memberId) {
        this.appointeesMemberIds.remove(memberId);
    }

    @Override
    public boolean hasPermission(Permission permission) {
        return this.status == RoleStatus.ACTIVE;
    }
}
