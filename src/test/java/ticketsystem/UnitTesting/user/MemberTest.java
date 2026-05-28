package ticketsystem.UnitTesting.user;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Manager;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.CompanyRole;

import java.time.LocalDateTime;
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
    @Test
void GivenMember_WhenSetUserNameFullNameAndPhone_ThenValuesAreUpdated() {
    // Act
    member.setUserName("new_user");
    member.setFullName("New Name");
    member.setPhone("0522222222");

    // Assert
    assertEquals("new_user", member.getUserName());
    assertEquals("New Name", member.getFullName());
    assertEquals("0522222222", member.getPhone());
}

@Test
void GivenMember_WhenSetVersion_ThenVersionIsUpdated() {
    // Act
    member.setVersion(5L);

    // Assert
    assertEquals(5L, member.getVersion());
}

@Test
void GivenMemberWithoutRole_WhenHasPermission_ThenReturnFalse() {
    // Act & Assert
    assertFalse(member.hasPermission(companyId, Permission.MANAGE_INQUIRIES));
}

@Test
void GivenManagerRoleWithPermission_WhenHasPermission_ThenReturnTrue() {
    // Arrange
    Set<Permission> permissions = new HashSet<>();
    permissions.add(Permission.MANAGE_INQUIRIES);
    member.addManagerRole(companyId, 50L, permissions);
    member.getRoleInCompany(companyId).activate();

    // Act & Assert
    assertTrue(member.hasPermission(companyId, Permission.MANAGE_INQUIRIES));
}

@Test
void GivenManagerRoleWithoutPermission_WhenHasPermission_ThenReturnFalse() {
    // Arrange
    Set<Permission> permissions = new HashSet<>();
    permissions.add(Permission.MANAGE_EVENT_INVENTORY);
    member.addManagerRole(companyId, 50L, permissions);

    // Act & Assert
    assertFalse(member.hasPermission(companyId, Permission.MANAGE_INQUIRIES));
}

@Test
void GivenMember_WhenAddOwnerRole_ThenRoleIsAdded() {
    // Act
    boolean result = member.addOwnerRole(companyId, 50L);

    // Assert
    assertTrue(result);
    assertNotNull(member.getRoleInCompany(companyId));
}

@Test
void GivenMember_WhenAddManagerRole_ThenRoleIsAdded() {
    // Arrange
    Set<Permission> permissions = new HashSet<>();
    permissions.add(Permission.MANAGE_INQUIRIES);

    // Act
    boolean result = member.addManagerRole(companyId, 50L, permissions);

    // Assert
    assertTrue(result);
    assertNotNull(member.getRoleInCompany(companyId));
    assertTrue(member.getRoleInCompany(companyId) instanceof Manager);
}

@Test
void GivenMemberWithExistingRole_WhenAddAnotherRoleToSameCompany_ThenRoleIsNotAdded() {
    // Arrange
    member.addFounderRole(companyId);

    // Act
    boolean result = member.addOwnerRole(companyId, 50L);

    // Assert
    assertFalse(result);
    assertTrue(member.getRoleInCompany(companyId) instanceof Founder);
}

@Test
void GivenMemberWithRoles_WhenGetAllRoles_ThenReturnAllRoles() {
    // Arrange
    member.addFounderRole(1L);
    member.addOwnerRole(2L, 50L);

    // Act & Assert
    assertEquals(2, member.getAllRoles().size());
}

@Test
void GivenMemberWithoutRole_WhenDeleteRoleInCompany_ThenReturnFalse() {
    // Act
    boolean result = member.deleteRoleInCompany(companyId);

    // Assert
    assertFalse(result);
}

@Test
void GivenOwnerRole_WhenUpdateManagerPermissions_ThenRoleDoesNotChange() {
    // Arrange
    member.addOwnerRole(companyId, 50L);

    Set<Permission> newPermissions = new HashSet<>();
    newPermissions.add(Permission.MANAGE_EVENT_INVENTORY);

    // Act
    member.updateManagerPermissions(companyId, newPermissions);

    // Assert
    assertFalse(member.getRoleInCompany(companyId) instanceof Manager);
}

@Test
void GivenMemberWithoutManagerRole_WhenUpdateManagerPermissions_ThenNoExceptionIsThrown() {
    // Arrange
    Set<Permission> newPermissions = new HashSet<>();
    newPermissions.add(Permission.MANAGE_EVENT_INVENTORY);

    // Act & Assert
    assertDoesNotThrow(() ->
            member.updateManagerPermissions(companyId, newPermissions)
    );
}

@Test
void GivenMember_WhenSuspendMember_ThenMemberIsSuspended() {
    // Arrange
    LocalDateTime startDate = LocalDateTime.now().minusDays(1);
    LocalDateTime endDate = LocalDateTime.now().plusDays(1);

    // Act
    member.suspendMember(1L, startDate, endDate, "Violation");

    // Assert
    assertTrue(member.isSuspended());
    assertNotNull(member.getSuspension());
}

@Test
void GivenSuspendedMember_WhenSuspendAgain_ThenExceptionIsThrown() {
    // Arrange
    LocalDateTime startDate = LocalDateTime.now().minusDays(1);
    LocalDateTime endDate = LocalDateTime.now().plusDays(1);
    member.suspendMember(1L, startDate, endDate, "Violation");

    // Act & Assert
    assertThrows(IllegalStateException.class, () ->
            member.suspendMember(2L, startDate, endDate, "Another violation")
    );
}

@Test
void GivenSuspendedMember_WhenRevokeSuspension_ThenMemberIsNotSuspended() {
    // Arrange
    LocalDateTime startDate = LocalDateTime.now().minusDays(1);
    LocalDateTime endDate = LocalDateTime.now().plusDays(1);
    member.suspendMember(1L, startDate, endDate, "Violation");

    // Act
    member.revokeSuspension();

    // Assert
    assertFalse(member.isSuspended());
    assertTrue(member.getSuspension().isRevoked());
}

@Test
void GivenMemberWithoutSuspension_WhenRevokeSuspension_ThenExceptionIsThrown() {
    // Act & Assert
    assertThrows(IllegalStateException.class, () ->
            member.revokeSuspension()
    );
}

@Test
void GivenMemberWithExpiredSuspension_WhenRevokeSuspension_ThenExceptionIsThrown() {
    // Arrange
    LocalDateTime startDate = LocalDateTime.now().minusDays(2);
    LocalDateTime endDate = LocalDateTime.now().minusDays(1);
    member.suspendMember(1L, startDate, endDate, "Expired violation");

    // Act & Assert
    assertThrows(IllegalStateException.class, () ->
            member.revokeSuspension()
    );
}

@Test
void GivenMemberWithRole_WhenCopyConstructor_ThenCopiedMemberHasSameDataAndRole() {
    // Arrange
    member.addFounderRole(companyId);

    // Act
    Member copiedMember = new Member(member);

    // Assert
    assertEquals(member.getId(), copiedMember.getId());
    assertEquals(member.getUserName(), copiedMember.getUserName());
    assertEquals(member.getFullName(), copiedMember.getFullName());
    assertEquals(member.getPhone(), copiedMember.getPhone());
    assertNotNull(copiedMember.getRoleInCompany(companyId));
    assertTrue(copiedMember.getRoleInCompany(companyId) instanceof Founder);
}
}