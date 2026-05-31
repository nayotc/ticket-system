package ticketsystem.ApplicationLayer;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.stereotype.Service;

import ticketsystem.ApplicationLayer.Events.UserLoginListener;
import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.DomainLayer.user.Member;

@Service
public class UserService {
    
    private final IUserRepository userRepository;
    private final ITokenService tokenService;
    private final IPasswordService passwordService;
    private final ISystemLogger logger;
    private final List<UserLoginListener> userLoginListeners;

    public UserService(IUserRepository userRepository, ITokenService tokenService, ISystemLogger logger) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordService = new PasswordService();
        this.logger = logger;
        this.userLoginListeners = new ArrayList<>();
    }


    // 1. System Visit: Allows a guest to visit the system and receive a session
    // token.
    public String visitSystem() {
        logger.logEvent("VisitSystem started: creating guest session", LogLevel.INFO);

        try {
            Guest guest = new Guest();
            logger.logEvent("VisitSystem: guest object created", LogLevel.DEBUG);

            String guestToken = tokenService.addActiveSession(guest);
            logger.logEvent("VisitSystem succeeded: guest session created, guestToken=" + maskToken(guestToken), LogLevel.INFO);

            return guestToken;
        } catch (Exception e) {
            logger.logError("Failed to create guest session", e);
            throw e;
        }
    }

    // 2. Sign Up: Allows a guest to sign up as a member by providing a uniqe
    // username and password.
    public boolean signUp(String sessionToken, String username, String password, String fullName, String phone) {
        try {
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                logger.logEvent("Sign-up rejected: blank username or password", LogLevel.WARN);
                throw new IllegalArgumentException("Username and password are required.");
            }
            String normalizedFullName = validateAndNormalizeFullName(fullName);
            String normalizedPhone = validateAndNormalizePhone(phone);
            tokenService.validateToken(sessionToken);

            if (!tokenService.isGuestToken(sessionToken)) {
                logger.logEvent("Sign-up rejected: session is not a guest token", LogLevel.WARN);
                throw new IllegalStateException("Only guests can sign up.");
            }

            if (userRepository.isUsernameTaken(username)) {
                logger.logEvent("Sign-up rejected: username already taken, username=" + username, LogLevel.INFO);
                throw new IllegalArgumentException("Username is already taken.");
            }

            Long newId = new SecureRandom().nextLong();
            while (userRepository.isIDTaken(newId)) {
                newId = new SecureRandom().nextLong();
            }

            String hashedPassword = passwordService.hashPassword(password);
            userRepository.addRegisteredMember(newId, new Member(newId, username, normalizedFullName, normalizedPhone), hashedPassword);
            logger.logEvent("Sign-up succeeded: new member registered, username=" + username, LogLevel.INFO);
            return true;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;

        } catch (Exception e) {
            logger.logError("Sign-up failed with unexpected error", e);
            throw e;
        }
    }

    // 3. Login: Allows a guest to log in as a member by providing their username
