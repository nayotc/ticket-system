package ticketsystem.UnitTesting;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.user.*;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MembershipDomainServiceTest {

    private MembershipDomainService domainService;
    private IUserRepository userRepositoryMock;
    
    private Member appointerMock;
    private Member appointeeMock;
    private Company companyMock;
    private final Long companyId = 1L;

    @BeforeEach
    public void setUp() {
        userRepositoryMock = mock(IUserRepository.class);
        domainService = new MembershipDomainService(userRepositoryMock);
        
        appointerMock = mock(Member.class);
        when(appointerMock.getId()).thenReturn(100L);
        
        appointeeMock = mock(Member.class);
        when(appointeeMock.getId()).thenReturn(200L);
        
        companyMock = mock(Company.class);
        when(companyMock.getId()).thenReturn(companyId);
    }

    // --- Validate Permission ---

    @Test
    public void GivenNullRole_WhenValidatePermission_ThenReturnFalse() {
        // Arrange
        when(userRepositoryMock.getMemberById(100L)).thenReturn(appointerMock);
        when(appointerMock.getRoleInCompany(companyId)).thenReturn(null);

        // Act & Assert
        assertFalse(domainService.validatePermission(100L, companyId, Permission.MANAGE_EVENT_INVENTORY));
    }

    @Test
    public void GivenPendingRole_WhenValidatePermission_ThenReturnFalse() {
        // Arrange
        CompanyRole pendingRole = mock(CompanyRole.class);
        when(pendingRole.getStatus()).thenReturn(RoleStatus.PENDING);
        
        when(userRepositoryMock.getMemberById(100L)).thenReturn(appointerMock);
        when(appointerMock.getRoleInCompany(companyId)).thenReturn(pendingRole);

        // Act & Assert
        assertFalse(domainService.validatePermission(100L, companyId, Permission.MANAGE_EVENT_INVENTORY));
    }

    @Test
    public void GivenActiveRoleWithPermission_WhenValidatePermission_ThenReturnTrue() {
        // Arrange
        CompanyRole activeRole = mock(CompanyRole.class);
        when(activeRole.getStatus()).thenReturn(RoleStatus.ACTIVE);
        when(activeRole.hasPermission(Permission.MANAGE_EVENT_INVENTORY)).thenReturn(true);
        
        when(userRepositoryMock.getMemberById(100L)).thenReturn(appointerMock);
        when(appointerMock.getRoleInCompany(companyId)).thenReturn(activeRole);

        // Act & Assert
        assertTrue(domainService.validatePermission(100L, companyId, Permission.MANAGE_EVENT_INVENTORY));
    }

    // --- Manager Assignment Request ---

    @Test
    public void GivenAppointerHasNoRole_WhenManagerAssignmentRequest_ThenThrowException() {
        // Arrange
        when(appointerMock.getRoleInCompany(companyId)).thenReturn(null);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ManagerAssignmentRequest(appointerMock, appointeeMock, companyId, new HashSet<>());
        });
        assertEquals("You do not have a role in this company.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsPending_WhenManagerAssignmentRequest_ThenThrowException() {
        // Arrange
        CompanyRole pendingRole = mock(CompanyRole.class);
        when(pendingRole.getStatus()).thenReturn(RoleStatus.PENDING);
        when(appointerMock.getRoleInCompany(companyId)).thenReturn(pendingRole);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ManagerAssignmentRequest(appointerMock, appointeeMock, companyId, new HashSet<>());
        });
        assertEquals("Your role is not active yet. You cannot appoint others.", exception.getMessage());
    }

    @Test
    public void GivenAppointerIsManager_WhenManagerAssignmentRequest_ThenThrowException() {
        // Arrange
        Manager managerRole = mock(Manager.class);
        when(managerRole.getStatus()).thenReturn(RoleStatus.ACTIVE);
        when(appointerMock.getRoleInCompany(companyId)).thenReturn(managerRole);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ManagerAssignmentRequest(appointerMock, appointeeMock, companyId, new HashSet<>());
        });
        assertEquals("Only Owners and Founders can appoint others.", exception.getMessage());
    }

    @Test
    public void GivenAppointeeHasRole_WhenManagerAssignmentRequest_ThenThrowException() {
        // Arrange
        Owner ownerRole = mock(Owner.class);
        when(ownerRole.getStatus()).thenReturn(RoleStatus.ACTIVE);
        when(appointerMock.getRoleInCompany(companyId)).thenReturn(ownerRole);
        
        CompanyRole existingRole = mock(CompanyRole.class);
        when(appointeeMock.getRoleInCompany(companyId)).thenReturn(existingRole);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ManagerAssignmentRequest(appointerMock, appointeeMock, companyId, new HashSet<>());
        });
        assertEquals("This user already has an active or pending role in this company.", exception.getMessage());
    }

    @Test
    public void GivenValidOwner_WhenManagerAssignmentRequest_ThenAddRole() throws Exception {
        // Arrange
        Owner ownerRole = mock(Owner.class);
        when(ownerRole.getStatus()).thenReturn(RoleStatus.ACTIVE);
        when(appointerMock.getRoleInCompany(companyId)).thenReturn(ownerRole);
        when(appointeeMock.getRoleInCompany(companyId)).thenReturn(null);
        
        Set<Permission> perms = new HashSet<>();
        
        // Act
        domainService.ManagerAssignmentRequest(appointerMock, appointeeMock, companyId, perms);
        
        // Assert
        verify(appointeeMock, times(1)).addManagerRole(companyId, 100L, perms);
    }

    // --- Approve Assignment ---

    @Test
    public void GivenNoPendingRole_WhenApproveAssignment_ThenThrowException() {
        // Arrange
        when(appointeeMock.getRoleInCompany(companyId)).thenReturn(null);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ApproveAssignment(appointerMock, appointeeMock, companyMock);
        });
        assertEquals("No pending role invitation found.", exception.getMessage());
    }

    @Test
    public void GivenRoleAlreadyActive_WhenApproveAssignment_ThenThrowException() {
        // Arrange
        Manager activeRole = mock(Manager.class);
        when(activeRole.getStatus()).thenReturn(RoleStatus.ACTIVE);
        when(appointeeMock.getRoleInCompany(companyId)).thenReturn(activeRole);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.ApproveAssignment(appointerMock, appointeeMock, companyMock);
        });
        assertEquals("This role is already active.", exception.getMessage());
    }
    
    @Test
    public void GivenValidApproval_WhenApproveAssignment_ThenStatusBecomesActive() throws Exception {
        // Arrange
        Manager pendingRole = new Manager(companyId, 100L, new HashSet<>());
        when(appointeeMock.getRoleInCompany(companyId)).thenReturn(pendingRole);
        
        Founder appointerRole = new Founder(companyId);
        when(appointerMock.getRoleInCompany(companyId)).thenReturn(appointerRole);

        // Act
        domainService.ApproveAssignment(appointerMock, appointeeMock, companyMock);

        // Assert
        assertEquals(RoleStatus.ACTIVE, pendingRole.getStatus());
        assertTrue(appointerRole.getAppointeesMemberIds().contains(200L)); // 200L is appointeeMock ID
        verify(companyMock, times(1)).registerNewAppointment(eq(100L), eq(200L), anyString());
    }

    // --- Reject Assignment ---

    @Test
    public void GivenNoPendingRole_WhenRejectAssignment_ThenThrowException() {
        // Arrange
        when(appointeeMock.getRoleInCompany(companyId)).thenReturn(null);
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            domainService.RejectAssignment(appointerMock, appointeeMock, companyId);
        });
        assertEquals("No pending role invitation found.", exception.getMessage());
    }

    @Test
    public void GivenValidRejection_WhenRejectAssignment_ThenRoleDeletedAndAppointeeRemoved() throws Exception {
        // Arrange
        Manager pendingRole = new Manager(companyId, 100L, new HashSet<>());
        when(appointeeMock.getRoleInCompany(companyId)).thenReturn(pendingRole);
        
        Founder appointerRole = new Founder(companyId);
        appointerRole.addAppointee(200L); // simulate that the appointer has the appointee
        when(appointerMock.getRoleInCompany(companyId)).thenReturn(appointerRole);

        // Act
        domainService.RejectAssignment(appointerMock, appointeeMock, companyId);

        // Assert
        assertFalse(appointerRole.getAppointeesMemberIds().contains(200L));
        verify(appointeeMock, times(1)).deleteRoleInCompany(companyId);
    }
}
