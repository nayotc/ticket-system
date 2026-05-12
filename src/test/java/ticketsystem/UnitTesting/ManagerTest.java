package ticketsystem.UnitTesting;
import ticketsystem.DomainLayer.user.Manager;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ManagerTest {

    private Manager manager;
    private final Long companyId = 1L;
    private final Long appointerId = 50L;

    @BeforeEach
    void setUp() {
        // Arrange: Initialize a Manager with specific permissions
        Set<Permission> initialPermissions = new HashSet<>();
        initialPermissions.add(Permission.MANAGE_INQUIRIES);
        manager = new Manager(companyId, appointerId, initialPermissions);
    }

    @Test
    void GivenPendingManager_WhenHasPermission_ThenReturnFalse() {
        // Act & Assert: PENDING managers should not have permission access yet
        assertFalse(manager.hasPermission(Permission.MANAGE_INQUIRIES));
    }

    @Test
    void GivenManager_WhenSetAppointer_ThenAppointerIsUpdated() {
        // Act: Change the appointer
        manager.setAppointer(99L);

        // Assert: The appointer ID should be updated
        assertEquals(99L, manager.getAppointedByMemberId());
    }

    @Test
    void GivenManager_WhenAddPermission_ThenPermissionIsAdded() {
        // Arrange: Assume role is activated for permission check
        manager.setStatus(RoleStatus.ACTIVE);

        // Act: Add a new permission
        manager.addPermission(Permission.CONFIGURE_HALL_AND_MAP);

        // Assert: The new permission should be present
        assertTrue(manager.hasPermission(Permission.CONFIGURE_HALL_AND_MAP));
    }

    @Test
    void GivenManager_WhenSetPermissions_ThenOldPermissionsAreReplaced() {
        // Arrange: Create a new set of permissions
        Set<Permission> newPermissions = new HashSet<>();
        newPermissions.add(Permission.VIEW_PURCHASE_HISTORY);
        manager.setStatus(RoleStatus.ACTIVE);

        // Act: Set the new permissions
        manager.setPermissions(newPermissions);

        // Assert: Old permission is gone, new is present
        assertFalse(manager.hasPermission(Permission.MANAGE_INQUIRIES));
        assertTrue(manager.hasPermission(Permission.VIEW_PURCHASE_HISTORY));
    }
}