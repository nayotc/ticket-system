package ticketsystem.DomainLayer.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_seats")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private SeatPosition position;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SeatStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seating_area_id")
    private SeatingArea seatingArea;

    protected Seat() {}

    public Seat(SeatPosition position) {
        this.position = position;
        this.status = SeatStatus.AVAILABLE;
    }

    public Seat(Seat other) {
        this.id = other.id;
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

    public void setSeatingArea(SeatingArea seatingArea) {
        this.seatingArea = seatingArea;
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
