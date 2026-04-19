package ApplicationLayer;

public class UserService {
    private final IUserRepository userRepository;

    public UserService(IUserRepository userRepository){
        this.userRepository = userRepository;
    }

    public void signUp(SessionToken sessionToken, String username, String password){

    }

    public void logIn(SessionToken sessionToken, String username, String password){

    }
    
    public void exit(SessionToken sessionToken){

    }

}

