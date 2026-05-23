package ticketsystem.ApplicationLayer;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import ticketsystem.ApplicationLayer.Events.UserLoginListener;
import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.DomainLayer.user.Member;

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
        try {
            Guest guest = new Guest();
            return tokenService.addActiveSession(guest);
        } catch (Exception e) {
            logger.logError("Failed to create guest session", e);
            throw e;
        }
    }

    // 2. Sign Up: Allows a guest to sign up as a member by providing a uniqe
    // username and password.
    public boolean signUp(String sessionToken, String username, String password) {
        try {
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                logger.logEvent("Sign-up rejected: blank username or password", LogLevel.WARN);
                throw new IllegalArgumentException("Username and password are required.");
            }

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
            userRepository.addRegisteredMember(newId, new Member(newId, username), hashedPassword);

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
        try {
            tokenService.validateToken(sessionToken);

            if (!tokenService.isGuestToken(sessionToken)) {
                logger.logEvent("Login rejected: session is not a guest token", LogLevel.WARN);
                throw new IllegalStateException("Only guests can log in.");
            }

            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                logger.logEvent("Login rejected: blank username or password", LogLevel.WARN);
                throw new IllegalArgumentException("Username and password are required.");
            }

            String hashedPassword = userRepository.getHashedPasswordByUsername(username);
            if (hashedPassword == null || !passwordService.verifyPassword(password, hashedPassword)) {
                // Single message avoids distinguishing unknown user vs wrong password (user enumeration).
                logger.logEvent("Login rejected: invalid credentials, username=" + username, LogLevel.WARN);
                throw new IllegalArgumentException("Invalid username or password.");
            }

            Member member = userRepository.getMemberByUsername(username);
            if (member == null) {
                logger.logEvent(
                        "Login rejected: member missing after successful password check, username=" + username,
                        LogLevel.WARN);
                throw new IllegalStateException("Login failed. Please try again.");
            }

            String memberToken = tokenService.addActiveSession(member);

            try {
                notifyListeners(sessionToken, memberToken);
                tokenService.removeActiveSession(sessionToken);
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
            throw e;

        } catch (Exception e) {
            logger.logError("Login failed with unexpected error", e);
            throw e;
        }
    }

    // 4. Exit: Allows a user to exit the system entirely.
    public boolean exit(String sessionToken) {
        try {
            tokenService.validateToken(sessionToken);
            tokenService.removeActiveSession(sessionToken);
            logger.logEvent("Exit: session closed", LogLevel.INFO);
            return true;
        } catch (Exception e) {
            logger.logError("Exit failed", e);
            throw e;
        }
    }

    // 5. Log Out: Allows a member to log out and receive a new guest session token.
    public String logOut(String sessionToken) {
        try {
            tokenService.validateToken(sessionToken);
            if (tokenService.isGuestToken(sessionToken)) {
                logger.logEvent(
                        "Logout rejected: guest session cannot use member logout (use exit to end session)",
                        LogLevel.WARN);
                return null;
            }
            Long memberId = tokenService.extractUserId(sessionToken);
            tokenService.removeActiveSession(sessionToken);
            String guestToken = visitSystem();
            logger.logEvent("Logout succeeded: new guest session issued, memberId=" + memberId, LogLevel.INFO);
            return guestToken;
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
        for (UserLoginListener listener : userLoginListeners) {
            listener.onUserLogin(guestToken, memberToken);
        }
    }

    public void addUserLoginListener(UserLoginListener listener) {
        userLoginListeners.add(listener);
    }

    public void removeUserLoginListener(UserLoginListener listener) {
        userLoginListeners.remove(listener);
    }

}
