package DomainLayer.event;

public enum SeatStatus {
    AVAILABLE,
    RESERVED,
    SOLD
}

public class Seat {
    private final SeatPosition position;
    private SeatStatus status;
    private LocalDateTime reservedUntil;

    public Seat(SeatPosition position) {
        this.position = position;
        this.status = SeatStatus.AVAILABLE;
        this.reservedUntil = null;
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

    public LocalDateTime getReservedUntil() {
        return reservedUntil;
    }

    public void setReservedUntil(LocalDateTime reservedUntil) {
        this.reservedUntil = reservedUntil;
    }
}
