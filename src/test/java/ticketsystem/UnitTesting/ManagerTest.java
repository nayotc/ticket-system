package ticketsystem.UnitTesting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ticketsystem.DomainLayer.user.Manager;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ManagerTest {

    private Manager manager;
    private Member memberMock;
    private Set<Permission> initialPermissions;
    private final Long companyId = 1L;
    private final Long appointedById = 75L;

    @BeforeEach
    public void setUp() {
        memberMock = mock(Member.class);
        initialPermissions = new HashSet<>();
        initialPermissions.add(Permission.MANAGE_INQUIRIES);
        manager = new Manager(memberMock, companyId, initialPermissions, appointedById);
    }

    @Test
    public void GivenNewManager_WhenGetStatus_ThenReturnPending() {
        assertEquals(RoleStatus.PENDING, manager.getStatus());
    }

    @Test
    public void GivenManager_WhenActivate_ThenStatusBecomesActive() {
        manager.activate();

        assertEquals(RoleStatus.ACTIVE, manager.getStatus());
    }

    @Test
    public void GivenPendingManager_WhenHasPermission_ThenReturnFalse() {
        // Should be false even if the permission is in the set because status is PENDING
        assertFalse(manager.hasPermission(Permission.MANAGE_INQUIRIES));
    }

    @Test
    public void GivenActiveManagerWithPermission_WhenHasPermission_ThenReturnTrue() {
        manager.activate();

        assertTrue(manager.hasPermission(Permission.MANAGE_INQUIRIES));
    }

    @Test
    public void GivenActiveManagerWithoutPermission_WhenHasPermission_ThenReturnFalse() {
        manager.activate();

        assertFalse(manager.hasPermission(Permission.SET_PURCHASING_POLICY));
    }

    @Test
    public void GivenManager_WhenAddPermission_ThenPermissionIsAdded() {
        manager.activate();

        manager.addPermission(Permission.VIEW_PURCHASE_HISTORY);

        assertTrue(manager.hasPermission(Permission.VIEW_PURCHASE_HISTORY));
    }

    @Test
    public void GivenManager_WhenDeletePermission_ThenPermissionIsRemoved() {
        manager.activate();

        manager.deletePermission(Permission.MANAGE_INQUIRIES);

        assertFalse(manager.hasPermission(Permission.MANAGE_INQUIRIES));
    }

    @Test
    public void GivenManagerWithPermissions_WhenGetPermissionKeys_ThenReturnKeysSet() {
        manager.addPermission(Permission.CONFIGURE_HALL_AND_MAP);

        Set<String> keys = manager.getPermissionKeys();

        assertEquals(2, keys.size());
        assertTrue(keys.contains("inquiry:response:manage"));
        assertTrue(keys.contains("hall:config:setup"));
    }
}
