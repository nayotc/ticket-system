package DomainLayer.company;

public class PurchasePolicy {
    private String description;

    public PurchasePolicy(String description)
    {
        this.description=description;
    }
    public String getDescription() {
        return description;
    }
    public void SetDescription(String description) {
        this.description=description;
    }
}
