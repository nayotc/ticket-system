package ticketsystem.DomainLayer.user;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Member extends User {

    private final Long memberId;
    private String userName;
    private String fullName;
    private String phone;
    private long version;
    private ConcurrentHashMap<Long, CompanyRole> myRoles; // Key: companyId, Value: Role in that company
    private Suspension suspension;


    public Member(Long memberId, String userName, String fullName, String phone) {
        this.memberId = memberId;
        this.userName = userName;
        this.fullName = fullName;
        this.phone = phone;
        this.version = 0; // Initialize version
        this.myRoles = new ConcurrentHashMap<Long, CompanyRole>();
    }

    // Copy Constructor for Deep Copying
    public Member(Member other) {
        this.memberId = other.memberId;
        this.userName = other.userName;
        this.fullName = other.fullName;
        this.phone = other.phone;
        this.version = other.version;
        this.myRoles = new ConcurrentHashMap<>();//deep copy???
        this.suspension = other.suspension == null
        ? null
        : new Suspension(other.suspension);
        
        // Deep copy of the roles map to prevent shared memory references between threads
        for (java.util.Map.Entry<Long, CompanyRole> entry : other.myRoles.entrySet()) {
            Long compId = entry.getKey();
            CompanyRole originalRole = entry.getValue();
            CompanyRole copiedRole = null;
            
            // Polymorphic copying based on the role instance
            if (originalRole instanceof Founder) {
                copiedRole = new Founder((Founder) originalRole, compId);
            } else if (originalRole instanceof Owner) {
                copiedRole = new Owner((Owner) originalRole, compId);
            } else if (originalRole instanceof Manager) {
                copiedRole = new Manager((Manager) originalRole, compId);
            }
            
            if (copiedRole != null) {
                this.myRoles.put(compId, copiedRole);
            }
        }
    }

    public Long getId() {
        return this.memberId;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getFullName() { return this.fullName;}

    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return this.phone; }

    public void setPhone(String phone) { this.phone = phone; }

    public long getVersion() {
        return this.version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public List<CompanyRole> getAllRoles() {
        return myRoles.values().stream().collect(Collectors.toList());
    }

    public CompanyRole getRoleInCompany(Long companyId) {
        return myRoles.get(companyId);
    }

    public boolean hasPermission(Long companyId, Permission permission) {
        CompanyRole role = myRoles.get(companyId);
        return role != null && role.hasPermission(permission);
    }

    public boolean addManagerRole(Long companyId, Long memberId, Set<Permission> permissions) {
        CompanyRole newRole = new Manager(companyId, memberId, permissions);
        return myRoles.putIfAbsent(companyId, newRole) == null;
    }

    public boolean addOwnerRole(Long companyId, Long memberId) {
        CompanyRole newRole = new Owner(companyId, memberId);
        return myRoles.putIfAbsent(companyId, newRole) == null;
    }

    public boolean addFounderRole(Long companyId) {
        CompanyRole newRole = new Founder(companyId);
        return myRoles.putIfAbsent(companyId, newRole) == null;
    }

    public boolean deleteRoleInCompany(Long companyId) {
        return myRoles.remove(companyId) != null;
    }

    public void updateManagerPermissions(Long companyId, Set<Permission> newPermissions) {
        CompanyRole role = myRoles.get(companyId);
        if (role != null && role instanceof Manager) {
            ((Manager) role).setPermissions(newPermissions);
        }
    }

    //Suspend
     public void suspendMember(Long suspendedByAdminId,
                      LocalDateTime startDate,
                      LocalDateTime endDate,
                      String reason){

        if(isSuspended()){
            throw new IllegalStateException("Member is already suspended");
        }
        //validation in the constructor
        Suspension suspension=new Suspension(suspendedByAdminId, startDate ,endDate, reason);
      
        this.suspension=suspension;
    }

    public void revokeSuspension(){

        Suspension activeSuspension = getSuspension();
        if(activeSuspension == null|| !suspension.isActive()){
            throw new IllegalStateException("Member is not suspended");
        }

        activeSuspension.revoke();
    }

    public boolean isSuspended(){
        if(suspension==null)
            return false;
        return suspension.isActive();
    }

    public Suspension getSuspension(){
            return suspension;
    }
}