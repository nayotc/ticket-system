package DomainLayer.company;

public class Company {
    private String name;
    private Boolean isActive;
    private PurchasePolicy purchasePolicy;
    private DiscountPolicy discountPolicy;

    public Company(String name, Boolean isActive, PurchasePolicy purchasePolicy,DiscountPolicy discountPolicy)
    {
        this.name=name;
        this.isActive=isActive;
        this.discountPolicy=discountPolicy;
        this.purchasePolicy=purchasePolicy;
    }
    public String getName()
    {
        return this.name;
    }
    public void setName(String name)
    {
        this.name=name;
    }
    public Boolean getIsActive()
    {
        return this.isActive;
    }
    public void setIsActive(boolean isActive)
    {
        this.isActive=isActive;
    }
        public void setPurchasePolicy(PurchasePolicy purchasePolicy) {
        this.purchasePolicy = purchasePolicy;
    }

    public void setDiscountPolicy(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }

    public PurchasePolicy getPurchasePolicy() {
        return purchasePolicy;
    }

    public DiscountPolicy getDiscountPolicy() {
        return discountPolicy;
    }
}
