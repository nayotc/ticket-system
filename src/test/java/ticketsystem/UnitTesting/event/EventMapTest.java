package ticketsystem.UnitTesting.event;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.StandingArea;
import ticketsystem.DomainLayer.event.Element;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SeatPosition;

import static org.junit.jupiter.api.Assertions.*;

class EventMapTest {

    private Pair<Integer, Integer> pair(int first, int second) {
        return new Pair<>(first, second);
    }

    private SeatPosition position(int row, int number) {
        return new SeatPosition(row, number);
    }

    private EventMap eventMap() {
        return new EventMap(pair(100, 100));
    }

    private SeatingArea seatingArea( int rows, int columns) {
        SeatingArea area = new SeatingArea( "Seating", pair(0, 0), pair(10, 10), rows, columns);
        setElementId(area, 1L);
        return area;
    }

    private StandingArea standingArea(long capacity) {
        StandingArea area = new StandingArea( "Standing", pair(20, 20), pair(10, 10), capacity);
        setElementId(area, 2L);
        return area;
    }

    private void setElementId(Element element, Long id) {
        try {
            Field idField = Element.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(element, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to set generated element ID in test",
                    exception
            );
        }
    }

    @Test
    void GivenElement_WhenAddElement_ThenElementIsAddedToMap() {
        EventMap map = eventMap();
        Element stage = new Element( "Stage", pair(5, 5), pair(20, 10));

        map.addElement(stage);

        assertTrue(map.getElements().contains(stage));
    }

    @Test
    void GivenExistingElement_WhenRemoveElement_ThenElementIsRemovedFromMap() {
        EventMap map = eventMap();
        Element stage = new Element("Stage", pair(5, 5), pair(20, 10));
        map.addElement(stage);

        map.removeElement(stage);

        assertFalse(map.getElements().contains(stage));
    }

    @Test
    void GivenSeatingAreaExists_WhenReserveSeat_ThenSeatStatusIsReserved() {
        EventMap map = eventMap();
        SeatingArea seatingArea = seatingArea(2, 2);
        map.addElement(seatingArea);

        map.reserveSeat(seatingArea.getId(), position(1, 1));

        assertEquals(SeatStatus.RESERVED, map.isSeatAvailable(seatingArea.getId(), position(1, 1)));
    }

    @Test
    void GivenReservedSeat_WhenReleaseSeat_ThenSeatStatusIsAvailable() {
        EventMap map = eventMap();
        SeatingArea seatingArea = seatingArea(2, 2);
        map.addElement(seatingArea);
        map.reserveSeat(seatingArea.getId(), position(1, 1));

        map.releaseSeat(seatingArea.getId(), position(1, 1));

        assertEquals(SeatStatus.AVAILABLE, map.isSeatAvailable(seatingArea.getId(), position(1, 1)));
    }

    @Test
    void GivenSeatingAreaExists_WhenSellSeat_ThenSeatStatusIsSold() {
        EventMap map = eventMap();
        SeatingArea seatingArea = seatingArea(2, 2);
        map.addElement(seatingArea);

        map.sellSeat(seatingArea.getId(), position(1, 1));

        assertEquals(SeatStatus.SOLD, map.isSeatAvailable(seatingArea.getId(), position(1, 1)));
    }

    @Test
    void GivenMissingSeatingArea_WhenReserveSeat_ThenThrowException() {
        EventMap map = eventMap();
        SeatingArea seatingArea = seatingArea(2, 2);
        map.addElement(seatingArea);

        assertThrows(IllegalArgumentException.class, () -> map.reserveSeat(99L, position(1, 1)));
    }

    @Test
    void GivenStandingAreaExists_WhenReserveSpot_ThenIncreaseReserved() {
        EventMap map = eventMap();
        StandingArea standingArea = standingArea( 10);
        map.addElement(standingArea);

        map.reserveSpot(standingArea.getId(), 4);

        assertEquals(4, standingArea.getReserved());
    }

    @Test
    void GivenReservedSpots_WhenReleaseSpot_ThenDecreaseReserved() {
        EventMap map = eventMap();
        StandingArea standingArea = standingArea( 10);
        map.addElement(standingArea);
        map.reserveSpot(standingArea.getId(), 4);

        map.releaseSpot(standingArea.getId(), 2);

        assertEquals(2, standingArea.getReserved());
    }

    @Test
    void GivenReservedSpots_WhenSellSpot_ThenIncreaseSold() {
        EventMap map = eventMap();
        StandingArea standingArea = standingArea( 10);
        map.addElement(standingArea);
        map.reserveSpot(standingArea.getId(), 4);

        map.sellSpot(standingArea.getId(), 3);

        assertEquals(1, standingArea.getReserved());
        assertEquals(3, standingArea.getSold());
    }

    @Test
    void GivenMissingStandingArea_WhenReserveSpot_ThenThrowException() {
        EventMap map = eventMap();
        map.addElement(standingArea( 10));

        assertThrows(IllegalArgumentException.class, () -> map.reserveSpot(99L, 1));
    }

    @Test
    void GivenEmptyMap_WhenIsSoldOut_ThenReturnFalse() {
        EventMap map = eventMap();

        assertFalse(map.isSoldOut());
    }

    @Test
    void GivenMapWithNoAreaElements_WhenIsSoldOut_ThenReturnFalse() {
        EventMap map = eventMap();
        map.addElement(new Element("Stage", pair(5, 5), pair(20, 10)));

        assertFalse(map.isSoldOut());
    }

    @Test
    void GivenAtLeastOneAreaNotSoldOut_WhenIsSoldOut_ThenReturnFalse() {
        EventMap map = eventMap();
        SeatingArea seatingArea = seatingArea( 1, 1);
        StandingArea standingArea = standingArea( 10);
        standingArea.setSold(10);
        map.addElement(seatingArea);
        map.addElement(standingArea);

        assertFalse(map.isSoldOut());
    }

    @Test
    void GivenAllAreasSoldOut_WhenIsSoldOut_ThenReturnTrue() {
        EventMap map = eventMap();
        SeatingArea seatingArea = seatingArea(1, 1);
        seatingArea.sellSeat(position(1, 1));
        StandingArea standingArea = standingArea( 10);
        standingArea.setSold(10);
        map.addElement(seatingArea);
        map.addElement(standingArea);

        assertTrue(map.isSoldOut());
    }

    @Test
    void GivenEventMap_WhenCopy_ThenAreasAreCopiedAndIndependent() {
        EventMap original = eventMap();
        SeatingArea seatingArea = seatingArea( 1, 1);
        original.addElement(seatingArea);
        original.reserveSeat(seatingArea.getId(), position(1, 1));

        EventMap copy = new EventMap(original);
        copy.sellSeat(seatingArea.getId(), position(1, 1));

        assertNotSame(original, copy);
        assertNotSame(original.getElements(), copy.getElements());
        assertNotSame(original.getElements().get(0), copy.getElements().get(0));
        assertEquals(SeatStatus.RESERVED, original.isSeatAvailable(seatingArea.getId(), position(1, 1)));
        assertEquals(SeatStatus.SOLD, copy.isSeatAvailable(seatingArea.getId(), position(1, 1)));
    }
}
