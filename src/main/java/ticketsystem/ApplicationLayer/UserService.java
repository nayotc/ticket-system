package ticketsystem.ApplicationLayer;

import java.security.SecureRandom;

import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.DomainLayer.user.Member;

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
        while (!userRepository.addActiveSession(sessionToken, guest)) {
            sessionToken = tokenService.generateNewGuestToken();
        }
        return sessionToken;
    }

    public void signUp(String sessionToken, String username, String password) {
        if(!tokenService.validateToken(sessionToken)) {
            System.out.println("Invalid session token");
            return;
        }
        if(userRepository.isUsernameTaken(username)) {
            System.out.println("Username is already taken");
            return;
        }
        Long newId = new SecureRandom().nextLong();
        while (userRepository.isIDTaken(newId)) {
            newId = new SecureRandom().nextLong();
        }
        userRepository.addRegisteredMember(newId, new Member(newId, username, password));
    }

    public void logIn(String sessionToken, String username, String password) {

    }

    public void exit(String sessionToken) {

    }

}
