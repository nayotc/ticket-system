package ticketsystem.DomainLayer.user;

public class User {
    
    private long sessionToken;
    private UserState userState;

    public User(long sessionToken) {
        this.sessionToken = sessionToken;
        this.userState = new Guest(this);
    }

    public long getSessionToken() {
        return sessionToken;
    }

    public UserState getUserState() {
        return userState;
    }

}