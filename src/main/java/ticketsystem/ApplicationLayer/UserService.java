package ticketsystem.ApplicationLayer;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.Guest;

public class UserService {
    private final IUserRepository userRepository;
    private final TokenService tokenService;

    public UserService(IUserRepository userRepository){
        this.userRepository = userRepository;
        this.tokenService = new TokenService();
    }
    public String visitSystem(){
        Guest guest = new Guest();
        String sessionToken = tokenService.generateNewGuestToken();
        while(!userRepository.addGuest(sessionToken, guest)){
            sessionToken = tokenService.generateNewGuestToken();
        }
        return sessionToken;
    }

    public void signUp(String sessionToken, String username, String password){
        
    }

    public void logIn(String sessionToken, String username, String password){

    }
    
    public void exit(String sessionToken){

    }

}

