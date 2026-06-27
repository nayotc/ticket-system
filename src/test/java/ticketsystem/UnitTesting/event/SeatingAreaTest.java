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
        return new SeatingArea( "Main Seating", pair(0, 0), rows, columns,DEFAULT_PRICE);
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
                2,
                3,
                BigDecimal.ZERO
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(area.getPrice()));
    }

    @Test
    void GivenEvenRowsAndColumns_WhenCreateArea_ThenSizeUsesHalfSeatScaleAndHeader() {
        SeatingArea area =
                new SeatingArea(
                        "Area A",
                        new Pair<>(1, 1),
                        10,
                        20,
                        BigDecimal.TEN
                );

        assertEquals(new Pair<>(10, 6), area.getSize());
        assertEquals(200, area.getSeats().size());
    }

    @Test
    void GivenOddRowsAndColumns_WhenCreateArea_ThenSizeRoundsUp() {
        SeatingArea area =
                new SeatingArea(
                        "Area A",
                        new Pair<>(1, 1),
                        5,
                        7,
                        BigDecimal.TEN
                );

        assertEquals(new Pair<>(4, 4), area.getSize());
        assertEquals(35, area.getSeats().size());
    }

    @Test
    void GivenSmallArea_WhenCreateArea_ThenHeaderMinimumWidthIsUsed() {
        SeatingArea area =
                new SeatingArea(
                        "Area A",
                        new Pair<>(1, 1),
                        1,
                        1,
                        BigDecimal.TEN
                );

        assertEquals(new Pair<>(2, 2), area.getSize());
    }

    @Test
    void GivenLargerDimensions_WhenExpandArea_ThenSizeIsRecalculated() {
        SeatingArea area =
                new SeatingArea(
                        "Area A",
                        new Pair<>(1, 1),
                        4,
                        8,
                        BigDecimal.TEN
                );

        int addedSeats =
                area.expandTo(10, 20);

        assertEquals(168, addedSeats);
        assertEquals(10, area.getRows());
        assertEquals(20, area.getColumns());

        assertEquals(new Pair<>(10, 6), area.getSize());
        assertEquals(200, area.getSeats().size());
    }

    @Test
    void GivenManualSizeChange_WhenSetSize_ThenFail() {
        SeatingArea area =
                new SeatingArea(
                        "Area A",
                        new Pair<>(1, 1),
                        10,
                        20,
                        BigDecimal.TEN
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> area.setSize(
                        new Pair<>(50, 50)
                )
        );
    }

    @Test
    void GivenElevenRowsAndSixteenColumns_WhenCalculateSize_ThenAreaMatchesSeatGrid() {
        SeatingArea area =
                new SeatingArea(
                        "B",
                        new Pair<>(1, 1),
                        11,
                        16,
                        BigDecimal.valueOf(20)
                );

        assertEquals(new Pair<>(8, 7), area.getSize());
    }
}
