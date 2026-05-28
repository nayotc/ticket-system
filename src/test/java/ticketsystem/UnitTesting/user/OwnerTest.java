package ticketsystem.UnitTesting.user;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OwnerTest {

    private Owner owner;
    private final Long companyId = 1L;
    private final Long appointerId = 50L;

    @BeforeEach
    void setUp() {
        // Arrange: Initialize a new Owner
        owner = new Owner(companyId, appointerId);
    }

    @Test
    void GivenPendingOwner_WhenHasPermission_ThenReturnFalse() {
        // Act & Assert: PENDING owners should not have permission access yet
        assertFalse(owner.hasPermission(Permission.MANAGE_EVENT_INVENTORY));
    }

    @Test
    void GivenActiveOwner_WhenHasPermission_ThenReturnTrueForAll() {
        // Arrange: Activate the owner
        owner.setStatus(RoleStatus.ACTIVE);

        // Act & Assert: Active owners have all permissions
        assertTrue(owner.hasPermission(Permission.MANAGE_EVENT_INVENTORY));
        assertTrue(owner.hasPermission(Permission.SET_PURCHASING_POLICY));
    }

    @Test
    void GivenOwner_WhenSetAppointer_ThenAppointerIsUpdated() {
        // Act: Update the appointer
        owner.setAppointer(200L);

        // Assert: The new appointer ID should be returned
        assertEquals(200L, owner.getAppointedByMemberId());
    }

    @Test
    void GivenOwnerWithAppointee_WhenDeleteAppointee_ThenAppointeeIsRemoved() {
        // Arrange: Add an appointee
        owner.addAppointee(300L);

        // Act: Remove the appointee
        owner.deleteAppointee(300L);

        // Assert: The appointee list should be empty
        assertFalse(owner.getAppointeesMemberIds().contains(300L));
    }
}