package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.UserRepository;

public class UserServiceTest {
    private UserRepository userRepository;
    private UserService userService;
    private ITokenService tokenService;
    private ITokenRepository tokenRepository;
    private LogbackSystemLogger logger;
    @BeforeEach
    public void setup() {
        logger = new LogbackSystemLogger();
        userRepository = new UserRepository();
        tokenRepository = new TokenRepository();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository);
        userService = new UserService(userRepository, tokenService, logger);
    }

    @Test
    void TestSuccessfulSystemVisit_Acceptance() {
        // Act: invoke the method under test
        String sessionToken = userService.visitSystem();

        // Assert: check that the session token is valid and the guest is added
        assertNotNull(sessionToken, "Session token should not be null");
        assertFalse(sessionToken.isEmpty(), "Session token should not be empty");
        assertFalse(tokenService.isActiveSession("invalid-token"), "Invalid token should not be active");
        assertTrue(tokenService.isActiveSession(sessionToken), "Valid token should be active");

    }

    @Test
    void TestSuccessfulSignUp_Acceptance() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();

        // Act: sign up with a unique username
        boolean answer = userService.signUp(sessionToken, "newUser", "password123");

        // Assert: check that the new member is added to the repository
        assertTrue(answer, "Sign up should succeed with a unique username");
        assertTrue(userRepository.isUsernameTaken("newUser"), "Username should be taken after sign up");
        assertEquals(userRepository.getAllRegisteredMembersCount(), 1,
                "There should be one registered member after sign up");
    }

    @Test
    void TestSignUpWithTakenUsername_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken1 = userService.visitSystem();
        userService.signUp(sessionToken1, "existingUser", "password123");

        // Act: attempt to sign up with the same username
        String sessionToken2 = userService.visitSystem();
        boolean answer = userService.signUp(sessionToken2, "existingUser", "password456");

        // Assert: check that the second sign-up attempt fails due to username being
        // taken
        assertFalse(answer, "Sign up should fail with a taken username");
        assertTrue(userRepository.isUsernameTaken("existingUser"), "Username should be taken");
        assertEquals(userRepository.getAllRegisteredMembersCount(), 1,
                "There should still be only one registered member");
    }

    @Test
    void TestSignUpWithInvalidSessionToken_Acceptance() {
        // arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        String invalidToken = "fake-token";

        // Act & Assert: attempt to sign up with an invalid session token and expect an
        // exception
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(invalidToken, "user", "password");
        }, "Sign up should throw an exception for an invalid session token");

    }

    @Test
    void TestLoginWithRightUsernameAndPassword() {
        // Arange: visit and signup a new Member
        String sessionToken1 = userService.visitSystem();
        userService.signUp(sessionToken1, "newUser", "password123");
        // Act: login with the correct username and password
        String loginToken = userService.login(sessionToken1, "newUser", "password123");
        // Assert: check that the login token is valid and the user is logged in
        assertNotNull(loginToken, "Login token should not be null");
        assertFalse(tokenService.isActiveSession(sessionToken1),
                "Original session token should no longer be active after login");
        assertFalse(loginToken.isEmpty(), "Login token should not be empty");
        assertTrue(tokenService.isActiveSession(loginToken), "Login token should be active");
        assertTrue(tokenService.validateToken(loginToken), "Login token should be valid");
        assertTrue(tokenService.isMemberToken(loginToken), "Login token should be a member token");
    }

    @Test
    void TestLoginWithWrongUsername() {
        // Arange: visit and signup a new Member
        String sessionToken1 = userService.visitSystem();
        userService.signUp(sessionToken1, "newUser", "password123");
        // Act: login with the wrong username
        String loginToken = userService.login(sessionToken1, "wrongUser", "password123");
        // Assert: check that the login attempt fails due to incorrect username
        assertNull(loginToken, "Login token should be null for wrong username");
        assertFalse(tokenService.isActiveSession(loginToken), "Login token should not be active for wrong username");
        assertTrue(tokenService.isActiveSession(sessionToken1),
                "Original session token should still be active after failed login");
    }

    @Test
    void TestLoginWithWrongPassword() {
        // Arange: visit and signup a new Member
        String sessionToken1 = userService.visitSystem();
        userService.signUp(sessionToken1, "newUser", "password123");
        // Act: login with the wrong password
        String loginToken = userService.login(sessionToken1, "newUser", "password1234");
        // Assert: check that the login attempt fails due to incorrect password
        assertNull(loginToken, "Login token should be null for wrong password");
        assertFalse(tokenService.isActiveSession(loginToken), "Login token should not be active for wrong password");
    }

    @Test
    void TestLoginWithWrongToken() {
        // arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(sessionToken, "user", "password");

        String invalidToken = "fake-token";

        // Act & Assert: attempt to sign up with an invalid session token and expect an
        // exception
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login(invalidToken, "user", "password");
        }, "Login should throw an exception for an invalid session token");
    }

    @Test
    void TestLoginWith2Users() {
        // Arange: visit and signup for 2 new Members
        String sessionToken1 = userService.visitSystem();
        userService.signUp(sessionToken1, "newUser1", "password1");
        String sessionToken2 = userService.visitSystem();
        userService.signUp(sessionToken2, "newUser2", "password2");
        // Act: login with the wrong password
        String loginToken = userService.login(sessionToken1, "newUser1", "password2");
        // Assert: check that the login attempt fails due to incorrect password
        assertNull(loginToken, "Login token should be null for wrong password");
    }

    @Test
    void TestUpdateMemberUsername_Successful_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(sessionToken, "userToUpdate", "oldPassword");
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");

        // Act: update username then password with correct current password (same steps as before split API)
        boolean result = userService.updateMemberUsername(loginToken, "oldPassword", "userToUpdate", "updatedUser");

        // Assert: check that the update was successful and the member details were
        // updated
        assertTrue(result, "Updating member username should succeed with correct current password");
    }
    @Test
    void TestUpdateMemberPassword_Successful_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(sessionToken, "userToUpdate", "oldPassword");
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");

        // Act: password change while still registered as userToUpdate
        boolean result = userService.updateMemberPassword(loginToken, "oldPassword", "userToUpdate", "newPassword");

        // Assert: check that the update was successful and the member details were
        // updated
        assertTrue(result, "Updating member password should succeed with correct current password");
    }

    @Test
    void TestUpdateMemberDetails_DifferentToken_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up two users
        String sessionToken1 = userService.visitSystem();
        userService.signUp(sessionToken1, "user1", "password1");
        String loginToken1 = userService.login(sessionToken1, "user1", "password1");

        String sessionToken2 = userService.visitSystem();
        userService.signUp(sessionToken2, "user2", "password2");
        String loginToken2 = userService.login(sessionToken2, "user2", "password2");

        // Act: attempt to update user1's username using user2's token
        boolean result1 = userService.updateMemberUsername(loginToken2, "password1", "user1", "newUser1");
        boolean result2 = userService.updateMemberPassword(loginToken2, "password1", "user1", "newPassword1");
        // Assert: check that the update fails due to the token not belonging to the
        // user being updated
        assertFalse(result1, "Updating member username should fail when the token does not belong to the user");
        assertFalse(result2, "Updating member password should fail when the token does not belong to the user");
    }


    @Test
    void TestUpdateMemberDetails_WrongUsername_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(sessionToken, "userToUpdate", "oldPassword");
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");

        // Act: attempt to update member details with incorrect current username
        boolean result1 = userService.updateMemberUsername(loginToken, "oldPassword", "WrongUserToUpdate", "updatedUser");
        boolean result2 = userService.updateMemberPassword(loginToken, "oldPassword", "WrongUserToUpdate", "newPassword");
        // Assert: check that the update was unsuccessful due to incorrect current
        // username
        assertFalse(result1, "Updating member username should fail with incorrect current username");
        assertFalse(result2, "Updating member password should fail with incorrect current username");
    }

    @Test
    void TestUpdateMemberDetails_WrongPassword_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(sessionToken, "userToUpdate", "oldPassword");
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");

        // Act: attempt to update member details with incorrect current password
        boolean result1 = userService.updateMemberUsername(loginToken, "wrongPassword", "userToUpdate", "updatedUser");
        boolean result2 = userService.updateMemberPassword(loginToken, "wrongPassword", "userToUpdate", "newPassword");
        // Assert: check that the update fails due to incorrect current password
        assertFalse(result1, "Updating member username should fail with incorrect current password");
        assertFalse(result2, "Updating member password should fail with incorrect current password");
    }

    @Test
    void TestUpdateMemberDetails_invalidToken_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(sessionToken, "userToUpdate", "oldPassword");
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");

        // Act: attempt to update member details with a token that does not belong to
        // the user
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberUsername("invalid-token", "oldPassword", "userToUpdate", "updatedUser");
        }, "Updating member details should throw an exception for an invalid token");
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberPassword("invalid-token", "oldPassword", "userToUpdate", "newPassword");
        }, "Updating member details should throw an exception for an invalid token");
    }

    @Test
    void TestUpdateMembersDetails_UsernameTaken_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up two users
        String sessionToken1 = userService.visitSystem();
        userService.signUp(sessionToken1, "user1", "password1");
        String loginToken1 = userService.login(sessionToken1, "user1", "password1");

        String sessionToken2 = userService.visitSystem();
        userService.signUp(sessionToken2, "user2", "password2");

        // Act: attempt to update user1's username to user2's username
        boolean result = userService.updateMemberUsername(loginToken1, "password1", "user1", "user2");

        // Assert: check that the update fails due to the new username being taken
        assertFalse(result, "Updating member details should fail when the new username is already taken");
    }


    @Test
    void TestExitWithInvalidToken_Acceptance() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(sessionToken, "newUser", "password123");
        String loginToken = userService.login(sessionToken, "newUser", "password123");

        // Act & Assert: attempt to exit with an invalid token and expect an exception
        assertThrows(IllegalArgumentException.class, () -> {
            userService.exit("invalid-token");
        }, "Exit should throw an exception for an invalid token");
    }

    @Test
    void TestExitWithInactiveToken_Acceptance() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(sessionToken, "newUser", "password123");
        String loginToken = userService.login(sessionToken, "newUser", "password123");
        userService.exit(loginToken);

        // Act & Assert: attempt to exit with an inactive token and expect an exception
        assertThrows(IllegalArgumentException.class, () -> {
            userService.exit(loginToken);
        }, "Exit should throw an exception for an inactive token");
    }

    @Test
    void TestMemberExitSuccessful_Acceptance() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(sessionToken, "newUser", "password123");
        String loginToken = userService.login(sessionToken, "newUser", "password123");

        // Act: attempt to sign up with an invalid token
        boolean answer = userService.exit(loginToken); // Exit the member session to make the token invalid

        // Assert: check that the sign-up attempt fails due to invalid token and no new
        // member is added
        assertTrue(answer, "Sign up should succeed with a valid guest token");
    }

    @Test
    void TestGuestExitSuccessful_Acceptance() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();

        // Act: attempt to sign up with an invalid token
        boolean answer = userService.exit(sessionToken); // Exit the member session to make the token invalid

        // Assert: check that the sign-up attempt fails due to invalid token and no new
        // member is added
        assertTrue(answer, "Sign up should succeed with a valid guest token");
    }

    @Test
    void TestLogoutSuccessful_Acceptance() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(sessionToken, "newUser", "password123");
        String loginToken = userService.login(sessionToken, "newUser", "password123");

        // Act: logout with a valid token
        sessionToken = userService.logOut(loginToken);

        // Assert: check that the login token is no longer active and a new guest token
        // is returned and active
        assertNotNull(sessionToken, "Logout should return a new guest token");
        assertTrue(tokenService.validateToken(sessionToken), "New guest token should be valid");
        assertTrue(tokenService.isActiveSession(sessionToken), "New guest token should be active");
        assertFalse(tokenService.isActiveSession(loginToken), "Login token should no longer be active after logout");
    }

    @Test
    void TestLogoutWithInvalidToken_Acceptance() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(sessionToken, "newUser", "password123");
        String loginToken = userService.login(sessionToken, "newUser", "password123");

        // Act & Assert: attempt to logout with an invalid token and expect an exception
        assertThrows(IllegalArgumentException.class, () -> {
            userService.logOut("invalid-token");
        }, "Logout should throw an exception for an invalid token");
    }

    @Test
    void TestLogoutWithInactiveToken_Acceptance() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(sessionToken, "newUser", "password123");
        String loginToken = userService.login(sessionToken, "newUser", "password123");
        userService.logOut(loginToken);

        // Act& Assert: attempt to logout with an inactive token and expect an exception
        assertThrows(IllegalArgumentException.class, () -> {
            userService.logOut(loginToken);
        }, "Logout should throw an exception for an inactive token");
    }

    @Test
    void TestLogoutWithGuestToken_Acceptance() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();

        // Act: logout with a guest token
        String newSessionToken = userService.logOut(sessionToken);

        // Assert: check that the logout attempt fails and the original guest token is
        // still active
        assertNull(newSessionToken, "Logout should return null for a guest token");
        assertTrue(tokenService.isActiveSession(sessionToken),
                "Original guest token should still be active after failed logout");
    }

}
