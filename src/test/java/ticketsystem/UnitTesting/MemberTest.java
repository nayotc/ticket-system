package ticketsystem.UnitTesting;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Manager;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.CompanyRole;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MemberTest {

    private Member member;
    private final Long memberId = 100L;
    private final Long companyId = 1L;

    @BeforeEach
    void setUp() {
        // Arrange: Initialize a new Member
        member = new Member(memberId, "john_doe", "John Doe", "0500000000");
    }

    @Test
    void GivenNewMember_WhenGetIdAndName_ThenReturnCorrectValues() {
        // Act & Assert: Verify initialization
        assertEquals(memberId, member.getId());
        assertEquals("john_doe", member.getUserName());
    }

    @Test
    void GivenMember_WhenAddFounderRole_ThenRoleIsAdded() {
        // Act: Add a Founder role
        member.addFounderRole(companyId);

        // Assert: The role should exist and be of type Founder
        CompanyRole role = member.getRoleInCompany(companyId);
        assertNotNull(role);
        assertTrue(role instanceof Founder);
    }

    @Test
    void GivenMemberWithRole_WhenDeleteRoleInCompany_ThenRoleIsRemoved() {
        // Arrange: Add an Owner role
        member.addOwnerRole(companyId, 50L);

        // Act: Delete the role
        member.deleteRoleInCompany(companyId);

        // Assert: The role should no longer exist
        assertNull(member.getRoleInCompany(companyId));
    }

    @Test
    void GivenMemberWithManagerRole_WhenUpdateManagerPermissions_ThenPermissionsChange() {
        // Arrange: Add a Manager role with initial permissions
        Set<Permission> initialPerms = new HashSet<>();
        initialPerms.add(Permission.MANAGE_INQUIRIES);
        member.addManagerRole(companyId, 50L, initialPerms);
        
        // Act: Update permissions
        Set<Permission> newPerms = new HashSet<>();
        newPerms.add(Permission.MANAGE_EVENT_INVENTORY);
        member.updateManagerPermissions(companyId, newPerms);

        // Assert: The manager should have the new permissions
        Manager updatedManager = (Manager) member.getRoleInCompany(companyId);
        assertTrue(updatedManager.getPermissionKeys().contains(Permission.MANAGE_EVENT_INVENTORY.getKey()));
        assertFalse(updatedManager.getPermissionKeys().contains(Permission.MANAGE_INQUIRIES.getKey()));
    }
}