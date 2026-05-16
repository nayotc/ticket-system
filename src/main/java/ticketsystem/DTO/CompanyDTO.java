package ticketsystem.DTO;



import ticketsystem.DomainLayer.company.Company;

public class CompanyDTO {
    private long id;
    private String name;
    private long founderId;
    private boolean isActive;


    public CompanyDTO(Company company) {
        this.id = company.getId();
        this.name = company.getName();
        this.founderId = company.getFounderId();
        this.isActive = company.isActive();
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
}
