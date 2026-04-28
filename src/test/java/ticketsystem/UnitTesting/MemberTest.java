package ticketsystem.UnitTesting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Manager;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.User;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MemberTest {

    private Member member;
    private final long memberId = 100L;
    private final long companyId = 1L;

    @BeforeEach
    public void setUp() {
        User userMock = mock(User.class);
        member = new Member(userMock, memberId, "testUser", "password123");
    }

    @Test
    public void GivenNewMember_WhenGetProperties_ThenReturnCorrectValues() {
        assertEquals(memberId, member.getId());
        assertEquals("testUser", member.getUserName());
        assertEquals("password123", member.getPassword());
    }

    @Test
    public void GivenMember_WhenAddAndGetRole_ThenReturnAddedRole() {
        CompanyRole roleMock = mock(CompanyRole.class);
        when(roleMock.getCompanyId()).thenReturn(companyId);
        
        member.addRole(roleMock);

        assertEquals(roleMock, member.getRole(companyId));
    }

    @Test
    public void GivenMemberWithRole_WhenDeleteRole_ThenRoleIsRemoved() {
        CompanyRole roleMock = mock(CompanyRole.class);
        when(roleMock.getCompanyId()).thenReturn(companyId);
        member.addRole(roleMock);

        member.deleteRole(companyId);

        assertNull(member.getRole(companyId));
    }

    @Test
    public void GivenMemberWithManagerRole_WhenActivateRole_ThenManagerIsActivated() {
        Manager managerMock = mock(Manager.class);
        when(managerMock.getCompanyId()).thenReturn(companyId);
        member.addRole(managerMock);

        member.activateRole(companyId);

        verify(managerMock, times(1)).activate();
    }

    @Test
    public void GivenMemberWithOwnerRole_WhenActivateRole_ThenOwnerIsActivated() {
        Owner ownerMock = mock(Owner.class);
        when(ownerMock.getCompanyId()).thenReturn(companyId);
        member.addRole(ownerMock);

        member.activateRole(companyId);

        verify(ownerMock, times(1)).activate();
    }

    @Test
    public void GivenMemberWithRole_WhenRejectRole_ThenRoleIsRemoved() {
        CompanyRole roleMock = mock(CompanyRole.class);
        when(roleMock.getCompanyId()).thenReturn(companyId);
        member.addRole(roleMock);

        member.rejectRole(companyId);

        assertNull(member.getRole(companyId));
    }
}
