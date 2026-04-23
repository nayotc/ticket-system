package ticketsystem.DomainLayer.user;
import java.util.HashMap;
import java.util.Map;

public class Member extends UserState {
    private final long id;
    private String userName;
    private String password;
    private Map<Long, CompanyRole> companyRoles;

    public Member(User user, long id, String userName, String password) {
        super(user);
        this.id = id;
        this.userName = userName;
        this.password = password;
        this.companyRoles = new HashMap<>();

    }

}
