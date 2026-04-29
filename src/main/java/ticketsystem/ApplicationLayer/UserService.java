package ticketsystem.ApplicationLayer;

import java.security.SecureRandom;

import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.DomainLayer.user.Member;

public class UserService {
    private final IUserRepository userRepository;
    private final ITokenService tokenService;
    private final IPasswordService passwordService;

    public UserService(IUserRepository userRepository, ITokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordService = new PasswordService();
    }
    // 1. System Visit: Allows a guest to visit the system and receive a session token.
    public String visitSystem() {
        Guest guest = new Guest();
        return tokenService.addActiveSession(guest);
    }
    // 2. Sign Up: Allows a guest to sign up as a member by providing a uniqe username and password.
    public boolean signUp(String sessionToken, String username, String password) {
        if (!tokenService.validateToken(sessionToken)) {
            return false;
        }
        if (!tokenService.isGuestToken(sessionToken)) {
            return false;
        }
        if (userRepository.isUsernameTaken(username)) {
            return false;
        }
        Long newId = new SecureRandom().nextLong();
        while (userRepository.isIDTaken(newId)) {
            newId = new SecureRandom().nextLong();
        }
        String hashedPassword = passwordService.hashPassword(password);
        userRepository.addRegisteredMember(newId, new Member(newId, username), hashedPassword);
        return true;
    }

    public String logIn(String sessionToken, String username, String password) {
        if (!tokenService.validateToken(sessionToken)) {
            System.out.println("Invalid session token");
            return null;
        }
        if (!userRepository.isUserDetailsCorrect(username, password)) {
            System.out.println("User Details are incorrect");
            return null;
        }
        Member member = userRepository.getMemberByUsername(username);
        return tokenService.addActiveSession(member);
    }

    public void exit(String sessionToken) {

    }

}
