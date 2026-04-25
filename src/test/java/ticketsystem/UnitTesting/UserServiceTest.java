package ticketsystem.UnitTesting;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
public class UserServiceTest {
    private IUserRepository mockUserRepository;
    private UserService mockUserService;
    @Test
    void TestGuestEnteringTheSystem_returnValidTokenAndAddOnce() {
        // Arrange: set up the mock behavior
        // Create a mock instance of the repository to isolate the service logic
        mockUserRepository = mock(IUserRepository.class);
        mockUserService = new UserService(mockUserRepository);
        
        // Act: invoke the method under test
        String sessionToken = mockUserService.visitSystem();

        // Assert:
        assertNotNull(sessionToken, "Session token should not be null");
        assertFalse(sessionToken.isEmpty(), "Session token should not be empty");
        verify(mockUserRepository, times(1)).addGuest(sessionToken);
    }
}
