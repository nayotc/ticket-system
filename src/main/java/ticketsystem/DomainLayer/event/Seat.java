package ticketsystem.DomainLayer.event;

import java.time.LocalDateTime;

public class Seat {
    private final SeatPosition position;
    private SeatStatus status;

    public Seat(SeatPosition position) {
        this.position = position;
        this.status = SeatStatus.AVAILABLE;
    }

    public Seat(Seat other) {
        this.position = new SeatPosition(other.position.row(), other.position.number());
        this.status = other.status;
    }

    public Seat copy() {
        return new Seat(this);
    }

    public SeatPosition getPosition() {
        return position;
    }

    public SeatStatus getStatus() {
        return status;
    }

    public void setStatus(SeatStatus status) {
        this.status = status;
    }

    public enum SeatStatus {
    AVAILABLE,
    RESERVED,
    SOLD
    }

    public void reserve() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("Seat is not available");
        }
        this.status = SeatStatus.RESERVED;
    }

    public void release() {
        if (this.status != SeatStatus.RESERVED) {
            throw new IllegalStateException("Seat is not reserved");
        }
        this.status = SeatStatus.AVAILABLE;
    }

    public void sell() {
        if (this.status == SeatStatus.SOLD) {
            throw new IllegalStateException("Seat is already sold");
        }
        this.status = SeatStatus.SOLD;
    }
}
