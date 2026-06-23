package ticketsystem.UnitTesting.event;

import org.junit.jupiter.api.Test;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SeatPosition;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;

class SeatingAreaTest {

    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("120.00");

    private Pair<Integer, Integer> pair(int first, int second) {
        return new Pair<>(first, second);
    }

    private SeatPosition position(int row, int number) {
        return new SeatPosition(row, number);
    }

    private SeatingArea seatingArea(int rows, int columns) {
        return new SeatingArea( "Main Seating", pair(0, 0), pair(10, 10), rows, columns,DEFAULT_PRICE);
    }

    @Test
    void GivenRowsAndColumns_WhenCreateSeatingArea_ThenCreateAllSeatsAsAvailable() {
        SeatingArea area = seatingArea(2, 3);

        assertEquals(6, area.getSeats().size());
        assertEquals(SeatStatus.AVAILABLE, area.isSeatAvailable(position(1, 1)));
        assertEquals(SeatStatus.AVAILABLE, area.isSeatAvailable(position(2, 3)));
    }

    @Test
    void GivenAvailableSeat_WhenReserveSeat_ThenSeatStatusIsReserved() {
        SeatingArea area = seatingArea(2, 2);

        area.reserveSeat(position(1, 1));

        assertEquals(SeatStatus.RESERVED, area.isSeatAvailable(position(1, 1)));
    }

    @Test
    void GivenReservedSeat_WhenReleaseSeat_ThenSeatStatusIsAvailable() {
        SeatingArea area = seatingArea(2, 2);
        area.reserveSeat(position(1, 1));

        area.releaseSeat(position(1, 1));

        assertEquals(SeatStatus.AVAILABLE, area.isSeatAvailable(position(1, 1)));
    }

    @Test
    void GivenAvailableSeat_WhenSellSeat_ThenSeatStatusIsSold() {
        SeatingArea area = seatingArea(2, 2);

        area.sellSeat(position(1, 1));

        assertEquals(SeatStatus.SOLD, area.isSeatAvailable(position(1, 1)));
    }

    @Test
    void GivenInvalidSeatPosition_WhenReserveSeat_ThenThrowException() {
        SeatingArea area = seatingArea(2, 2);

        assertThrows(IllegalArgumentException.class, () -> area.reserveSeat(position(3, 1)));
    }

    @Test
    void GivenInvalidSeatPosition_WhenReleaseSeat_ThenThrowException() {
        SeatingArea area = seatingArea(2, 2);

        assertThrows(IllegalArgumentException.class, () -> area.releaseSeat(position(3, 1)));
    }

    @Test
    void GivenInvalidSeatPosition_WhenSellSeat_ThenThrowException() {
        SeatingArea area = seatingArea(2, 2);

        assertThrows(IllegalArgumentException.class, () -> area.sellSeat(position(3, 1)));
    }

    @Test
    void GivenInvalidSeatPosition_WhenIsSeatAvailable_ThenThrowException() {
        SeatingArea area = seatingArea(2, 2);

        assertThrows(IllegalArgumentException.class, () -> area.isSeatAvailable(position(3, 1)));
    }

    @Test
    void GivenNotAllSeatsSold_WhenIsSoldOut_ThenReturnFalse() {
        SeatingArea area = seatingArea(1, 2);
        area.sellSeat(position(1, 1));

        assertFalse(area.isSoldOut());
    }

    @Test
    void GivenAllSeatsSold_WhenIsSoldOut_ThenReturnTrue() {
        SeatingArea area = seatingArea(1, 2);
        area.sellSeat(position(1, 1));
        area.sellSeat(position(1, 2));

        assertTrue(area.isSoldOut());
    }

    @Test
    void GivenSeatingArea_WhenCopy_ThenCopyHasSameSeatsAndIsIndependent() {
        SeatingArea original = seatingArea(1, 1);
        original.reserveSeat(position(1, 1));

        SeatingArea copy = original.copy();
        copy.sellSeat(position(1, 1));

        assertNotSame(original, copy);
        assertEquals(SeatStatus.RESERVED, original.isSeatAvailable(position(1, 1)));
        assertEquals(SeatStatus.SOLD, copy.isSeatAvailable(position(1, 1)));
        assertEquals(0, original.getPrice().compareTo(copy.getPrice()));
    }

    @Test
    void GivenValidPrice_WhenCreateSeatingArea_ThenPriceIsStored() {
        SeatingArea area = seatingArea(2, 3);

        assertEquals(
                0,
                DEFAULT_PRICE.compareTo(area.getPrice())
        );
    }

    @Test
    void GivenNegativePrice_WhenCreateSeatingArea_ThenThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SeatingArea(
                        "Main Seating",
                        pair(0, 0),
                        pair(10, 10),
                        2,
                        3,
                        new BigDecimal("-1.00")
                )
        );
    }

    @Test
    void GivenNullPrice_WhenCreateSeatingArea_ThenThrowException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SeatingArea(
                        "Main Seating",
                        pair(0, 0),
                        pair(10, 10),
                        2,
                        3,
                        null
                )
        );
    }

    @Test
    void GivenZeroPrice_WhenCreateSeatingArea_ThenPriceIsAccepted() {
        SeatingArea area = new SeatingArea(
                "Free Seating",
                pair(0, 0),
                pair(10, 10),
                2,
                3,
                BigDecimal.ZERO
        );

        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(area.getPrice())
        );
    }
}
