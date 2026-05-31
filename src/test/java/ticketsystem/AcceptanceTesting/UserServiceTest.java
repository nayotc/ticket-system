package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository, logger);
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
        boolean answer = userService.signUp(sessionToken, "newUser", "password123", "Test User", "0500000000");

        // Assert: check that the new member is added to the repository
        assertTrue(answer, "Sign up should succeed with a unique username");
        assertTrue(userRepository.isUsernameTaken("newUser"), "Username should be taken after sign up");
        assertEquals(1, userRepository.getAllRegisteredMembersCount(),
                "There should be one registered member after sign up");

        assertNotNull(userRepository.getMemberByUsername("newUser"), "Member should be stored after sign up");
        assertEquals("Test User", userRepository.getMemberByUsername("newUser").getFullName(),
                "Full name should be stored after sign up");
        assertEquals("0500000000", userRepository.getMemberByUsername("newUser").getPhone(),
                "Phone should be stored after sign up");
    }

    @Test
    void TestSuccessfulSignUpWithFormattedPhone_ThenPhoneIsNormalized() {
        // Arrange
        String sessionToken = userService.visitSystem();

        // Act
        boolean answer = userService.signUp(
                sessionToken,
                "formattedPhoneUser",
                "password123",
                "Formatted Phone User",
                "050-1234567");

        // Assert
        assertTrue(answer, "Sign up should succeed with a formatted phone number");
        assertNotNull(userRepository.getMemberByUsername("formattedPhoneUser"),
                "Member should be stored after sign up");
        assertEquals("0501234567", userRepository.getMemberByUsername("formattedPhoneUser").getPhone(),
                "Phone should be normalized and stored without separators");
    }

    @Test
    void TestSignUpWithBlankFullName_ThenThrowsException() {
        // Arrange
        String sessionToken = userService.visitSystem();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(
                    sessionToken,
                    "blankFullNameUser",
                    "password123",
                    "   ",
                    "0500000000");
        }, "Sign up should throw when full name is blank");

        assertFalse(userRepository.isUsernameTaken("blankFullNameUser"),
                "User should not be registered when full name is invalid");
    }

    @Test
    void TestSignUpWithTooShortFullName_ThenThrowsException() {
        // Arrange
        String sessionToken = userService.visitSystem();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(
                    sessionToken,
                    "shortFullNameUser",
                    "password123",
                    "A",
                    "0500000000");
        }, "Sign up should throw when full name is too short");

        assertFalse(userRepository.isUsernameTaken("shortFullNameUser"),
                "User should not be registered when full name is invalid");
    }

    @Test
    void TestSignUpWithPhoneContainingLetters_ThenThrowsException() {
        // Arrange
        String sessionToken = userService.visitSystem();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(
                    sessionToken,
                    "invalidPhoneCharactersUser",
                    "password123",
                    "Invalid Phone User",
                    "05012abc67");
        }, "Sign up should throw when phone contains non-digit characters");

        assertFalse(userRepository.isUsernameTaken("invalidPhoneCharactersUser"),
                "User should not be registered when phone is invalid");
    }

    @Test
    void TestSignUpWithTooShortPhone_ThenThrowsException() {
        // Arrange
        String sessionToken = userService.visitSystem();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(
                    sessionToken,
                    "shortPhoneUser",
                    "password123",
                    "Short Phone User",
                    "05012345");
        }, "Sign up should throw when phone is too short");

        assertFalse(userRepository.isUsernameTaken("shortPhoneUser"),
                "User should not be registered when phone is invalid");
    }

    @Test
    void TestSignUpWithTooLongPhone_ThenThrowsException() {
        // Arrange
        String sessionToken = userService.visitSystem();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(
                    sessionToken,
                    "longPhoneUser",
                    "password123",
                    "Long Phone User",
                    "05012345678");
        }, "Sign up should throw when phone is too long");

        assertFalse(userRepository.isUsernameTaken("longPhoneUser"),
                "User should not be registered when phone is invalid");
    }

    @Test
    void TestSignUpWithBlankPhone_ThenThrowsException() {
        // Arrange
        String sessionToken = userService.visitSystem();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(
                    sessionToken,
                    "blankPhoneUser",
                    "password123",
                    "Blank Phone User",
                    "   ");
        }, "Sign up should throw when phone is blank");

        assertFalse(userRepository.isUsernameTaken("blankPhoneUser"),
                "User should not be registered when phone is invalid");
    }

    @Test
    void TestSignUpWithTakenUsername_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken1 = userService.visitSystem();
        userService.signUp(sessionToken1, "existingUser", "password123", "Existing User", "0500000000");

        // Act & Assert: attempt to sign up with the same username
        String sessionToken2 = userService.visitSystem();
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(sessionToken2, "existingUser", "password456", "Another User", "0500000001");
        }, "Sign up should throw when the username is already taken");

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
            userService.signUp(invalidToken, "user", "password", "Test User", "0500000000");
        }, "Sign up should throw an exception for an invalid session token");

    }

    @Test
    void TestLoginWithRightUsernameAndPassword() {
        // Arange: visit and signup a new Member
        String sessionToken1 = userService.visitSystem();
        userService.signUp(sessionToken1, "newUser", "password123", "Test User", "0500000000");
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
        userService.signUp(
                sessionToken1,
                "newUser",
                "password123",
                "Test User",
                "0500000000");
        // Act & Assert: login with the wrong username
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login(sessionToken1, "wrongUser", "password123");
        }, "Login should throw with wrong username");
        assertTrue(tokenService.isActiveSession(sessionToken1),
                "Original session token should still be active after failed login");
    }

    @Test
    void TestLoginWithWrongPassword() {
        // Arange: visit and signup a new Member
        String sessionToken1 = userService.visitSystem();
        userService.signUp(
                sessionToken1,
                "newUser",
                "password123",
                "Test User",
                "0500000000");

        // Act & Assert: login with the wrong password
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login(sessionToken1, "newUser", "password1234");
        }, "Login should throw with wrong password");

        assertTrue(tokenService.isActiveSession(sessionToken1),
                "Original session token should still be active after failed login");
    }

    @Test
    void TestLoginWithWrongToken() {
        // arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "user",
                "password",
                "Test User",
                "0500000000");

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
        userService.signUp(
                sessionToken1,
                "newUser1",
                "password1",
                "Test User One",
                "0500000001");

        String sessionToken2 = userService.visitSystem();
        userService.signUp(
                sessionToken2,
                "newUser2",
                "password2",
                "Test User Two",
                "0500000002");
        // Act & Assert: login with the wrong password
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login(sessionToken1, "newUser1", "password2");
        }, "Login should throw with wrong password");

        assertTrue(tokenService.isActiveSession(sessionToken1),
                "Original session token should still be active after failed login");
    }

    @Test
    void TestUpdateMemberUsername_Successful_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "userToUpdate",
                "oldPassword",
                "User To Update",
                "0500000000");
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");

        // Act: update username then password with correct current password (same steps
        // as before split API)
        boolean result = userService.updateMemberUsername(loginToken, "oldPassword", "userToUpdate", "updatedUser");

        // Assert: check that the update was successful and the member details were
        // updated
        assertTrue(result, "Updating member username should succeed with correct current password");
    }

    @Test
    void TestUpdateMemberPassword_Successful_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "userToUpdate",
                "oldPassword",
                "User To Update",
                "0500000000");
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
        userService.signUp(
                sessionToken1,
                "user1",
                "password1",
                "User One",
                "0500000001");
        String loginToken1 = userService.login(sessionToken1, "user1", "password1");

        String sessionToken2 = userService.visitSystem();
        userService.signUp(
                sessionToken2,
                "user2",
                "password2",
                "User Two",
                "0500000002");
        String loginToken2 = userService.login(sessionToken2, "user2", "password2");

        // Act & Assert: attempt to update user1's username using user2's token
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberUsername(loginToken2, "password1", "user1", "newUser1");
        }, "Updating member username should throw when the token does not belong to the user");

        // Act & Assert: attempt to update user1's password using user2's token
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberPassword(loginToken2, "password1", "user1", "newPassword1");
        }, "Updating member password should throw when the token does not belong to the user");
    }

    @Test
    void TestUpdateMemberDetails_WrongUsername_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "userToUpdate",
                "oldPassword",
                "User To Update",
                "0500000000");
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");

        // Act & Assert: attempt to update username with incorrect current username
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberUsername(loginToken, "oldPassword", "WrongUserToUpdate", "updatedUser");
        }, "Updating member username should throw with incorrect current username");

        // Act & Assert: attempt to update password with incorrect current username
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberPassword(loginToken, "oldPassword", "WrongUserToUpdate", "newPassword");
        }, "Updating member password should throw with incorrect current username");
    }

    @Test
    void TestUpdateMemberDetails_WrongPassword_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "userToUpdate",
                "oldPassword",
                "User To Update",
                "0500000000");
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");

        // Act & Assert: attempt to update username with incorrect current password
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberUsername(loginToken, "wrongPassword", "userToUpdate", "updatedUser");
        }, "Updating member username should throw with incorrect current password");

        // Act & Assert: attempt to update password with incorrect current password
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberPassword(loginToken, "wrongPassword", "userToUpdate", "newPassword");
        }, "Updating member password should throw with incorrect current password");
    }

    @Test
    void TestUpdateMemberDetails_invalidToken_Acceptance() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "userToUpdate",
                "oldPassword",
                "User To Update",
                "0500000000");
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
        userService.signUp(
                sessionToken1,
                "user1",
                "password1",
                "User One",
                "0500000001");
        String loginToken1 = userService.login(sessionToken1, "user1", "password1");

        String sessionToken2 = userService.visitSystem();
        userService.signUp(
                sessionToken2,
                "user2",
                "password2",
                "User Two",
                "0500000002");

        // Act & Assert: attempt to update user1's username to user2's username
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberUsername(loginToken1, "password1", "user1", "user2");
        }, "Updating member username should throw when the new username is already taken");
    }

    @Test
    void TestExitWithInvalidToken_Acceptance() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "newUser",
                "password123",
                "Test User",
                "0500000000");
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
        userService.signUp(
                sessionToken,
                "newUser",
                "password123",
                "Test User",
                "0500000000");
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
        userService.signUp(
                sessionToken,
                "newUser",
                "password123",
                "Test User",
                "0500000000");
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
        userService.signUp(
                sessionToken,
                "newUser",
                "password123",
                "Test User",
                "0500000000");
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
        userService.signUp(
                sessionToken,
                "newUser",
                "password123",
                "Test User",
                "0500000000");
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
        userService.signUp(
                sessionToken,
                "newUser",
                "password123",
                "Test User",
                "0500000000");
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

        // Act & Assert: logout with a guest token
        assertThrows(IllegalStateException.class, () -> {
            userService.logOut(sessionToken);
        }, "Logout should throw for a guest token");

        assertTrue(tokenService.isActiveSession(sessionToken),
                "Original guest token should still be active after failed logout");
    }

    @Test
    void TestSignUpWithBlankUsername_ThenThrowsException() {
        String sessionToken = userService.visitSystem();

        assertThrows(IllegalArgumentException.class,
                () -> userService.signUp(sessionToken, "   ", "password123", "Test User", "0500000000"));
    }

    @Test
    void TestSignUpWithBlankPassword_ThenThrowsException() {
        String sessionToken = userService.visitSystem();

        assertThrows(IllegalArgumentException.class,
                () -> userService.signUp(sessionToken, "user", "   ", "Test User", "0500000000"));
    }

    @Test
    void TestSignUpWithMemberToken_ThenThrowsException() {
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, "user", "password123", "Test User", "0500000000");
        String memberToken = userService.login(guestToken, "user", "password123");

        assertThrows(IllegalStateException.class,
                () -> userService.signUp(memberToken, "anotherUser", "password123", "Another User", "0500000001"));
    }

    @Test
    void TestLoginWithBlankUsername_ThenThrowsException() {
        String sessionToken = userService.visitSystem();

        assertThrows(IllegalArgumentException.class, () -> userService.login(sessionToken, "   ", "password123"));
    }

    @Test
    void TestLoginWithBlankPassword_ThenThrowsException() {
        String sessionToken = userService.visitSystem();

        assertThrows(IllegalArgumentException.class, () -> userService.login(sessionToken, "user", "   "));
    }

    @Test
    void TestLoginWithMemberToken_ThenThrowsException() {
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, "user", "password123", "Test User", "0500000000");
        String memberToken = userService.login(guestToken, "user", "password123");

        assertThrows(IllegalStateException.class, () -> userService.login(memberToken, "user", "password123"));
    }

    @Test
    void TestLoginWithListener_ThenListenerIsCalled() {
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, "listenerUser", "password123", "Listener User", "0500000000");

        final boolean[] wasCalled = {false};

        userService.addUserLoginListener((oldGuestToken, newMemberToken) -> {
            wasCalled[0] = true;
            assertEquals(guestToken, oldGuestToken);
            assertNotNull(newMemberToken);
        });

        String memberToken = userService.login(guestToken, "listenerUser", "password123");

        assertNotNull(memberToken);
        assertTrue(wasCalled[0]);
    }

    @Test
    void TestLoginWithRemovedListener_ThenListenerIsNotCalled() {
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, "removedListenerUser", "password123", "Removed Listener User", "0500000000");

        final boolean[] wasCalled = {false};

        ticketsystem.ApplicationLayer.Events.UserLoginListener listener = (oldGuestToken,
                newMemberToken) -> wasCalled[0] = true;

        userService.addUserLoginListener(listener);
        userService.removeUserLoginListener(listener);

        String memberToken = userService.login(guestToken, "removedListenerUser", "password123");

        assertNotNull(memberToken);
        assertFalse(wasCalled[0]);
    }

    @Test
    void TestLoginWhenListenerThrows_ThenLoginFailsAndMemberTokenIsRolledBack() {
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, "failingListenerUser", "password123", "Failing Listener User", "0500000000");

        userService.addUserLoginListener((oldGuestToken, newMemberToken) -> {
            throw new RuntimeException("listener failed");
        });

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> userService.login(guestToken, "failingListenerUser", "password123"));

        assertEquals("Login failed. Please try again.", exception.getMessage());
        assertTrue(tokenService.isActiveSession(guestToken));
    }

    @Test
    void TestUpdateMemberUsernameWithBlankCurrentUsername_ThenThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateMemberUsername("token", "password", "   ", "newUser"));
    }

    @Test
    void TestUpdateMemberUsernameWithBlankNewUsername_ThenThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateMemberUsername("token", "password", "user", "   "));
    }

    @Test
    void TestUpdateMemberUsernameToSameUsername_ThenSucceeds() {
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, "sameUser", "password123", "Same User", "0500000000");
        String memberToken = userService.login(guestToken, "sameUser", "password123");

        boolean result = userService.updateMemberUsername(
                memberToken,
                "password123",
                "sameUser",
                "sameUser");

        assertTrue(result);
    }

    @Test
    void TestUpdateMemberPasswordWithBlankUsername_ThenThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateMemberPassword("token", "password", "   ", "newPassword"));
    }

    @Test
    void TestUpdateMemberPasswordWithBlankNewPassword_ThenThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateMemberPassword("token", "password", "user", "   "));
    }

}
