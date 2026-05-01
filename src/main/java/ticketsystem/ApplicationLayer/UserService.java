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
    // 1. System Visit: Allows a guest to visit the system and receive a session token.
    public String visitSystem() {
        Guest guest = new Guest();
        return tokenService.addActiveSession(guest);
    }
    // 2. Sign Up: Allows a guest to sign up as a member by providing a uniqe username and password.
    public void signUp(String sessionToken, String username, String password) {
        if (!tokenService.validateToken(sessionToken)) {
            return;
        }
        if (!tokenService.isGuestToken(sessionToken)) {
            return;
        }
        if (userRepository.isUsernameTaken(username)) {
            return;
        }
        Long newId = new SecureRandom().nextLong();
        while (userRepository.isIDTaken(newId)) {
            newId = new SecureRandom().nextLong();
        }

        userRepository.addRegisteredMember(newId, new Member(newId, username), password);
    }

    public void logIn(String sessionToken, String username, String password) {

    }

    public void exit(String sessionToken) {

    }

}
