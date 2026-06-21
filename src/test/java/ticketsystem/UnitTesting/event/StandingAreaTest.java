package ticketsystem.UnitTesting.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.StandingArea;


class StandingAreaTest {

    private Pair<Integer, Integer> pair(int first, int second) {
        return new Pair<>(first, second);
    }

    private StandingArea standingArea(long capacity) {
        return new StandingArea( "Standing", pair(0, 0), pair(10, 10), capacity);
    }

    @Test
    void GivenAvailableCapacity_WhenReserveSpot_ThenIncreaseReserved() {
        StandingArea area = standingArea(10);

        area.reserveSpot(4);

        assertEquals(4, area.getReserved());
        assertEquals(0, area.getSold());
    }

    @Test
    void GivenQuantityEqualsFreeCapacity_WhenReserveSpot_ThenReserveSuccessfully() {
        StandingArea area = standingArea(10);

        area.reserveSpot(10);

        assertEquals(10, area.getReserved());
    }

    @Test
    void GivenQuantityAboveFreeCapacity_WhenReserveSpot_ThenThrowException() {
        StandingArea area = standingArea(10);
        area.reserveSpot(7);

        assertThrows(IllegalStateException.class, () -> area.reserveSpot(4));
    }

    @Test
    void GivenReservedSpots_WhenReleaseSpot_ThenDecreaseReserved() {
        StandingArea area = standingArea(10);
        area.reserveSpot(5);

        area.releaseSpot(2);

        assertEquals(3, area.getReserved());
    }

    @Test
    void GivenNotEnoughReservedSpots_WhenReleaseSpot_ThenThrowException() {
        StandingArea area = standingArea(10);
        area.reserveSpot(2);

        assertThrows(IllegalStateException.class, () -> area.releaseSpot(3));
    }

    @Test
    void GivenReservedSpots_WhenSellSpot_ThenMoveFromReservedToSold() {
        StandingArea area = standingArea(10);
        area.reserveSpot(5);

        area.sellSpot(3);

        assertEquals(2, area.getReserved());
        assertEquals(3, area.getSold());
    }

    @Test
    void GivenNoReservedSpots_WhenSellSpot_ThenThrowException() {
        StandingArea area = standingArea(10);

        assertThrows(IllegalStateException.class, () -> area.sellSpot(1));
    }

    @Test
    void GivenSoldLessThanCapacity_WhenIsSoldOut_ThenReturnFalse() {
        StandingArea area = standingArea(10);
        area.reserveSpot(5);
        area.sellSpot(5);

        assertFalse(area.isSoldOut());
    }

    @Test
    void GivenSoldEqualsCapacity_WhenIsSoldOut_ThenReturnTrue() {
        StandingArea area = standingArea(10);
        area.setSold(10);

        assertTrue(area.isSoldOut());
    }

    @Test
    void GivenStandingArea_WhenCopy_ThenCopyHasSameStateAndIsIndependent() {
        StandingArea original = standingArea(10);
        original.reserveSpot(5);
        original.sellSpot(3);

        StandingArea copy = original.copy();
        copy.releaseSpot(2);

        assertNotSame(original, copy);
        assertEquals(2, original.getReserved());
        assertEquals(3, original.getSold());
        assertEquals(0, copy.getReserved());
        assertEquals(3, copy.getSold());
    }
}

