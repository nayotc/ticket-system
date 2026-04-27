package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.InfrastructureLayer.UserRepository;

public class UserServiceTest {
    private UserRepository userRepository;
    private UserService userService;
    private ITokenService tokenService;

    @BeforeEach
    public void setup() {
        userRepository = new UserRepository();
        tokenService = new TokenService();
        userService = new UserService(userRepository, tokenService);
    }

    @Test
    void TestSuccessfulSystemVisit_Acceptance() {
        // Act: invoke the method under test
        String sessionToken = userService.visitSystem();

        // Assert: check that the session token is valid and the guest is added
        assertNotNull(sessionToken, "Session token should not be null");
        assertFalse(sessionToken.isEmpty(), "Session token should not be empty");
        assertFalse(userRepository.isActiveSession("invalid-token"), "Invalid token should not be active");
        assertTrue(userRepository.isActiveSession(sessionToken), "Valid token should be active");

    }

    @Test
    void TestSuccessfulSignUp_Acceptance() {
        // Arrange: simulate a guest visiting the system
        String sessionToken = userService.visitSystem();

        // Act: sign up with a unique username
        userService.signUp(sessionToken, "newUser", "password123");

        // Assert: check that the new member is added to the repository
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
        userService.signUp(sessionToken2, "existingUser", "password456");

        // Assert: check that the second sign-up attempt fails due to username being
        // taken
        assertTrue(userRepository.isUsernameTaken("existingUser"), "Username should be taken");
        assertEquals(userRepository.getAllRegisteredMembersCount(), 1,
                "There should still be only one registered member");
    }
}
