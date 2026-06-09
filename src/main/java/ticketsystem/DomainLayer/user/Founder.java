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
@DiscriminatorValue("FOUNDER")
public class Founder extends CompanyRole {

    @ElementCollection
    @CollectionTable(name = "founder_appointees", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "appointee_member_id")
    private List<Long> appointeesMemberIds = new ArrayList<>();

    protected Founder() {
    }

    public Founder(Long companyId) {
        super(companyId);
        this.status = RoleStatus.ACTIVE;
    }

    public Founder(Founder other, Long companyId) {
        super(companyId);
        this.status = other.status;
        this.appointeesMemberIds = new ArrayList<>(other.appointeesMemberIds);
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
        return true;
    }
}
