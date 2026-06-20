package ticketsystem.DomainLayer.event;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SeatPosition implements Serializable {

    @Column(name = "seat_row")
    private int row;

    @Column(name = "seat_number")
    private int number;

    protected SeatPosition() {
    }

    public SeatPosition(int row, int number) {
        this.row = row;
        this.number = number;
    }

    public int row() {
        return row;
    }

    public int number() {
        return number;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof SeatPosition position)) {
            return false;
        }

        return row == position.row && number == position.number;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, number);
    }
}