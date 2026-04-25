package ticketsystem.ApplicationLayer;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.InfrastructureLayer.UserRepository;
public class UserServiceTest {
    private IUserRepository mockuserRepository;
    private UserService userService;
    private IUserRepository realUserRepository;
    private UserService realUserService;
    @Test
    void TestGuestEnteringTheSystem_returnValidTokenAndAddOnce() {
        // Arrange: set up the mock behavior
        // Create a mock instance of the repository to isolate the service logic
        mockuserRepository = mock(IUserRepository.class);
        userService = new UserService(mockuserRepository);
        
        when(mockuserRepository.isActiveGuest(anyString())).thenReturn(false);
        // Act: invoke the method under test
        String sessionToken = userService.visitSystem();

        // Assert:
        assertNotNull(sessionToken, "Session token should not be null");
        assertFalse(sessionToken.isEmpty(), "Session token should not be empty");
        verify(mockuserRepository, times(1)).addGuest(sessionToken);
    }
    @Test
    void TestSuccessfulSystemVisit_Acceptance(){
        // Arrange: use the real repository to test the actual behavior
        realUserRepository = new UserRepository();
        UserService realUserService = new UserService(realUserRepository);

        // Act: invoke the method under test
        String sessionToken = realUserService.visitSystem();

        // Assert: check that the session token is valid and the guest is added
        assertNotNull(sessionToken, "Session token should not be null");
        assertFalse(sessionToken.isEmpty(), "Session token should not be empty");
        assertFalse(realUserRepository.isActiveGuest("invalid-token"), "Invalid token should not be active");
        assertTrue(realUserRepository.isActiveGuest(sessionToken), "Valid token should be active");

    }

    
}
