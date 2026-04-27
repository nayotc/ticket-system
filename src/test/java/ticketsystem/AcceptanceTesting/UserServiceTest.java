package ticketsystem.AcceptanceTesting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.InfrastructureLayer.UserRepository;

public class UserServiceTest {
        private UserRepository userRepository;
        private UserService userService;
        private ITokenService tokenService;
    @Test
    void TestSuccessfulSystemVisit_Acceptance(){
        // Arrange: use the real repository to test the actual behavior
        userRepository = new UserRepository();
        tokenService = new TokenService();  
        userService = new UserService(userRepository, tokenService);

        // Act: invoke the method under test
        String sessionToken = userService.visitSystem();

        // Assert: check that the session token is valid and the guest is added
        assertNotNull(sessionToken, "Session token should not be null");
        assertFalse(sessionToken.isEmpty(), "Session token should not be empty");
        assertFalse(userRepository.isActiveGuest("invalid-token"), "Invalid token should not be active");
        assertTrue(userRepository.isActiveGuest(sessionToken), "Valid token should be active");

    }
}
