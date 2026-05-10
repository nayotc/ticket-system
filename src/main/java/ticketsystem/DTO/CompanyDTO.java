package ticketsystem.DTO;

import java.util.ArrayList;
import java.util.List;

import ticketsystem.DomainLayer.company.Company;

public class CompanyDTO {
    private long id;
    private String name;
    private long founderId;
    private boolean isActive;
    private List<Long> owners;
    private List<Long> managers;

    public CompanyDTO(Company company) {
        this.id = company.getId();
        this.name = company.getName();
        this.founderId = company.getFounderId();
        this.isActive = company.isActive();
        this.owners = new ArrayList<>(company.getOwners());
        this.managers = new ArrayList<>(company.getManagers());
    }

    // --- Getters ---

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getFounderId() {
        return founderId;
    }

    public boolean isActive() {
        return isActive;
    }

    public List<Long> getOwners() {
        return owners;
    }

    public List<Long> getManagers() {
        return managers;
    }
}
