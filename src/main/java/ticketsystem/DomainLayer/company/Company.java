package ticketsystem.DomainLayer.company;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class Company {
    private static long idCounter = 1; 
    
    private long id; 
    private String name;
    private final String founderUsername; 
    private boolean isActive;
    private List<String> owners; 
    private List<String> managers; 
    private final CompanyTree rolesTree;
    private PurchasePolicy purchasePolicy; 
    private DiscountPolicy discountPolicy; 

    public Company(String name, String founderUsername, PurchasePolicy purchasePolicy, DiscountPolicy discountPolicy) {
        this.id = idCounter++; 
        
        this.name = name;
        this.founderUsername = founderUsername;
        this.isActive = true; 
        
        this.owners = new ArrayList<>();
        this.owners.add(founderUsername); 
        
        this.managers = new ArrayList<>();
        this.purchasePolicy = purchasePolicy;
        this.discountPolicy = discountPolicy;
        
        this.rolesTree = new CompanyTree(founderUsername);
    }

    // --- Getters & Setters ---

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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

    public boolean isActive() {
        return isActive;
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

    private boolean isFounder(String user) {
        return user.equals(this.founderUsername);
    }

    private boolean isOwner(String user) {
        return owners.contains(user); 
    }

    // --- Use Cases Logic ---

    public void closeOrSuspend(String requestingUser) throws Exception {
        if (!isFounder(requestingUser)) { // if not founder - throw exception
            throw new Exception("The system rejects the request due to lack of permissions. Only the Founder can close the company.");
        }

        if (!this.isActive) { // if the company is already inactive
            throw new Exception("Company is already inactive.");
        }

        this.isActive = false;
    }

    public void reopenCompany(String requestingUser) throws Exception {
        if (!isFounder(requestingUser)) { // if not founder - throw exception
            throw new Exception("The system rejects the request due to lack of permissions. Only the Founder can reopen the company.");
        }

        if (this.isActive) { // if the company is already active
            throw new Exception("The company is already Active. No action needed.");
        }

        this.isActive = true;
    }

    public String getRolesTreeRepresentation(String requestingUser, Map<String, String> userPermissions) throws Exception {
            if (!isOwner(requestingUser)) { 
                throw new Exception("The system rejects the request due to lack of permissions. Only Owners can view the roles tree.");
            }
            
            return this.rolesTree.getStructuredData(userPermissions);
        }

    public void registerNewAppointment(String appointer, String appointee) { // insert the appointment to the tree
        rolesTree.addAppointment(appointer, appointee);
    }

    public void removeUserFromAllRoles(String userId) {
        this.owners.remove(userId);
        this.managers.remove(userId);
        this.rolesTree.removeNode(userId);
    }
}