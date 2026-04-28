package ticketsystem.ApplicationLayer;

import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.Guest;

public class UserService {
    private final IUserRepository userRepository;
    private final ITokenService tokenService;

    public UserService(IUserRepository userRepository, ITokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    public String visitSystem() {
        Guest guest = new Guest();
        String sessionToken = tokenService.generateNewGuestToken();
        while (!userRepository.addGuest(sessionToken, guest)) {
            sessionToken = tokenService.generateNewGuestToken();
        }
        return sessionToken;
    }

    public void signUp(String sessionToken, String username, String password) {

    }

    public void logIn(String sessionToken, String username, String password) {

    }

    public void exit(String sessionToken) {

    }

}
