package ticketsystem.DomainLayer.user;
import java.util.HashMap;
import java.util.Map;

public class Member extends User {
    private final long id;
    private String userName;
    private String password;
    private Map<Long, CompanyRole> companyRoles;

    public Member(long id, String userName, String password) {
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.companyRoles = new HashMap<>();

    }
    public long getId() {
        return id;
    }
    public String getUserName() {
        return userName;
    }
    protected void setUserName(String userName) {
        this.userName = userName;
    }

}
