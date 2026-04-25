package ticketsystem.ApplicationLayer;
import ticketsystem.DomainLayer.IRepository.IUserRepository;

public class UserService {
    private final IUserRepository userRepository;

    public UserService(IUserRepository userRepository){
        this.userRepository = userRepository;
    }
    public String visitSystem(){
        String sessionToken = new TokenService().generateNewGuestToken();
        userRepository.addGuest(sessionToken);
        return sessionToken;
    }

    public void signUp(String sessionToken, String username, String password){
        
    }

    public void logIn(String sessionToken, String username, String password){

    }
    
    public void exit(String sessionToken){

    }

}

