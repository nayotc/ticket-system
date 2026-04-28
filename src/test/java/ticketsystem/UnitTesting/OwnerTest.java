package ticketsystem.UnitTesting;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Owner;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OwnerTest {

    private Owner owner;
    private Member memberMock;
    private final long companyId = 1L;
    private final Long appointedById = 50L;

    @BeforeEach
    public void setUp() {
        memberMock = mock(Member.class);
        owner = new Owner(memberMock, companyId, appointedById);
    }

    @Test
    public void GivenNewOwner_WhenGetStatus_ThenReturnPending() {
        assertEquals(RoleStatus.PENDING, owner.getStatus());
    }

    @Test
    public void GivenOwner_WhenActivate_ThenStatusBecomesActive() {
        owner.activate();

        assertEquals(RoleStatus.ACTIVE, owner.getStatus());
    }

    @Test
    public void GivenPendingOwner_WhenHasPermission_ThenReturnFalse() {
        assertFalse(owner.hasPermission(Permission.MANAGE_EVENT_INVENTORY));
    }

    @Test
    public void GivenActiveOwner_WhenHasPermission_ThenReturnTrueForAllPermissions() {
        owner.activate();

        assertTrue(owner.hasPermission(Permission.MANAGE_EVENT_INVENTORY));
        assertTrue(owner.hasPermission(Permission.GENERATE_SALES_REPORT));
    }

    @Test
    public void GivenOwner_WhenGetAppointedByMemberId_ThenReturnCorrectId() {
        assertEquals(appointedById, owner.getAppointedByMemberId());
    }

    @Test
    public void GivenOwner_WhenAddAppointee_ThenAppointeeIdIsStored() {
        owner.addAppointee(300L);

        assertTrue(owner.getAppointeesMemberIds().contains(300L));
        assertEquals(1, owner.getAppointeesMemberIds().size());
    }

    @Test
    public void GivenOwnerWithAppointee_WhenDeleteAppointee_ThenAppointeeIdIsRemoved() {
        owner.addAppointee(300L);

        owner.deleteAppointee(300L);

        assertFalse(owner.getAppointeesMemberIds().contains(300L));
        assertEquals(0, owner.getAppointeesMemberIds().size());
    }
}
