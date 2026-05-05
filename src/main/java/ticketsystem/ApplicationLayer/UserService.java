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
    // 3. Login: Allows a guest to log in as a member by providing their username and password, and receive a new session token.
    public String login(String sessionToken, String username, String password) {
        if (!tokenService.isGuestToken(sessionToken)) { // Only guests can log in, if the token is not a guest token, it means the user is already logged in as a member
            return null;
        }
        if (!tokenService.validateToken(sessionToken)) { 
            return null;
        }
        String hashedPassword = userRepository.getHashedPasswordByUsername(username); // Get the hashed password for the given username from the repository, null if the username does not exist
        if (hashedPassword == null) {
            return null;
        }
        if (!passwordService.verifyPassword(password, hashedPassword)) {
            System.out.println("User Details are incorrect");
            return null;
        }
        Member member = userRepository.getMemberByUsername(username);
        //TODO: implement add Guest's active order to Member's active order if exists
        tokenService.removeActiveSession(sessionToken); // Remove the guest session token since the user is now logged in as a member, and we will create a new session token for the member
        return tokenService.addActiveSession(member);
    }

    public void exit(String sessionToken) {
        
    }

}
