package ticketsystem.UnitTesting.user;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Permission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FounderTest {

    private Founder founder;
    private final Long companyId = 1L;

    @BeforeEach
    void setUp() {
        // Arrange: Initialize a new Founder
        founder = new Founder(companyId);
    }

    @Test
    void GivenNewFounder_WhenHasPermission_ThenReturnTrueForAll() {
        // Act & Assert: Founder is active immediately and has all permissions
        assertTrue(founder.hasPermission(Permission.MANAGE_EVENT_INVENTORY));
        assertTrue(founder.hasPermission(Permission.SET_DISCOUNT_POLICY));
    }

    @Test
    void GivenFounder_WhenAddAppointee_ThenAppointeeIsStored() {
        // Act: Add an appointee
        founder.addAppointee(100L);

        // Assert: The appointee ID should be in the list
        assertTrue(founder.getAppointeesMemberIds().contains(100L));
        assertEquals(1, founder.getAppointeesMemberIds().size());
    }

    @Test
    void GivenFounderWithAppointee_WhenDeleteAppointee_ThenAppointeeIsRemoved() {
        // Arrange: Add an appointee first
        founder.addAppointee(100L);

        // Act: Remove the appointee
        founder.deleteAppointee(100L);

        // Assert: The list should be empty
        assertFalse(founder.getAppointeesMemberIds().contains(100L));
        assertEquals(0, founder.getAppointeesMemberIds().size());
    }
}
