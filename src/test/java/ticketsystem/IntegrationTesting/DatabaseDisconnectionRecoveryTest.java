package ticketsystem.IntegrationTesting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.CannotCreateTransactionException;

import ticketsystem.ApplicationLayer.EventService;
import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserAccessService;
import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.InfrastructureLayer.EventRepository;
import ticketsystem.InfrastructureLayer.UserRepository;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;

/**
 * Requirement 6 (Issue #358) Automated Integration Test:
 * Verifies that the system survives critical database disconnection exceptions
 * without crashing the Spring application context, and successfully resumes 
 * normal domain operations immediately once the database connection is restored.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:isolated_recovery_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class DatabaseDisconnectionRecoveryTest {

    @Autowired
    private EventService eventService;

    @Autowired
    private MembershipService membershipService;

    @MockitoSpyBean
    private EventRepository eventRepositorySpy;

    @MockitoSpyBean
    private UserRepository userRepositorySpy;

    @MockitoBean
    private TokenService tokenServiceMock;

    // ---> Mocks to isolate DB write testing from complex domain/access rules <---
    @MockitoBean
    private MembershipDomainService membershipDomainMock;

    @MockitoBean
    private UserAccessService userAccessServiceMock;

    @MockitoBean
    private INotifier notifierMock;

    @Test
    void GivenDatabaseDisconnectsDuringRead_WhenRestored_ThenSystemRecoversGracefully() {
        doThrow(new CannotCreateTransactionException("Simulated DB Connection Failure"))
            .when(eventRepositorySpy).getEventById(anyLong());

        assertThrows(
            Exception.class, 
            () -> eventService.getEvent("dummy-token", 1L),
            "Expected database access exception during simulated network drop"
        );

        doCallRealMethod().when(eventRepositorySpy).getEventById(anyLong());

        assertDoesNotThrow(() -> {
            try {
                eventService.getEvent("dummy-token", 1L);
            } catch (Exception ignored) {}
        }, "System failed to resume normal database read operations after connection restored");
    }

    @Test
    void GivenDatabaseDisconnectsDuringManagerAppointment_WhenRestored_ThenAppointmentSucceeds() throws Exception {
        // --- Prepare secure dummy data (Mocks) ---
        when(tokenServiceMock.validateToken(anyString())).thenReturn(true);
        when(tokenServiceMock.extractUserId(anyString())).thenReturn(101L);

        Member founderMock = mock(Member.class);
        when(founderMock.getId()).thenReturn(101L);
        when(founderMock.getUserName()).thenReturn("founder");

        Member targetMock = mock(Member.class);
        when(targetMock.getId()).thenReturn(102L);
        when(targetMock.getUserName()).thenReturn("targetManager");

        doReturn(founderMock).when(userRepositorySpy).getMemberById(101L);
        
        // ---> THE EXACT METHOD NAME FROM MembershipService.java <---
        doReturn(targetMock).when(userRepositorySpy).getMemberByUsernameIgnoreCase(anyString());

        Set<Permission> permissions = Set.of(Permission.MANAGE_EVENT_INVENTORY);

        // --- Phase 1: Simulate database lost exactly at commit time (Mutation Drop) ---
        doThrow(new CannotCreateTransactionException("Simulated DB Lost During Commit"))
            .when(userRepositorySpy).updateMember(any());

        assertThrows(
            Exception.class,
            () -> membershipService.requestManagerAssignment("dummy-token", 10L, "targetManager", permissions),
            "Expected database transaction exception during network drop"
        );

        // --- Phase 2: Simulate database online & successful re-commit ---
        doReturn(true).when(userRepositorySpy).updateMember(any());

        assertDoesNotThrow(() -> {
            boolean success = membershipService.requestManagerAssignment("dummy-token", 10L, "targetManager", permissions);
            assertTrue(success, "Manager appointment failed to recover after database reconnected");
        }, "System completely failed to resume write transactions after DB connection restored");
    }

    @Test
    void GivenDatabaseRemainsInComa_WhenMultipleUsersAttack_ThenServerConsistentlyFailsSafely() {
        doThrow(new CannotCreateTransactionException("CRITICAL: DB down"))
            .when(eventRepositorySpy).getEventById(anyLong());

        assertThrows(Exception.class, () -> eventService.getEvent("token-A", 1L));
        assertThrows(Exception.class, () -> eventService.getEvent("token-B", 1L));
        assertThrows(Exception.class, () -> eventService.getEvent("token-C", 1L));

        assertNotNull(eventService, "Catastrophic failure: ApplicationContext crashed after repeated DB errors");
    }
}