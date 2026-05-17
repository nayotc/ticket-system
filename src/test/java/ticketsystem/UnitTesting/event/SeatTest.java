package ticketsystem.UnitTesting.event;

import org.junit.jupiter.api.Test;
import ticketsystem.DomainLayer.event.Seat.SeatStatus;
import ticketsystem.DomainLayer.event.Seat;
import ticketsystem.DomainLayer.event.SeatPosition;

import static org.junit.jupiter.api.Assertions.*;

class SeatTest {

    private SeatPosition position() {
        return new SeatPosition(1, 1);
    }

    @Test
    void GivenNewSeat_WhenCreated_ThenSeatIsAvailable() {
        Seat seat = new Seat(position());

        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
    }

    @Test
    void GivenAvailableSeat_WhenReserve_ThenSeatIsReserved() {
        Seat seat = new Seat(position());

        seat.reserve();

        assertEquals(SeatStatus.RESERVED, seat.getStatus());
    }

    @Test
    void GivenReservedSeat_WhenRelease_ThenSeatIsAvailable() {
        Seat seat = new Seat(position());
        seat.reserve();

        seat.release();

        assertEquals(SeatStatus.AVAILABLE, seat.getStatus());
    }

    @Test
    void GivenAvailableSeat_WhenRelease_ThenThrowException() {
        Seat seat = new Seat(position());

        assertThrows(IllegalStateException.class, seat::release);
    }

    @Test
    void GivenReservedSeat_WhenReserveAgain_ThenThrowException() {
        Seat seat = new Seat(position());
        seat.reserve();

        assertThrows(IllegalStateException.class, seat::reserve);
    }

    @Test
    void GivenAvailableSeat_WhenSell_ThenSeatIsSold() {
        Seat seat = new Seat(position());

        seat.sell();

        assertEquals(SeatStatus.SOLD, seat.getStatus());
    }

    @Test
    void GivenReservedSeat_WhenSell_ThenSeatIsSold() {
        Seat seat = new Seat(position());
        seat.reserve();

        seat.sell();

        assertEquals(SeatStatus.SOLD, seat.getStatus());
    }

    @Test
    void GivenSoldSeat_WhenSellAgain_ThenThrowException() {
        Seat seat = new Seat(position());
        seat.sell();

        assertThrows(IllegalStateException.class, seat::sell);
    }

    @Test
    void GivenSeat_WhenCopy_ThenCopyHasSameStateAndIsIndependent() {
        Seat original = new Seat(position());
        original.reserve();

        Seat copy = original.copy();
        copy.sell();

        assertNotSame(original, copy);
        assertNotSame(original.getPosition(), copy.getPosition());
        assertEquals(original.getPosition(), copy.getPosition());
        assertEquals(SeatStatus.RESERVED, original.getStatus());
        assertEquals(SeatStatus.SOLD, copy.getStatus());
    }
}

