package ticketsystem.AcceptanceTesting;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.Events.UserExitListener;
import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DTO.MyAccountDTO;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.InfrastructureLayer.InMemoryUserRepository;

public class UserServiceTest {

    private static final LocalDate VALID_BIRTH_DATE = LocalDate.of(2001, 1, 1);

    private IUserRepository userRepository;
    private UserService userService;
    private ITokenService tokenService;
    private ITokenRepository tokenRepository;
    private ISystemLogger logger;

    @BeforeEach
    public void setup() {
        logger = new LogbackSystemLogger();
        userRepository = new InMemoryUserRepository();
        tokenRepository = new TokenRepository();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository, logger);
        userService = new UserService(userRepository, tokenService, logger);
    }

    @Test
    void TestVisitSystem_ThenReturnsActiveGuestToken() {
        // Act: invoke the method under test
        String sessionToken = userService.visitSystem();

        // Assert: check that the session token is valid and the guest is added
        assertNotNull(sessionToken, "Session token should not be null");
        assertFalse(sessionToken.isEmpty(), "Session token should not be empty");
        assertFalse(tokenService.isActiveSession("invalid-token"), "Invalid token should not be active");
        assertTrue(tokenService.isActiveSession(sessionToken), "Valid token should be active");

    }

    @Test
    void TestSignUpWithValidData_ThenRegistersMember() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();

        // Act: sign up with a unique username
        boolean answer = userService.signUp(sessionToken, "newUser", "password123", "Test User", "0500000000", VALID_BIRTH_DATE);

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
                "050-1234567", VALID_BIRTH_DATE);

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
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(
                    sessionToken,
                    "blankFullNameUser",
                    "password123",
                    "   ",
                    "0500000000", VALID_BIRTH_DATE);
        }, "Sign up should throw when full name is blank");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "sign up with blank full name");
        assertFalse(userRepository.isUsernameTaken("blankFullNameUser"),
                "User should not be registered when full name is invalid");
    }

    @Test
    void TestSignUpWithTooShortFullName_ThenThrowsException() {
        // Arrange
        String sessionToken = userService.visitSystem();
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(
                    sessionToken,
                    "shortFullNameUser",
                    "password123",
                    "A",
                    "0500000000", VALID_BIRTH_DATE);
        }, "Sign up should throw when full name is too short");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "sign up with too short full name");
        assertFalse(userRepository.isUsernameTaken("shortFullNameUser"),
                "User should not be registered when full name is invalid");
    }

    @Test
    void TestSignUpWithPhoneContainingLetters_ThenThrowsException() {
        // Arrange
        String sessionToken = userService.visitSystem();
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(
                    sessionToken,
                    "invalidPhoneCharactersUser",
                    "password123",
                    "Invalid Phone User",
                    "05012abc67", VALID_BIRTH_DATE);
        }, "Sign up should throw when phone contains non-digit characters");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "sign up with phone containing letters");
        assertFalse(userRepository.isUsernameTaken("invalidPhoneCharactersUser"),
                "User should not be registered when phone is invalid");
    }

    @Test
    void TestSignUpWithTooShortPhone_ThenThrowsException() {
        // Arrange
        String sessionToken = userService.visitSystem();
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(
                    sessionToken,
                    "shortPhoneUser",
                    "password123",
                    "Short Phone User",
                    "05012345", VALID_BIRTH_DATE);
        }, "Sign up should throw when phone is too short");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "sign up with too short phone");
        assertFalse(userRepository.isUsernameTaken("shortPhoneUser"),
                "User should not be registered when phone is invalid");
    }

    @Test
    void TestSignUpWithTooLongPhone_ThenThrowsException() {
        // Arrange
        String sessionToken = userService.visitSystem();
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(
                    sessionToken,
                    "longPhoneUser",
                    "password123",
                    "Long Phone User",
                    "05012345678", VALID_BIRTH_DATE);
        }, "Sign up should throw when phone is too long");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "sign up with too long phone");
        assertFalse(userRepository.isUsernameTaken("longPhoneUser"),
                "User should not be registered when phone is invalid");
    }

    @Test
    void TestSignUpWithBlankPhone_ThenThrowsException() {
        // Arrange
        String sessionToken = userService.visitSystem();
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(
                    sessionToken,
                    "blankPhoneUser",
                    "password123",
                    "Blank Phone User",
                    "   ", VALID_BIRTH_DATE);
        }, "Sign up should throw when phone is blank");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "sign up with blank phone");
        assertFalse(userRepository.isUsernameTaken("blankPhoneUser"),
                "User should not be registered when phone is invalid");
    }

    @Test
    void TestSignUpWithTakenUsername_ThenThrowsException() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken1 = userService.visitSystem();
        userService.signUp(sessionToken1, "existingUser", "password123", "Existing User", "0500000000", VALID_BIRTH_DATE);

        // Act & Assert: attempt to sign up with the same username
        String sessionToken2 = userService.visitSystem();
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(sessionToken2, "existingUser", "password456", "Another User", "0500000001", VALID_BIRTH_DATE);
        }, "Sign up should throw when the username is already taken");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "sign up with taken username");
        assertTrue(userRepository.isUsernameTaken("existingUser"), "Username should be taken");
        assertEquals(1, userRepository.getAllRegisteredMembersCount(),
                "There should still be only one registered member");
    }

    @Test
    void TestSignUpWithInvalidSessionToken_ThenThrowsException() {
        // arrange: simulate a guest visiting the system
        userService.visitSystem();
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);
        String invalidToken = "fake-token";

        // Act & Assert: attempt to sign up with an invalid session token and expect an
        // exception
        assertThrows(IllegalArgumentException.class, () -> {
            userService.signUp(invalidToken, "user", "password", "Test User", "0500000000", VALID_BIRTH_DATE);
        }, "Sign up should throw an exception for an invalid session token");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "sign up with invalid session token");
        assertFalse(userRepository.isUsernameTaken("user"),
                "User should not be registered with invalid session token");
    }

    @Test
    void TestLoginWithValidCredentials_ThenReturnsMemberToken() {
        // Arange: visit and signup a new Member
        String sessionToken1 = userService.visitSystem();
        userService.signUp(sessionToken1, "newUser", "password123", "Test User", "0500000000", VALID_BIRTH_DATE);
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
    void TestLoginWithWrongUsername_ThenThrowsException() {
        // Arange: visit and signup a new Member
        String sessionToken1 = userService.visitSystem();
        userService.signUp(
                sessionToken1,
                "newUser",
                "password123",
                "Test User",
                "0500000000", VALID_BIRTH_DATE);
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);
        // Act & Assert: login with the wrong username
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login(sessionToken1, "wrongUser", "password123");
        }, "Login should throw with wrong username");
        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "login with wrong username");
        assertTrue(tokenService.isActiveSession(sessionToken1),
                "Original session token should still be active after failed login");
    }

    @Test
    void TestLoginWithWrongPassword_ThenThrowsException() {
        // Arange: visit and signup a new Member
        String sessionToken1 = userService.visitSystem();
        userService.signUp(
                sessionToken1,
                "newUser",
                "password123",
                "Test User",
                "0500000000", VALID_BIRTH_DATE);
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert: login with the wrong password
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login(sessionToken1, "newUser", "password1234");
        }, "Login should throw with wrong password");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "login with wrong password");
        assertTrue(tokenService.isActiveSession(sessionToken1),
                "Original session token should still be active after failed login");
    }

    @Test
    void TestLoginWithWrongToken_ThenThrowsException() {
        // arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "user",
                "password",
                "Test User",
                "0500000000", VALID_BIRTH_DATE);
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);
        String invalidToken = "fake-token";

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login(invalidToken, "user", "password");
        }, "Login should throw an exception for an invalid session token");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "login with wrong token");
        assertTrue(tokenService.isActiveSession(sessionToken),
                "Original guest session token should still be active after failed login");
        assertEquals(1, userRepository.getAllRegisteredMembersCount(),
                "Member count should remain unchanged after failed login");
    }

    @Test
    void TestLoginWithMismatchedPassword_ThenThrowsException() {
        // Arange: visit and signup for 2 new Members
        String sessionToken1 = userService.visitSystem();
        userService.signUp(
                sessionToken1,
                "newUser1",
                "password1",
                "Test User One",
                "0500000001", VALID_BIRTH_DATE);

        String sessionToken2 = userService.visitSystem();
        userService.signUp(
                sessionToken2,
                "newUser2",
                "password2",
                "Test User Two",
                "0500000002", VALID_BIRTH_DATE);
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);
        // Act & Assert: login with the wrong password
        assertThrows(IllegalArgumentException.class, () -> {
            userService.login(sessionToken1, "newUser1", "password2");
        }, "Login should throw with wrong password");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "login with mismatched password");
        assertTrue(tokenService.isActiveSession(sessionToken1),
                "Original session token should still be active after failed login");
    }

    @Test
    void TestUpdateMemberUsernameWithValidData_ThenSucceeds() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "userToUpdate",
                "oldPassword",
                "User To Update",
                "0500000000", VALID_BIRTH_DATE);
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");

        // Act: update username then password with correct current password (same steps
        // as before split API)
        boolean result = userService.updateMemberUsername(loginToken, "oldPassword", "userToUpdate", "updatedUser");

        // Assert: check that the update was successful and the member details were
        // updated
        assertTrue(result, "Updating member username should succeed with correct current password");
    }

    @Test
    void TestUpdateMemberPasswordWithValidData_ThenSucceeds() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "userToUpdate",
                "oldPassword",
                "User To Update",
                "0500000000", VALID_BIRTH_DATE);
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");

        // Act: password change while still registered as userToUpdate
        boolean result = userService.updateMemberPassword(loginToken, "oldPassword", "userToUpdate", "newPassword");

        // Assert: check that the update was successful and the member details were
        // updated
        assertTrue(result, "Updating member password should succeed with correct current password");
    }

    @Test
    void TestUpdateMemberDetailsWithDifferentToken_ThenThrowsException() {
        // Arrange: simulate a guest visiting the system and signing up two users
        String sessionToken1 = userService.visitSystem();
        userService.signUp(
                sessionToken1,
                "user1",
                "password1",
                "User One",
                "0500000001", VALID_BIRTH_DATE);
        String loginToken1 = userService.login(sessionToken1, "user1", "password1");

        String sessionToken2 = userService.visitSystem();
        userService.signUp(
                sessionToken2,
                "user2",
                "password2",
                "User Two",
                "0500000002", VALID_BIRTH_DATE);
        String loginToken2 = userService.login(sessionToken2, "user2", "password2");

        MemberSnapshot user1Before = MemberSnapshot.of(userRepository, "user1");
        MemberSnapshot user2Before = MemberSnapshot.of(userRepository, "user2");
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert: attempt to update user1's username using user2's token
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberUsername(loginToken2, "password1", "user1", "newUser1");
        }, "Updating member username should throw when the token does not belong to the user");

        // Act & Assert: attempt to update user1's password using user2's token
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberPassword(loginToken2, "password1", "user1", "newPassword1");
        }, "Updating member password should throw when the token does not belong to the user");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "update member details with different token");
        user1Before.assertUnchanged(MemberSnapshot.of(userRepository, "user1"), "user1");
        user2Before.assertUnchanged(MemberSnapshot.of(userRepository, "user2"), "user2");
    }

    @Test
    void TestUpdateMemberDetailsWithWrongUsername_ThenThrowsException() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "userToUpdate",
                "oldPassword",
                "User To Update",
                "0500000000", VALID_BIRTH_DATE);
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");
        MemberSnapshot before = MemberSnapshot.of(userRepository, "userToUpdate");
        UserSystemSnapshot systemBefore = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert: attempt to update username with incorrect current username
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberUsername(loginToken, "oldPassword", "WrongUserToUpdate", "updatedUser");
        }, "Updating member username should throw with incorrect current username");

        // Act & Assert: attempt to update password with incorrect current username
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberPassword(loginToken, "oldPassword", "WrongUserToUpdate", "newPassword");
        }, "Updating member password should throw with incorrect current username");

        assertSystemStateUnchanged(systemBefore, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "update with wrong username");
        before.assertUnchanged(MemberSnapshot.of(userRepository, "userToUpdate"), "userToUpdate");
    }

    @Test
    void TestUpdateMemberDetailsWithWrongPassword_ThenThrowsException() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "userToUpdate",
                "oldPassword",
                "User To Update",
                "0500000000", VALID_BIRTH_DATE);
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");
        MemberSnapshot before = MemberSnapshot.of(userRepository, "userToUpdate");
        UserSystemSnapshot systemBefore = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert: attempt to update username with incorrect current password
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberUsername(loginToken, "wrongPassword", "userToUpdate", "updatedUser");
        }, "Updating member username should throw with incorrect current password");

        // Act & Assert: attempt to update password with incorrect current password
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberPassword(loginToken, "wrongPassword", "userToUpdate", "newPassword");
        }, "Updating member password should throw with incorrect current password");

        assertSystemStateUnchanged(systemBefore, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "update with wrong password");
        before.assertUnchanged(MemberSnapshot.of(userRepository, "userToUpdate"), "userToUpdate");
    }

    @Test
    void TestUpdateMemberDetailsWithInvalidToken_ThenThrowsException() {
        // Arrange: simulate a guest visiting the system and signing up
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "userToUpdate",
                "oldPassword",
                "User To Update",
                "0500000000", VALID_BIRTH_DATE);
        String loginToken = userService.login(sessionToken, "userToUpdate", "oldPassword");
        MemberSnapshot before = MemberSnapshot.of(userRepository, "userToUpdate");
        UserSystemSnapshot systemBefore = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act: attempt to update member details with a token that does not belong to
        // the user
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberUsername("invalid-token", "oldPassword", "userToUpdate", "updatedUser");
        }, "Updating member details should throw an exception for an invalid token");
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberPassword("invalid-token", "oldPassword", "userToUpdate", "newPassword");
        }, "Updating member details should throw an exception for an invalid token");

        assertSystemStateUnchanged(systemBefore, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "update with invalid token");
        before.assertUnchanged(MemberSnapshot.of(userRepository, "userToUpdate"), "userToUpdate");
        assertTrue(tokenService.isActiveSession(loginToken),
                "Member session should remain active after failed update");
    }

    @Test
    void TestUpdateMemberUsernameToTakenUsername_ThenThrowsException() {
        // Arrange: simulate a guest visiting the system and signing up two users
        String sessionToken1 = userService.visitSystem();
        userService.signUp(
                sessionToken1,
                "user1",
                "password1",
                "User One",
                "0500000001", VALID_BIRTH_DATE);
        String loginToken1 = userService.login(sessionToken1, "user1", "password1");

        String sessionToken2 = userService.visitSystem();
        userService.signUp(
                sessionToken2,
                "user2",
                "password2",
                "User Two",
                "0500000002", VALID_BIRTH_DATE);
        MemberSnapshot user1Before = MemberSnapshot.of(userRepository, "user1");
        MemberSnapshot user2Before = MemberSnapshot.of(userRepository, "user2");
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert: attempt to update user1's username to user2's username
        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateMemberUsername(loginToken1, "password1", "user1", "user2");
        }, "Updating member username should throw when the new username is already taken");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "update username to taken username");
        user1Before.assertUnchanged(MemberSnapshot.of(userRepository, "user1"), "user1");
        user2Before.assertUnchanged(MemberSnapshot.of(userRepository, "user2"), "user2");
    }

    @Test
    void TestExitWithInvalidToken_ThenThrowsException() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "newUser",
                "password123",
                "Test User",
                "0500000000", VALID_BIRTH_DATE);
        String loginToken = userService.login(sessionToken, "newUser", "password123");
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert: attempt to exit with an invalid token and expect an exception
        assertThrows(IllegalArgumentException.class, () -> {
            userService.exit("invalid-token");
        }, "Exit should throw an exception for an invalid token");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "exit with invalid token");
        assertTrue(tokenService.isActiveSession(loginToken),
                "Member session should remain active after failed exit");
    }

    @Test
    void TestExitWithInactiveToken_ThenThrowsException() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "newUser",
                "password123",
                "Test User",
                "0500000000", VALID_BIRTH_DATE);
        String loginToken = userService.login(sessionToken, "newUser", "password123");
        userService.exit(loginToken);
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert: attempt to exit with an inactive token and expect an exception
        assertThrows(IllegalArgumentException.class, () -> {
            userService.exit(loginToken);
        }, "Exit should throw an exception for an inactive token");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "exit with inactive token");
        assertFalse(tokenService.isActiveSession(loginToken),
                "Token should remain inactive after failed exit");
    }

    @Test
    void TestMemberExitWithValidToken_ThenSucceeds() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "newUser",
                "password123",
                "Test User",
                "0500000000", VALID_BIRTH_DATE);
        String loginToken = userService.login(sessionToken, "newUser", "password123");

        // Act: attempt to sign up with an invalid token
        boolean answer = userService.exit(loginToken); // Exit the member session to make the token invalid

        // Assert: check that the sign-up attempt fails due to invalid token and no new
        // member is added
        assertTrue(answer, "Sign up should succeed with a valid guest token");
    }

    @Test
    void TestGuestExitWithValidToken_ThenSucceeds() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();

        // Act: attempt to sign up with an invalid token
        boolean answer = userService.exit(sessionToken); // Exit the member session to make the token invalid

        // Assert: check that the sign-up attempt fails due to invalid token and no new
        // member is added
        assertTrue(answer, "Sign up should succeed with a valid guest token");
    }

    @Test
    void TestLogoutWithValidMemberToken_ThenSucceeds() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "newUser",
                "password123",
                "Test User",
                "0500000000", VALID_BIRTH_DATE);
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
    void TestLogoutWithInvalidToken_ThenThrowsException() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "newUser",
                "password123",
                "Test User",
                "0500000000", VALID_BIRTH_DATE);
        String loginToken = userService.login(sessionToken, "newUser", "password123");
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert: attempt to logout with an invalid token and expect an exception
        assertThrows(IllegalArgumentException.class, () -> {
            userService.logOut("invalid-token");
        }, "Logout should throw an exception for an invalid token");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "logout with invalid token");
        assertTrue(tokenService.isActiveSession(loginToken),
                "Member session should remain active after failed logout");
    }

    @Test
    void TestLogoutWithInactiveToken_ThenThrowsException() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        userService.signUp(
                sessionToken,
                "newUser",
                "password123",
                "Test User",
                "0500000000", VALID_BIRTH_DATE);
        String loginToken = userService.login(sessionToken, "newUser", "password123");
        userService.logOut(loginToken);
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act& Assert: attempt to logout with an inactive token and expect an exception
        assertThrows(IllegalArgumentException.class, () -> {
            userService.logOut(loginToken);
        }, "Logout should throw an exception for an inactive token");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "logout with inactive token");
        assertFalse(tokenService.isActiveSession(loginToken),
                "Token should remain inactive after failed logout");
    }

    @Test
    void TestLogoutWithGuestToken_ThenThrowsException() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        // Act & Assert: logout with a guest token
        assertThrows(IllegalStateException.class, () -> {
            userService.logOut(sessionToken);
        }, "Logout should throw for a guest token");

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "logout with guest token");
        assertTrue(tokenService.isActiveSession(sessionToken),
                "Original guest token should still be active after failed logout");
    }

    @Test
    void TestSignUpWithBlankUsername_ThenThrowsException() {
        String sessionToken = userService.visitSystem();
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        assertThrows(IllegalArgumentException.class,
                () -> userService.signUp(sessionToken, "   ", "password123", "Test User", "0500000000", VALID_BIRTH_DATE));

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "sign up with blank username");
        assertFalse(userRepository.isUsernameTaken("user"),
                "User should not be registered when username is blank");
    }

    @Test
    void TestSignUpWithBlankPassword_ThenThrowsException() {
        String sessionToken = userService.visitSystem();
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        assertThrows(IllegalArgumentException.class,
                () -> userService.signUp(sessionToken, "user", "   ", "Test User", "0500000000", VALID_BIRTH_DATE));

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "sign up with blank password");
        assertFalse(userRepository.isUsernameTaken("user"),
                "User should not be registered when password is blank");
    }

    @Test
    void TestSignUpWithMemberToken_ThenThrowsException() {
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, "user", "password123", "Test User", "0500000000", VALID_BIRTH_DATE);
        String memberToken = userService.login(guestToken, "user", "password123");
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);
        MemberSnapshot userBefore = MemberSnapshot.of(userRepository, "user");

        assertThrows(IllegalStateException.class,
                () -> userService.signUp(memberToken, "anotherUser", "password123", "Another User", "0500000001", VALID_BIRTH_DATE));

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "sign up with member token");
        assertFalse(userRepository.isUsernameTaken("anotherUser"),
                "Second user should not be registered");
        userBefore.assertUnchanged(MemberSnapshot.of(userRepository, "user"), "user");
        assertTrue(tokenService.isActiveSession(memberToken),
                "Member token should remain active after failed sign up");
    }

    @Test
    void TestLoginWithBlankUsername_ThenThrowsException() {
        String sessionToken = userService.visitSystem();
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        assertThrows(IllegalArgumentException.class, () -> userService.login(sessionToken, "   ", "password123"));

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "login with blank username");
        assertTrue(tokenService.isActiveSession(sessionToken),
                "Guest session should remain active after failed login");
    }

    @Test
    void TestLoginWithBlankPassword_ThenThrowsException() {
        String sessionToken = userService.visitSystem();
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        assertThrows(IllegalArgumentException.class, () -> userService.login(sessionToken, "user", "   "));

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "login with blank password");
        assertTrue(tokenService.isActiveSession(sessionToken),
                "Guest session should remain active after failed login");
    }

    @Test
    void TestLoginWithMemberToken_ThenThrowsException() {
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, "user", "password123", "Test User", "0500000000", VALID_BIRTH_DATE);
        String memberToken = userService.login(guestToken, "user", "password123");
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        assertThrows(IllegalStateException.class, () -> userService.login(memberToken, "user", "password123"));

        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "login with member token");
        assertTrue(tokenService.isActiveSession(memberToken),
                "Member token should remain active after failed login");
        assertEquals(1, userRepository.getAllRegisteredMembersCount(),
                "Member count should remain unchanged");
    }

    @Test
    void TestLoginWithListener_ThenListenerIsCalled() {
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, "listenerUser", "password123", "Listener User", "0500000000", VALID_BIRTH_DATE);

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
        userService.signUp(guestToken, "removedListenerUser", "password123", "Removed Listener User", "0500000000", VALID_BIRTH_DATE);

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
        userService.signUp(guestToken, "failingListenerUser", "password123", "Failing Listener User", "0500000000", VALID_BIRTH_DATE);
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);

        userService.addUserLoginListener((oldGuestToken, newMemberToken) -> {
            throw new RuntimeException("listener failed");
        });

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> userService.login(guestToken, "failingListenerUser", "password123"));

        assertEquals("Login failed. Please try again.", exception.getMessage());
        assertTrue(tokenService.isActiveSession(guestToken));
        assertEquals(1, userRepository.getAllRegisteredMembersCount(),
                "Member count should remain unchanged after failed login");
        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "login when listener throws");
        assertTrue(userRepository.isUsernameTaken("failingListenerUser"),
                "Existing member should remain registered after failed login");
    }

    @Test
    void TestUpdateMemberUsernameWithBlankCurrentUsername_ThenThrowsException() {
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateMemberUsername("token", "password", "   ", "newUser"));
        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "update username with blank current username");
    }

    @Test
    void TestUpdateMemberUsernameWithBlankNewUsername_ThenThrowsException() {
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateMemberUsername("token", "password", "user", "   "));
        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "update username with blank new username");
    }

    @Test
    void TestUpdateMemberUsernameToSameUsername_ThenSucceeds() {
        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, "sameUser", "password123", "Same User", "0500000000", VALID_BIRTH_DATE);
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
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateMemberPassword("token", "password", "   ", "newPassword"));
        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "update password with blank username");
    }

    @Test
    void TestUpdateMemberPasswordWithBlankNewPassword_ThenThrowsException() {
        UserSystemSnapshot before = UserSystemSnapshot.capture(userRepository, tokenRepository);
        assertThrows(IllegalArgumentException.class,
                () -> userService.updateMemberPassword("token", "password", "user", "   "));
        assertSystemStateUnchanged(before, UserSystemSnapshot.capture(userRepository, tokenRepository),
                "update password with blank new password");
    }

    private record UserSystemSnapshot(int registeredMembersCount, int activeSessionsCount) {

        static UserSystemSnapshot capture(IUserRepository users, ITokenRepository tokens) {
            return new UserSystemSnapshot(users.getAllRegisteredMembersCount(), tokens.getTotalActiveSessions());
        }
    }

    private record MemberSnapshot(String username, String fullName, String phone, String hashedPassword) {

        static MemberSnapshot of(IUserRepository repository, String username) {
            Member member = repository.getMemberByUsername(username);
            if (member == null) {
                return new MemberSnapshot(username, null, null, null);
            }
            return new MemberSnapshot(
                    username,
                    member.getFullName(),
                    member.getPhone(),
                    repository.getHashedPasswordByUsername(username));
        }

        void assertUnchanged(MemberSnapshot after, String context) {
            assertEquals(username, after.username, context + " – username");
            assertEquals(fullName, after.fullName, context + " – full name");
            assertEquals(phone, after.phone, context + " – phone");
            assertEquals(hashedPassword, after.hashedPassword, context + " – password hash");
        }
    }

    private void assertSystemStateUnchanged(UserSystemSnapshot before, UserSystemSnapshot after, String context) {
        assertEquals(before.registeredMembersCount, after.registeredMembersCount,
                context + " – registered member count");
        assertEquals(before.activeSessionsCount, after.activeSessionsCount,
                context + " – active session count");
    }

    @Test
    void TestSignUpWithTooShortPassword_ThenThrowsException() {
        String sessionToken = userService.visitSystem();

        assertThrows(IllegalArgumentException.class, ()
                -> userService.signUp(
                        sessionToken,
                        "shortPasswordUser",
                        "1234",
                        "Short Password User",
                        "0500000000",
                        LocalDate.of(2001, 1, 1)));

        assertFalse(userRepository.isUsernameTaken("shortPasswordUser"));
    }

    @Test
    void TestGetMyAccountDTO_WhenLoggedIn_ThenReturnsMemberDetails() {
        String guestToken = userService.visitSystem();

        userService.signUp(
                guestToken,
                "accountUser",
                "password123",
                "Account User",
                "050-1234567",
                LocalDate.of(2001, 1, 1));

        String memberToken = userService.login(guestToken, "accountUser", "password123");

        MyAccountDTO dto = userService.getMyAccountDTO(memberToken);

        assertNotNull(dto);
        assertEquals("accountUser", dto.getEmail());
        assertEquals("Account User", dto.getFullName());
        assertEquals("0501234567", dto.getPhone());
        assertEquals(LocalDate.of(2001, 1, 1), dto.getBirthDate());
    }

    @Test
    void TestGetMyAccountDTO_WithGuestToken_ThenThrowsException() {
        String guestToken = userService.visitSystem();

        assertThrows(IllegalArgumentException.class,
                () -> userService.getMyAccountDTO(guestToken));
    }

    @Test
    void TestGetMemberById_WhenMemberExists_ThenReturnsMember() {
        String guestToken = userService.visitSystem();

        userService.signUp(
                guestToken,
                "memberByIdUser",
                "password123",
                "Member By Id",
                "0500000000",
                LocalDate.of(2001, 1, 1));

        Member member = userRepository.getMemberByUsername("memberByIdUser");

        Member result = userService.getMemberById(member.getId());

        assertNotNull(result);
        assertEquals("memberByIdUser", result.getUserName());
    }

    @Test
    void TestGetMemberById_WhenMemberDoesNotExist_ThenReturnsNull() {
        Member result = userService.getMemberById(999999L);

        assertNull(result);
    }

    @Test
    void TestGetMemberById_WhenIdIsNull_ThenThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> userService.getMemberById(null));
    }

    @Test
    void TestGetAllUsers_WhenUsersExist_ThenReturnsAllMembers() {
        String token1 = userService.visitSystem();
        userService.signUp(token1, "userA", "password123", "User A", "0500000001", LocalDate.of(2001, 1, 1));

        String token2 = userService.visitSystem();
        userService.signUp(token2, "userB", "password123", "User B", "0500000002", LocalDate.of(2001, 1, 1));

        List<Member> users = userService.getAllUsers();

        assertEquals(2, users.size());
    }

    @Test
    void TestGuestExitWithExitListener_ThenListenerIsCalled() {
        String guestToken = userService.visitSystem();

        final boolean[] wasCalled = {false};

        userService.addUserExitListener(token -> {
            wasCalled[0] = true;
            assertEquals(guestToken, token);
        });

        boolean result = userService.exit(guestToken);

        assertTrue(result);
        assertTrue(wasCalled[0]);
    }

    @Test
    void TestGuestExitWithRemovedExitListener_ThenListenerIsNotCalled() {
        String guestToken = userService.visitSystem();

        final boolean[] wasCalled = {false};

        UserExitListener listener = token -> wasCalled[0] = true;

        userService.addUserExitListener(listener);
        userService.removeUserExitListener(listener);

        boolean result = userService.exit(guestToken);

        assertTrue(result);
        assertFalse(wasCalled[0]);
    }

    @Test
    void TestUpdateMemberFullName_WhenValid_ThenUpdatesFullName() {
        String guestToken = userService.visitSystem();

        userService.signUp(
                guestToken,
                "fullNameUser",
                "password123",
                "Old Name",
                "0500000000",
                LocalDate.of(2001, 1, 1));

        String memberToken = userService.login(guestToken, "fullNameUser", "password123");

        boolean result = userService.updateMemberFullName(
                memberToken,
                "password123",
                "fullNameUser",
                "New Name");

        assertTrue(result);
        assertEquals("New Name", userRepository.getMemberByUsername("fullNameUser").getFullName());
    }

    @Test
    void TestUpdateMemberFullName_WhenBlank_ThenThrowsException() {
        String guestToken = userService.visitSystem();

        userService.signUp(
                guestToken,
                "blankUpdateNameUser",
                "password123",
                "Old Name",
                "0500000000",
                LocalDate.of(2001, 1, 1));

        String memberToken = userService.login(guestToken, "blankUpdateNameUser", "password123");

        assertThrows(IllegalArgumentException.class, ()
                -> userService.updateMemberFullName(
                        memberToken,
                        "password123",
                        "blankUpdateNameUser",
                        "   "));
    }

    @Test
    void TestUpdateMemberPhone_WhenValidFormattedPhone_ThenUpdatesNormalizedPhone() {
        String guestToken = userService.visitSystem();

        userService.signUp(
                guestToken,
                "phoneUpdateUser",
                "password123",
                "Phone User",
                "0500000000",
                LocalDate.of(2001, 1, 1));

        String memberToken = userService.login(guestToken, "phoneUpdateUser", "password123");

        boolean result = userService.updateMemberPhone(
                memberToken,
                "password123",
                "phoneUpdateUser",
                "052-1234567");

        assertTrue(result);
        assertEquals("0521234567", userRepository.getMemberByUsername("phoneUpdateUser").getPhone());
    }

    @Test
    void TestUpdateMemberPhone_WhenInvalidPhone_ThenThrowsException() {
        String guestToken = userService.visitSystem();

        userService.signUp(
                guestToken,
                "invalidUpdatePhoneUser",
                "password123",
                "Phone User",
                "0500000000",
                LocalDate.of(2001, 1, 1));

        String memberToken = userService.login(guestToken, "invalidUpdatePhoneUser", "password123");

        assertThrows(IllegalArgumentException.class, ()
                -> userService.updateMemberPhone(
                        memberToken,
                        "password123",
                        "invalidUpdatePhoneUser",
                        "052-abc4567"));
    }

}
