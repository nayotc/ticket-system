package ticketsystem.UnitTesting.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ticketsystem.DomainLayer.user.CompanyRole;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;

import static org.junit.jupiter.api.Assertions.*;

public class CompanyRoleTest {

    private CompanyRole role;

    @BeforeEach
    void setUp() {
        role = new DummyCompanyRole(1L);
    }

    @Test
    void GivenNewRole_WhenCreated_ThenStatusIsPending() {
        // Act & Assert
        assertEquals(RoleStatus.PENDING, role.getStatus());
        assertTrue(role.isPending());
        assertFalse(role.isActive());
        assertFalse(role.isCancelled());
    }

    @Test
    void GivenRole_WhenGetCompanyId_ThenReturnCorrectId() {
        // Act & Assert
        assertEquals(1L, role.getCompanyId());
    }

    @Test
    void GivenPendingRole_WhenActivate_ThenStatusBecomesActive() {
        // Act
        role.activate();

        // Assert
        assertEquals(RoleStatus.ACTIVE, role.getStatus());
        assertTrue(role.isActive());
        assertFalse(role.isPending());
    }

    @Test
    void GivenRole_WhenSetStatus_ThenStatusChanges() {
        // Act
        role.setStatus(RoleStatus.CANCELLED);

        // Assert
        assertEquals(RoleStatus.CANCELLED, role.getStatus());
    }

    @Test
    void GivenRole_WhenCancel_ThenStatusBecomesCancelled() {
        // Act
        role.cancel();

        // Assert
        assertEquals(RoleStatus.CANCELLED, role.getStatus());
        assertTrue(role.isCancelled());
        assertFalse(role.isActive());
        assertFalse(role.isPending());
    }

    @Test
    void GivenCancelledRole_WhenActivate_ThenStatusBecomesActive() {
        // Arrange
        role.cancel();

        // Act
        role.activate();

        // Assert
        assertEquals(RoleStatus.ACTIVE, role.getStatus());
        assertTrue(role.isActive());
    }

    /**
     * Dummy implementation for testing abstract class
     */
    private static class DummyCompanyRole extends CompanyRole {

        public DummyCompanyRole(Long companyId) {
            super(companyId);
        }

        @Override
        public boolean hasPermission(Permission permission) {
            return false;
        }
    }
}
