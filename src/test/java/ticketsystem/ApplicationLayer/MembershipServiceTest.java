package ticketsystem.ApplicationLayer;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.*;
import ticketsystem.DomainLayer.company.Company;

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
        Founder founderRole = new Founder(companyId);
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
    
@Test
    public void GivenMemberAndAcceptsRequest_WhenAssignManager_ThenMemberBecomesManager() throws Exception {
        // Arrange
        String token = "valid-token";
        Set<Permission> permissions = new HashSet<>();
        permissions.add(Permission.MANAGE_EVENT_INVENTORY);
        
        // 1. Extract IDs to normal variables to avoid confusing Mockito with nested mock calls
        long founderId = 100L; 
        long plainMemberId = 103L; 
        
        // 2. Safe stubbing using the extracted variables
        when(tokenService.extractUserId(token)).thenReturn(founderId);
        when(userRepository.getMemberById(founderId)).thenReturn(founderMember);
        when(userRepository.getMemberById(plainMemberId)).thenReturn(member);
        when(companyRepository.findById(companyId)).thenReturn(testCompany);

        // Act
        membershipService.requestManagerAssignment(token, companyId, plainMemberId, permissions);

        // Assert
        // Verify that the domain service was called to execute the validation logic
        verify(domainService, times(1)).validateManagerAssignmentRequest(any(), any());
        
        // Verify that the user was saved to the repository with the changes
        verify(userRepository, times(1)).updateMember(member);
        
        // Verify that a notification was sent to the user regarding the new assignment
        // verify(notificationService, times(1)).sendNotification(eq(plainMemberId), anyString());
    }

    @Test
    public void GivenMemberAndRejectsRequest_WhenAssignManager_ThenMemberIsNotManager() {
        System.out.println("not implemented yet");
    }

    @Test
    public void GivenMemberAlreadyHasRole_WhenAssignManager_ThenAssignmentFails() {
        System.out.println("not implemented yet");
    }


}
