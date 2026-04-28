package ticketsystem.ApplicationLayer;

import java.security.SecureRandom;

import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;

public class UserService {
    
    private final IUserRepository userRepository;
    private final ITokenService tokenService;
    private final IPasswordService passwordService;
    private final ISystemLogger logger;

    public UserService(IUserRepository userRepository, ITokenService tokenService, ISystemLogger logger) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordService = new PasswordService();
        this.logger = logger;
    }


    // 1. System Visit: Allows a guest to visit the system and receive a session
    // token.
    public String visitSystem() {
        try {
            Guest guest = new Guest();
            return tokenService.addActiveSession(guest);
        } catch (Exception e) {
            logger.logEvent("Error adding active session: " + e.getMessage(), LogLevel.INFO);
            throw e;
        }
    }

    // 2. Sign Up: Allows a guest to sign up as a member by providing a uniqe
    // username and password.
    public boolean signUp(String sessionToken, String username, String password) {
        try {
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                return false;
            }
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
        } catch (Exception e) {
            logger.logEvent("Error signing up: " + e.getMessage(), LogLevel.INFO);
            throw e;
        }
    }

    // 3. Login: Allows a guest to log in as a member by providing their username
    // and password, and receive a new session token.
    public String login(String sessionToken, String username, String password) {
        try {
            if (!tokenService.validateToken(sessionToken) || !tokenService.isGuestToken(sessionToken)) { // Only guests
                                                                                                         // can
                                                                                                         // log in, if
                                                                                                         // the
                                                                                                         // token is not
                                                                                                         // a
                                                                                                         // guest token,
                                                                                                         // it
                                                                                                         // means the
                                                                                                         // user
                                                                                                         // is already
                                                                                                         // logged in as
                                                                                                         // a
                                                                                                         // member
                return null;
            }
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                return null;
            }
            String hashedPassword = userRepository.getHashedPasswordByUsername(username); // Get the hashed password for
                                                                                          // the
                                                                                          // given username from the
                                                                                          // repository, null if the
                                                                                          // username does not exist
            if (hashedPassword == null) {
                return null;
            }
            if (!passwordService.verifyPassword(password, hashedPassword)) {
                return null;
            }
            Member member = userRepository.getMemberByUsername(username);
            // TODO: implement add Guest's active order to Member's active order if exists
            tokenService.removeActiveSession(sessionToken); // Remove the guest session token since the user is now
                                                            // logged
                                                            // in as a member, and we will create a new session token
                                                            // for
                                                            // the member
            return tokenService.addActiveSession(member);
        } catch (Exception e) {
            logger.logEvent("Error logging in: " + e.getMessage(), LogLevel.INFO);
            throw e;
        }
    }

    // 4. Exit: Allows a user to exit the system entirely.
    public boolean exit(String sessionToken) {
        try {
            if (!tokenService.validateToken(sessionToken)) {
                return false;
            }
            if (!tokenService.isActiveSession(sessionToken)) {
                return false;
            }
            tokenService.removeActiveSession(sessionToken);
            return true;
        } catch (Exception e) {
            logger.logEvent("Error exiting system: " + e.getMessage(), LogLevel.INFO);
            throw e;
        }
    }

    // 5. Log Out: Allows a member to log out and receive a new guest session token.
    public String logOut(String sessionToken) {
        try {
            if (!tokenService.validateToken(sessionToken)) {
                return null;
            }
            if (!tokenService.isActiveSession(sessionToken)) {
                return null;
            }
            if (tokenService.isGuestToken(sessionToken)) {
                return null;
            }
            tokenService.removeActiveSession(sessionToken);
            return visitSystem();
        } catch (Exception e) {
            logger.logEvent("Error exiting system: " + e.getMessage(), LogLevel.INFO);
            throw e;
        }

    }

    // 6. Update Member Username: Allows a member to update their username by
    // providing their current username, password, and new username.
    public boolean updateMemberUsername(String sessionToken, String password, String username, String newUsername) {
        try {
            if (newUsername == null || newUsername.isBlank()) {
                return false;
            }
            if (authenticateMemberForUpdate(sessionToken, password, username) == null) {
                return false;
            }
            return userRepository.updateRegisteredMemberUsername(username, newUsername);
        } catch (Exception e) {
            logger.logEvent("Error updating member username: " + e.getMessage(), LogLevel.INFO);
            throw e;
        }
    }

    // 7. Update Member Password: Allows a member to update their password by
    // providing their current username, password, and new password.
    public boolean updateMemberPassword(String sessionToken, String password, String username, String newPassword) {
        try {
            if (newPassword == null || newPassword.isBlank()) {
                return false;
            }
            if (authenticateMemberForUpdate(sessionToken, password, username) == null) {
                return false;
            }
            String newHashedPassword = passwordService.hashPassword(newPassword);
            return userRepository.updateRegisteredMemberPassword(username, newHashedPassword);
        } catch (Exception e) {
            logger.logEvent("Error updating member password: " + e.getMessage(), LogLevel.INFO);
            throw e;
        }
    }

    private Member authenticateMemberForUpdate(String sessionToken, String password, String username) {
        if (password == null || password.isBlank() || username == null || username.isBlank()) {
            return null;
        }
        if (!tokenService.validateToken(sessionToken) || !tokenService.isMemberToken(sessionToken)) {
            return null;
        }
        Member member = userRepository.getMemberByUsername(username);
        if (member == null) {
            return null;
        }
        if (!tokenService.extractUserId(sessionToken).equals(member.getId())) {
            return null;
        }
        String hashedPassword = userRepository.getHashedPasswordByUsername(username);
        if (hashedPassword == null) {
            return null;
        }
        if (!passwordService.verifyPassword(password, hashedPassword)) {
            return null;
        }
        return member;
    }

}
