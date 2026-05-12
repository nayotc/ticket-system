package ticketsystem.DomainLayer.company;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class Company {
    private static long idCounter = 1; 
    
    private long id; 
    private String name;
    private final long founderId;
    private boolean isActive;
    private List<Long> owners; 
    private List<Long> managers;
    private CompanyTree rolesTree;
    private PurchasePolicy purchasePolicy; 
    private DiscountPolicy discountPolicy; 
    private Double rate = 0.0; // for search and filtering
    private Double totalRating = 0.0; // for calculating average rating
    private Integer ratingCount = 0; // for calculating average rating

    //Version field for Optimistic Locking
    private long version;

    public Company(String name, long founderId, PurchasePolicy purchasePolicy, DiscountPolicy discountPolicy) {
        this.id = idCounter++; 
        
        this.name = name;
        this.founderId = founderId;
        this.isActive = true; 
        
        this.owners = new ArrayList<>();
        this.owners.add(founderId); 
        
        this.managers = new ArrayList<>();
        this.purchasePolicy = purchasePolicy;
        this.discountPolicy = discountPolicy;
        
        this.rolesTree = new CompanyTree(founderId);

        this.version = 0; // Initialize version

        
    }

    // Copy Constructor
    public Company(Company other) {
        this.id = other.id;
        this.name = other.name;
        this.founderId = other.founderId;
        this.isActive = other.isActive;
        this.version = other.version;
        this.rate = other.rate;
        this.totalRating = other.totalRating;
        this.ratingCount = other.ratingCount;
        // Deep copy of the lists to prevent external modifications
        this.owners = new ArrayList<>(other.owners);
        this.managers = new ArrayList<>(other.managers);
        
        // Shallow copy for other objects (sufficient for this level of locking)
        this.rolesTree = other.rolesTree;
        this.purchasePolicy = other.purchasePolicy;
        this.discountPolicy = other.discountPolicy;
        this.rolesTree = new CompanyTree(other.rolesTree);
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

    public long getFounderUsername() {
        return founderId;
    }

    public boolean isActive() {
        return isActive;
    }

    public List<Long> getOwners() {
        return Collections.unmodifiableList(owners);
    }

    public void addOwner(long ownerId) {
        if (!owners.contains(ownerId)) {
            this.owners.add(ownerId);
        }
    }

    public List<Long> getManagers() {
        return Collections.unmodifiableList(managers);
    }

    public void addManager(long managerId) {
        if (!managers.contains(managerId)) {
            this.managers.add(managerId);
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

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    private boolean isFounder(long memberId) {
        return this.founderId == memberId;
    }

    private boolean isOwner(long memberId) {
        return owners.contains(memberId); 
    }

    public long getFounderId()
    {
        return this.founderId;
    }

    public Double getRate() {
        return this.rate;
    }

    public void setRate(Double rate) {
        this.totalRating += rate;
        this.ratingCount++;
        this.rate = this.totalRating / this.ratingCount;
    }

    public void inactivate() {
        this.isActive = false;
    }

    // --- Use Cases Logic ---

    public void closeOrSuspend(long requestingMemberId) throws Exception {
        if (!isFounder(requestingMemberId)) { // if not founder - throw exception
            throw new Exception("The system rejects the request due to lack of permissions. Only the Founder can close the company.");
        }

        if (!this.isActive) { // if the company is already inactive
            throw new Exception("Company is already inactive.");
        }

        this.isActive = false;
    }


    public void reopenCompany(long requestingMemberId) throws Exception {
        if (!isFounder(requestingMemberId)) { // if not founder - throw exception
            throw new Exception("The system rejects the request due to lack of permissions. Only the Founder can reopen the company.");
        }

        if (this.isActive) { // if the company is already active
            throw new Exception("The company is already Active. No action needed.");
        }

        this.isActive = true;
    }

    public String getRolesTreeRepresentation(long requestingMemberId, Map<Long, String> userPermissions) throws Exception {
        if (!isOwner(requestingMemberId)) { 
            throw new Exception("The system rejects the request due to lack of permissions. Only Owners can view the roles tree.");
        }
        
        return this.rolesTree.getStructuredData(userPermissions);
    }

    public void registerNewAppointment(long appointerId, long appointeeId, String role) { 
        rolesTree.addAppointment(appointerId, appointeeId, role);

        if ("OWNER".equalsIgnoreCase(role)) {
            addOwner(appointeeId);
        } else if ("MANAGER".equalsIgnoreCase(role)) {
            addManager(appointeeId);
        }
    }

    public void removeUserFromAllRoles(long memberId) throws Exception {
        this.owners.remove(Long.valueOf(memberId));
        this.managers.remove(Long.valueOf(memberId));
        this.rolesTree.removeNode(memberId);
    }

    public List<Long> getOwnersAndManagersIds() {
    List<Long> members = new ArrayList<>();
    members.addAll(owners);

    for (Long managerId : managers) {
        if (!members.contains(managerId)) {
            members.add(managerId);
        }
    }

    return members;
}

public void closeBySystemAdmin(long adminId) throws Exception {
    if (!this.isActive) {
        throw new Exception("Company is already inactive.");
    }

    this.isActive = false;

    this.owners.clear();
    this.managers.clear();
    this.rolesTree.clearAllAppointments();
}
}