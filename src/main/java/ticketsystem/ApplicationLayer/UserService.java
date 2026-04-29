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
        return tokenService.addActiveSession(guest);
    }

    public void signUp(String sessionToken, String username, String password) {
        if (!tokenService.validateToken(sessionToken)) {
            System.out.println("Invalid session token");
            return;
        }
        if (userRepository.isUsernameTaken(username)) {
            System.out.println("Username is already taken");
            return;
        }
        Long newId = new SecureRandom().nextLong();
        while (userRepository.isIDTaken(newId)) {
            newId = new SecureRandom().nextLong();
        }

        userRepository.addRegisteredMember(newId, new Member(newId, username), password);
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
