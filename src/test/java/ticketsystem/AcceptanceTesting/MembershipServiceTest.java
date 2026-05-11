package ticketsystem.AcceptanceTesting;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.ApplicationLayer.NotificationsService;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class MembershipServiceTest {

    @Mock private ITokenService tokenService;
    @Mock private IUserRepository userRepository;
    @Mock private ICompanyRepository companyRepository;
    @Mock private MembershipDomainService domainService;
    @Mock private NotificationsService notificationsService;

    private MembershipService membershipService;

    // Domain objects for testing
    private Company testCompany;
    private final Long companyId = 1L;
    
    private Member founderMember;
    private final Long founderId = 100L;
    
    private Member ownerMember;
    private final Long ownerId = 101L;
    
    private Member managerMember;
    private final Long managerId = 102L;
    
    private Member member;
    private final Long memberId = 103L;

    // Tokens for authentication in tests
    private final String appointerToken = "valid-appointer-token";
    private final String appointeeToken = "valid-appointee-token";

    @BeforeEach
    void setUp() {
        // Initializes the Mock objects
        MockitoAnnotations.openMocks(this);

        // Injecting the mocks into the service
        membershipService = new MembershipService(tokenService, userRepository, companyRepository, domainService, notificationsService);

        // Setting up the company
        testCompany = mock(Company.class);
        when(testCompany.getId()).thenReturn(companyId);

        // 1. Setting up Founder
        founderMember = mock(Member.class);
        when(founderMember.getId()).thenReturn(founderId);
        Founder founderRole = new Founder(companyId); // Status is ACTIVE by default
        when(founderMember.getRoleInCompany(companyId)).thenReturn(founderRole);

        // 2. Setting up Owner 
        ownerMember = mock(Member.class);
        when(ownerMember.getId()).thenReturn(ownerId);
        Owner ownerRole = new Owner(companyId, founderId);
        ownerRole.setStatus(RoleStatus.ACTIVE); // Activating the role
        when(ownerMember.getRoleInCompany(companyId)).thenReturn(ownerRole);

        // 3. Setting up Manager
        managerMember = mock(Member.class);
        when(managerMember.getId()).thenReturn(managerId);
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_INQUIRIES);
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);
        Manager managerRole = new Manager(companyId, founderId, permissions);
        managerRole.setStatus(RoleStatus.ACTIVE);
        when(managerMember.getRoleInCompany(companyId)).thenReturn(managerRole);

        // 4. Setting up a regular member with no role in the company
        member = mock(Member.class);
        when(member.getId()).thenReturn(memberId);
        when(member.getRoleInCompany(companyId)).thenReturn(null);
    }
    
    // =========================================================================================
    // Tests for: requestManagerAssignment
    // =========================================================================================

    @Test
    public void GivenValidDetails_WhenRequestManagerAssignment_ThenRequestIsSent() throws Exception {
        // Arrange
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);
        
        when(tokenService.validateToken(appointerToken)).thenReturn(true);
        when(tokenService.extractUserId(appointerToken)).thenReturn(founderId);
        when(userRepository.getMemberById(founderId)).thenReturn(founderMember);
        when(userRepository.getMemberById(memberId)).thenReturn(member);

        // Act
        String requestResult = membershipService.requestManagerAssignment(appointerToken, companyId, memberId, permissions);

        // Assert
        assertEquals("Manager assignment request sent successfully.", requestResult);
        verify(domainService, times(1)).validateAssignmentRequest(any(), any());
        verify(member, times(1)).addManagerRole(companyId, founderId, permissions);
        verify(userRepository, times(1)).updateMember(member);
    }

    @Test
    public void GivenTargetAlreadyHasRole_WhenRequestManagerAssignment_ThenThrowsException() throws Exception {
        // Arrange
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);

        when(tokenService.validateToken(appointerToken)).thenReturn(true);
        when(tokenService.extractUserId(appointerToken)).thenReturn(founderId);
        when(userRepository.getMemberById(founderId)).thenReturn(founderMember);
        when(userRepository.getMemberById(managerId)).thenReturn(managerMember);

        // Mock exact Exception from MembershipDomainService
        doThrow(new Exception("This user already has an active or pending role in this company."))
            .when(domainService).validateAssignmentRequest(any(), any());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.requestManagerAssignment(appointerToken, companyId, managerId, permissions);
        });

        assertEquals("This user already has an active or pending role in this company.", exception.getMessage());
        verify(userRepository, never()).updateMember(managerMember);
    }

    @Test
    public void GivenAppointerIsManager_WhenRequestManagerAssignment_ThenThrowsException() throws Exception {
        // Arrange: Manager tries to appoint another manager (forbidden)
        Set<Permission> permissions = new HashSet<>();
        String managerToken = "manager-token";
        
        when(tokenService.validateToken(managerToken)).thenReturn(true);
        when(tokenService.extractUserId(managerToken)).thenReturn(managerId);
        when(userRepository.getMemberById(managerId)).thenReturn(managerMember);
        when(userRepository.getMemberById(memberId)).thenReturn(member);

        // Mock exact Exception from MembershipDomainService
        doThrow(new Exception("Only Owners and Founders can appoint others."))
            .when(domainService).validateAssignmentRequest(any(), any());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.requestManagerAssignment(managerToken, companyId, memberId, permissions);
        });

        assertEquals("Only Owners and Founders can appoint others.", exception.getMessage());
    }

    // =========================================================================================
    // Tests for: approveAssignment
    // =========================================================================================

    @Test
    public void GivenPendingRole_WhenApproveAssignment_ThenStatusBecomesActive() throws Exception {
        // Arrange
        Manager pendingRole = new Manager(companyId, founderId, new HashSet<>());
        pendingRole.setStatus(RoleStatus.PENDING);
        when(member.getRoleInCompany(companyId)).thenReturn(pendingRole);

        when(tokenService.validateToken(appointeeToken)).thenReturn(true);
        when(tokenService.extractUserId(appointeeToken)).thenReturn(memberId);
        when(userRepository.getMemberById(memberId)).thenReturn(member);
        when(userRepository.getMemberById(founderId)).thenReturn(founderMember);
        when(companyRepository.findById(companyId)).thenReturn(testCompany);

        // Act
        String approveResult = membershipService.approveAssignment(appointeeToken, companyId);

        // Assert
        assertEquals("Assignment approved successfully.", approveResult);
        assertEquals(RoleStatus.ACTIVE, pendingRole.getStatus());
        verify(domainService, times(1)).validateApproveAssignment(any(), any(), eq(memberId));
        verify(userRepository, times(1)).updateMember(member);
        verify(testCompany, times(1)).registerNewAppointment(founderId, memberId);
    }

    @Test
    public void GivenRoleAlreadyActive_WhenApproveAssignment_ThenThrowsException() throws Exception {
        // Arrange
        when(tokenService.validateToken(appointeeToken)).thenReturn(true);
        when(tokenService.extractUserId(appointeeToken)).thenReturn(managerId);
        
        // Mock retrieving the appointee
        when(userRepository.getMemberById(managerId)).thenReturn(managerMember); 

        // Mock retrieving the appointer
        when(userRepository.getMemberById(founderId)).thenReturn(founderMember);
        
        // Mock the exact Exception we expect from the MembershipDomainService
        doThrow(new Exception("This role is already active."))
            .when(domainService).validateApproveAssignment(any(), any(), any());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.approveAssignment(appointeeToken, companyId);
        });

        // Now the exception message will match exactly because the service didn't crash early
        assertEquals("This role is already active.", exception.getMessage());
    }

    // =========================================================================================
    // Tests for: rejectAssignment
    // =========================================================================================

    @Test
    public void GivenPendingRole_WhenRejectAssignment_ThenRoleIsRemoved() throws Exception {        
        // Arrange
        Manager pendingRole = new Manager(companyId, founderId, new HashSet<>());
        pendingRole.setStatus(RoleStatus.PENDING);
        when(member.getRoleInCompany(companyId)).thenReturn(pendingRole);

        when(tokenService.validateToken(appointeeToken)).thenReturn(true);
        when(tokenService.extractUserId(appointeeToken)).thenReturn(memberId);
        when(userRepository.getMemberById(memberId)).thenReturn(member);
        when(userRepository.getMemberById(founderId)).thenReturn(founderMember);

        // Act
        String rejectResult = membershipService.rejectAssignment(appointeeToken, companyId); 

        // Assert
        assertEquals("Assignment rejected successfully.", rejectResult);
        verify(domainService, times(1)).validateRejectAssignment(any());
        verify(member, times(1)).deleteRoleInCompany(companyId);
        verify(userRepository, times(1)).updateMember(member);
        verify(userRepository, times(1)).updateMember(founderMember); // Appointer updated
    }

    @Test
    public void GivenRoleAlreadyActive_WhenRejectAssignment_ThenThrowsException() throws Exception {        
        // Arrange
        when(tokenService.validateToken(appointeeToken)).thenReturn(true);
        when(tokenService.extractUserId(appointeeToken)).thenReturn(managerId);
        when(userRepository.getMemberById(managerId)).thenReturn(managerMember); // Already ACTIVE

        // Mock exact Exception from MembershipDomainService
        doThrow(new Exception("This role is already active and cannot be rejected."))
            .when(domainService).validateRejectAssignment(any());

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            membershipService.rejectAssignment(appointeeToken, companyId); 
        });

        assertEquals("This role is already active and cannot be rejected.", exception.getMessage());
    }
}