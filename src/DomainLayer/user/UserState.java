package DomainLayer.user;

public abstract class UserState {
    protected User user;
    
    public UserState(User user){
        this.user = user;
    }
}
