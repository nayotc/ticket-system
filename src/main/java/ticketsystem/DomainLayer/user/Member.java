package ticketsystem.DomainLayer.user;
import java.util.HashMap;
import java.util.Map;

public class Member extends User {
    private final long id;
    private String userName;
    private Map<Long, CompanyRole> companyRoles;

    public Member(long id, String userName) {
        this.id = id;
        this.userName = userName;
        this.companyRoles = new HashMap<>();

    }
    public long getId() {
        return id;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

}
