package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.Guest;

public class UserServiceTest {
    private IUserRepository mockUserRepository;
    private UserService mockUserService;
    private TokenService mockTokenService;

    @BeforeEach
    void setUp() {
        // Create mock instances of the dependencies
        mockUserRepository = mock(IUserRepository.class);
        mockTokenService = mock(TokenService.class);
        // Initialize the service with mocked dependencies
        mockUserService = new UserService(mockUserRepository, mockTokenService);
    }

    @Test
    void TestGuestEnteringTheSystem_returnValidTokenAndAddOnce() {
        // Arrange: set up the mock behavior
        // Create a mock instance of the repository to isolate the service logic
        when(mockTokenService.generateNewGuestToken()).thenReturn("validToken");
        when(mockUserRepository.addActiveSession(anyString(), any(Guest.class))).thenReturn(true);

        // Act: invoke the method under test
        String sessionToken = mockUserService.visitSystem();

        // Assert:
        assertNotNull(sessionToken, "Session token should not be null");
        assertFalse(sessionToken.isEmpty(), "Session token should not be empty");
        verify(mockUserRepository, times(1)).addActiveSession(anyString(), any(Guest.class));
    }

    @Test
    void TestGuestSigningUpToTheSystem_WithUniqeUsername() {
        // Arrange: set up the mock behavior
        when(mockTokenService.validateToken("validToken")).thenReturn(true);
        when(mockUserRepository.isUsernameTaken("newUser")).thenReturn(false);
        when(mockUserRepository.isIDTaken(anyLong())).thenReturn(false);
        when(mockUserRepository.addRegisteredMember(anyLong(), any())).thenReturn(true);
        // Act: invoke the method under test
        mockUserService.signUp("validToken", "newUser", "password123");
        // Assert: verify that the repository method was called with the expected parameters
        verify(mockUserRepository, times(1)).addRegisteredMember(anyLong(), any());
    }
    @Test
    void TestGuestSigningUpToTheSystem_WithExistingUsername() {
        // Arrange: set up the mock behavior
        when(mockTokenService.validateToken("validToken")).thenReturn(true);
        when(mockUserRepository.isUsernameTaken("newUser")).thenReturn(true);
        when(mockUserRepository.isIDTaken(anyLong())).thenReturn(false);
        when(mockUserRepository.addRegisteredMember(anyLong(), any())).thenReturn(true);
        // Act: invoke the method under test
        mockUserService.signUp("validToken", "newUser", "password123");
        // Assert: verify that the repository method was called with the expected parameters
        verify(mockUserRepository, times(0)).addRegisteredMember(anyLong(), any());
    }
}
