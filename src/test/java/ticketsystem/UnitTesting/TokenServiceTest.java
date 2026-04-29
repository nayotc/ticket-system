package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.TokenService;

public class TokenServiceTest {
    private static TokenService tokenService;

    @BeforeAll
    public static void setUp() {
        tokenService = new TokenService();
    }

    @Test
    public void testGenerateNewGuestToken() {
        // Act: generate a new guest token
        String token = tokenService.generateNewGuestToken();
        // Assert: verify that the token is not null and has a valid format (e.g.,
        // non-empty string)
        assertNotNull(token, "Generated token should not be null");
        assertFalse(token.isEmpty(), "Generated token should not be empty");
    }

    @Test
    public void testGenerateNewMemberToken() {
        // Arrange: define a user ID for the member
        Long userId = 123L;
        // Act: generate a new member token using the user ID
        String token = tokenService.generateNewMemberToken(userId);
        // Assert: verify that the token is not null and has a valid format (e.g.,
        // non-empty string)
        assertNotNull(token, "Generated token should not be null");
        assertFalse(token.isEmpty(), "Generated token should not be empty");
        assertEquals(userId, tokenService.extractUserId(token), "Extracted subject should match the user ID");
    }

    @Test
    public void testValidateToken() {
        // Arrange: generate a valid token for testing
        String validToken = tokenService.generateNewGuestToken();
        // Act: validate the generated token
        boolean isValid = tokenService.validateToken(validToken);
        // Assert: verify that the token is valid
        assertTrue(isValid, "Generated token should be valid");
    }

    @Test
    public void testExtractRole() {
        // Arrange: generate a token for testing
        String token = tokenService.generateNewGuestToken();
        // Act: extract the role from the token
        String role = tokenService.extractRole(token);
        // Assert: verify that the extracted role is correct (e.g., "guest" for a guest
        // token)
        assertEquals("GUEST", role, "Extracted role should be 'GUEST'");
    }

}