// and password, and receive a new session token.
    public String login(String sessionToken, String username, String password) {
        logger.logEvent(
                "Login started: username=" + username + ", guestToken=" + maskToken(sessionToken),
                LogLevel.INFO);

        try {
            tokenService.validateToken(sessionToken);
            logger.logEvent("Login validation passed: token=" + maskToken(sessionToken), LogLevel.DEBUG);

            if (!tokenService.isGuestToken(sessionToken)) {
                logger.logEvent("Login rejected: session is not a guest token", LogLevel.WARN);
                throw new IllegalStateException("Only guests can log in.");
            }
            logger.logEvent("Login session type confirmed: guest token", LogLevel.DEBUG);

            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                logger.logEvent("Login rejected: blank username or password", LogLevel.WARN);
                throw new IllegalArgumentException("Username and password are required.");
            }
            logger.logEvent("Login input validation passed: username=" + username, LogLevel.DEBUG);

            String hashedPassword = userRepository.getHashedPasswordByUsername(username);
            logger.logEvent("Login password hash lookup completed: username=" + username + ", found=" + (hashedPassword != null), LogLevel.DEBUG);

            if (hashedPassword == null || !passwordService.verifyPassword(password, hashedPassword)) {
                // Single message avoids distinguishing unknown user vs wrong password (user enumeration).
                logger.logEvent("Login rejected: invalid credentials, username=" + username, LogLevel.WARN);
                throw new IllegalArgumentException("Invalid username or password.");
            }
            logger.logEvent("Login credentials verified: username=" + username, LogLevel.DEBUG);

            Member member = userRepository.getMemberByUsername(username);
            if (member == null) {
                logger.logEvent(
                        "Login rejected: member missing after successful password check, username=" + username,
                        LogLevel.WARN);
                throw new IllegalStateException("Login failed. Please try again.");
            }
            logger.logEvent("Login member loaded: username=" + username + ", memberId=" + member.getId(), LogLevel.DEBUG);

            String memberToken = tokenService.addActiveSession(member);
            logger.logEvent(
                    "Login member session created: username=" + username
                            + ", memberId=" + member.getId()
                            + ", memberToken=" + maskToken(memberToken),
                    LogLevel.INFO);

            try {
                logger.logEvent(
                        "Login post-processing started: notifying listeners, oldGuestToken=" + maskToken(sessionToken)
                                + ", newMemberToken=" + maskToken(memberToken),
                        LogLevel.DEBUG);

                notifyListeners(sessionToken, memberToken);
                logger.logEvent("Login listeners completed: username=" + username + ", memberId=" + member.getId(), LogLevel.DEBUG);

                tokenService.removeActiveSession(sessionToken);
                logger.logEvent("Login guest session removed: oldGuestToken=" + maskToken(sessionToken), LogLevel.DEBUG);

                logger.logEvent("Login succeeded: username=" + username, LogLevel.INFO);
                return memberToken;
            } catch (Exception e) {
                tokenService.removeActiveSession(memberToken);
                logger.logError(
                        "Login aborted: post-login listener failed; member session rolled back, username=" + username,
                        e);
                throw new IllegalStateException("Login failed. Please try again.");
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent("Login failed: username=" + username + ", reason=" + e.getMessage(), LogLevel.WARN);
            throw e;

        } catch (Exception e) {
            logger.logError("Login failed with unexpected error", e);
            throw new RuntimeException("Login failed. Please try again.", e);
        }
    }

    // 4. Exit: Allows a user to exit the system entirely.
    public boolean exit(String sessionToken) {
        logger.logEvent("Exit started: token=" + maskToken(sessionToken), LogLevel.INFO);

        try {
            tokenService.validateToken(sessionToken);
            logger.logEvent("Exit validation passed: token=" + maskToken(sessionToken), LogLevel.DEBUG);

            Long memberId = null;

            if (tokenService.isMemberToken(sessionToken)) {
                memberId = tokenService.extractUserId(sessionToken);
                logger.logEvent("Exit member identified: memberId=" + memberId, LogLevel.DEBUG);
            }

            tokenService.removeActiveSession(sessionToken);
            logger.logEvent("Exit session removed: token=" + maskToken(sessionToken) + ", memberId=" + memberId, LogLevel.DEBUG);

            logger.logEvent("Exit: session closed", LogLevel.INFO);
            return true;

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent("Exit failed: reason=" + e.getMessage() + ", token=" + maskToken(sessionToken), LogLevel.WARN);
            throw e;

        } catch (Exception e) {
            logger.logError("Exit failed", e);
            throw e;
        }
    }

    // 5. Log Out: Allows a member to log out and receive a new guest session token.
    public String logOut(String sessionToken) {
        logger.logEvent("Logout started: memberToken=" + maskToken(sessionToken), LogLevel.INFO);

        try {
            tokenService.validateToken(sessionToken);
            logger.logEvent("Logout validation passed: token=" + maskToken(sessionToken), LogLevel.DEBUG);

            if (tokenService.isGuestToken(sessionToken)) {
                logger.logEvent(
                        "Logout rejected: guest session cannot use member logout (use exit to end session)",
                        LogLevel.WARN);
                throw new IllegalStateException("Only logged-in members can log out.");
            }

            Long memberId = tokenService.extractUserId(sessionToken);
            logger.logEvent("Logout member identified: memberId=" + memberId, LogLevel.DEBUG);

            tokenService.removeActiveSession(sessionToken);
            logger.logEvent("Logout member session removed: memberId=" + memberId + ", oldMemberToken=" + maskToken(sessionToken), LogLevel.DEBUG);

            String guestToken = visitSystem();
            logger.logEvent("Logout guest session created: memberId=" + memberId + ", newGuestToken=" + maskToken(guestToken), LogLevel.DEBUG);

            logger.logEvent("Logout succeeded: new guest session issued, memberId=" + memberId, LogLevel.INFO);
            return guestToken;

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent("Logout failed: reason=" + e.getMessage() + ", token=" + maskToken(sessionToken), LogLevel.WARN);
            throw e;

        } catch (Exception e) {
            logger.logError("Logout failed", e);
            throw e;
        }
    }

    // 6. Update Member Username: Allows a member to update their username by
    // providing their current username, password, and new username.
    public boolean updateMemberUsername(String sessionToken, String password, String username, String newUsername) {
        try {
            if (username == null || username.isBlank()) {
                logger.logEvent("Update username rejected: current username is blank", LogLevel.WARN);
                throw new IllegalArgumentException("Current username cannot be blank.");
            }

            if (newUsername == null || newUsername.isBlank()) {
                logger.logEvent("Update username rejected: new username is blank", LogLevel.WARN);
                throw new IllegalArgumentException("New username cannot be blank.");
            }

            if (authenticateMemberForUpdate(sessionToken, password, username) == null) {
                logger.logEvent(
                        "Update username rejected: authentication failed, username=" + username,
                        LogLevel.WARN);
                throw new IllegalArgumentException("Invalid username or password.");
            }

            if (!username.equals(newUsername) && userRepository.isUsernameTaken(newUsername)) {
                logger.logEvent(
                        "Update username rejected: new username already taken, newUsername=" + newUsername,
                        LogLevel.WARN);
                throw new IllegalArgumentException("Username is already taken.");
            }

            boolean ok = userRepository.updateRegisteredMemberUsername(username, newUsername);
            if (!ok) {
                logger.logEvent(
                        "Update username rejected: repository update failed, username=" + username,
                        LogLevel.WARN);
                throw new IllegalStateException("Username update failed. Please try again.");
            }

            logger.logEvent(
                    "Member username updated: oldUsername=" + username + ", newUsername=" + newUsername,
                    LogLevel.INFO);
            return true;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;

        } catch (Exception e) {
            logger.logError("Update member username failed", e);
            throw e;
        }
    }

    // 7. Update Member Password: Allows a member to update their password by
    // providing their current username, password, and new password.
    public boolean updateMemberPassword(String sessionToken, String password, String username, String newPassword) {
        try {
            if (username == null || username.isBlank()) {
                logger.logEvent("Update password rejected: username is blank", LogLevel.WARN);
                throw new IllegalArgumentException("Username cannot be blank.");
            }

            if (newPassword == null || newPassword.isBlank()) {
                logger.logEvent("Update password rejected: new password is blank", LogLevel.WARN);
                throw new IllegalArgumentException("New password cannot be blank.");
            }

            if (authenticateMemberForUpdate(sessionToken, password, username) == null) {
                logger.logEvent(
                        "Update password rejected: authentication failed, username=" + username,
                        LogLevel.WARN);
                throw new IllegalArgumentException("Invalid username or password.");
            }

            String newHashedPassword = passwordService.hashPassword(newPassword);
            boolean ok = userRepository.updateRegisteredMemberPassword(username, newHashedPassword);
            if (!ok) {
                logger.logEvent(
                        "Update password rejected: repository update failed, username=" + username,
                        LogLevel.WARN);
                throw new IllegalStateException("Password update failed. Please try again.");
            }

            logger.logEvent("Member password updated: username=" + username, LogLevel.INFO);
            return true;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;

        } catch (Exception e) {
            logger.logError("Update member password failed", e);
            throw e;
        }
    }

    private Member authenticateMemberForUpdate(String sessionToken, String password, String username) {
        if (password == null || password.isBlank() || username == null || username.isBlank()) {
            return null;
        }
        tokenService.validateToken(sessionToken);
        if (!tokenService.isMemberToken(sessionToken)) {
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

    private void notifyListeners(String guestToken, String memberToken) {
        logger.logEvent(
                "Login listeners notification started: listeners=" + userLoginListeners.size()
                        + ", guestToken=" + maskToken(guestToken)
                        + ", memberToken=" + maskToken(memberToken),
                LogLevel.DEBUG);

        for (UserLoginListener listener : userLoginListeners) {
            logger.logEvent("Login listener notification: listener=" + listener.getClass().getSimpleName(), LogLevel.DEBUG);
            listener.onUserLogin(guestToken, memberToken);
        }

        logger.logEvent("Login listeners notification finished: listeners=" + userLoginListeners.size(), LogLevel.DEBUG);
    }

    public void addUserLoginListener(UserLoginListener listener) {
        userLoginListeners.add(listener);
        logger.logEvent(
                "UserLoginListener added: listener=" + (listener == null ? "null" : listener.getClass().getSimpleName())
                        + ", totalListeners=" + userLoginListeners.size(),
                LogLevel.DEBUG);
    }

    public void removeUserLoginListener(UserLoginListener listener) {
        userLoginListeners.remove(listener);
        logger.logEvent(
                "UserLoginListener removed: listener=" + (listener == null ? "null" : listener.getClass().getSimpleName())
                        + ", totalListeners=" + userLoginListeners.size(),
                LogLevel.DEBUG);
    }

    private String validateAndNormalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            logger.logEvent("Sign-up rejected: blank phone", LogLevel.WARN);
            throw new IllegalArgumentException("Phone number is required.");
        }

        String normalizedPhone = phone.replaceAll("[\\s-]", "");

        if (!normalizedPhone.matches("\\d+")) {
            logger.logEvent("Sign-up rejected: invalid phone characters", LogLevel.WARN);
            throw new IllegalArgumentException("Phone number must contain digits only.");
        }

        if (normalizedPhone.length() < 9 || normalizedPhone.length() > 10) {
            logger.logEvent("Sign-up rejected: invalid phone length", LogLevel.WARN);
            throw new IllegalArgumentException("Phone number must be 9 or 10 digits long.");
        }

        return normalizedPhone;
    }

    private String validateAndNormalizeFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            logger.logEvent("Sign-up rejected: blank full name", LogLevel.WARN);
            throw new IllegalArgumentException("Full name is required.");
        }

        String normalizedFullName = fullName.trim();

        if (normalizedFullName.length() < 2 || normalizedFullName.length() > 100) {
            logger.logEvent("Sign-up rejected: invalid full name length", LogLevel.WARN);
            throw new IllegalArgumentException("Full name must be between 2 and 100 characters.");
        }

        return normalizedFullName;
    }

    private String maskToken(String token) {
        if (token == null) {
            return "null";
        }

        if (token.length() <= 12) {
            return "***";
        }

        return token.substring(0, 6) + "..." + token.substring(token.length() - 6);
    }

    /**
     * Retrieves all registered members in the system.
     * Used primarily by the System Admin Dashboard.
     */
    public List<Member> getAllUsers() {
        try {
            return userRepository.getAllMembers();
        } catch (Exception e) {
            logger.logError("Failed to retrieve all users", e);
            throw new RuntimeException("Failed to retrieve all users", e);
        }
    }

}
