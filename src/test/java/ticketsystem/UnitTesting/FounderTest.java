package ticketsystem.UnitTesting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ticketsystem.DomainLayer.user.Founder;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;
import ticketsystem.DomainLayer.user.RoleStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class FounderTest {

    private Founder founder;
    private Member memberMock;
    private final Long companyId = 1L;

    @BeforeEach
    public void setUp() {
        memberMock = mock(Member.class);
        when(memberMock.getId()).thenReturn(100L);
        founder = new Founder(memberMock, companyId);
    }

    @Test
    public void GivenNewFounder_WhenGetStatus_ThenReturnActive() {
        assertEquals(RoleStatus.ACTIVE, founder.getStatus());
    }

    @Test
    public void GivenNewFounder_WhenHasPermission_ThenReturnTrueForAllPermissions() {
        assertTrue(founder.hasPermission(Permission.MANAGE_EVENT_INVENTORY));
        assertTrue(founder.hasPermission(Permission.SET_DISCOUNT_POLICY));
    }

    @Test
    public void GivenFounder_WhenAddAppointee_ThenAppointeeIdIsStored() {
        founder.addAppointee(200L);

        assertTrue(founder.getAppointeesMemberIds().contains(200L));
        assertEquals(1, founder.getAppointeesMemberIds().size());
    }

    @Test
    public void GivenFounderWithAppointee_WhenDeleteAppointee_ThenAppointeeIdIsRemoved() {
        founder.addAppointee(200L);

        founder.deleteAppointee(200L);

        assertFalse(founder.getAppointeesMemberIds().contains(200L));
        assertEquals(0, founder.getAppointeesMemberIds().size());
    }
}
