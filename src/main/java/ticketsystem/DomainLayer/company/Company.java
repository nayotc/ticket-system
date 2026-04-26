package ticketsystem.DomainLayer.company;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Company {
    private String name;
    private final String founderUsername; 
    private boolean isActive;
    private List<String> owners; 
    private List<String> managers; 
    private PurchasePolicy purchasePolicy; 
    private DiscountPolicy discountPolicy; 

    public Company(String name, String founderUsername, PurchasePolicy purchasePolicy, DiscountPolicy discountPolicy) {
        this.name = name;
        this.founderUsername = founderUsername;
        this.isActive = true; 
        this.owners = new ArrayList<>();
        this.owners.add(founderUsername);
        this.managers = new ArrayList<>();
        this.purchasePolicy = purchasePolicy;
        this.discountPolicy = discountPolicy;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFounderUsername() {
        return founderUsername;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive, String requestingUser) throws IllegalAccessException {
        if (!requestingUser.equals(founderUsername)) {
            throw new IllegalAccessException("Only the founder can change the company's activity status.");
        }
        this.isActive = isActive;
    }

    public List<String> getOwners() {
        return Collections.unmodifiableList(owners);
    }

    public void addOwner(String ownerUsername) {
        if (!owners.contains(ownerUsername)) {
            this.owners.add(ownerUsername);
        }
    }

    public List<String> getManagers() {
        return Collections.unmodifiableList(managers);
    }

    public void addManager(String managerUsername) {
        if (!managers.contains(managerUsername)) {
            this.managers.add(managerUsername);
        }
    }

    public PurchasePolicy getPurchasePolicy() {
        return purchasePolicy;
    }

    public void setPurchasePolicy(PurchasePolicy purchasePolicy) {
        this.purchasePolicy = purchasePolicy;
    }

    public DiscountPolicy getDiscountPolicy() {
        return discountPolicy;
    }

    public void setDiscountPolicy(DiscountPolicy discountPolicy) {
        this.discountPolicy = discountPolicy;
    }

    public void closeOrSuspend(String requestingUser) throws Exception {
    // 1. בדיקת הרשאות (Alternative flow)
    if (!this.founderUsername.equals(requestingUser)) {
        throw new Exception("The system rejects the request due to lack of permissions");
    }

    // 2. בדיקת תנאי קדם (Precondition)
    if (!this.isActive) {
        throw new Exception("Company is already inactive");
    }

    // 3. שינוי סטטוס (Main Scenario)
    this.isActive = false;
    
}
}