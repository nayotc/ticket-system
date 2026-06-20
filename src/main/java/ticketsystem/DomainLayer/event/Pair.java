package ticketsystem.DomainLayer.event;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Pair<K, V> implements Serializable {

    @Column(name = "first_value")
    private Integer first;

    @Column(name = "second_value")
    private Integer second;

    protected Pair() {
    }

    public Pair(K first, V second) {
        this.first = ((Number) first).intValue();
        this.second = ((Number) second).intValue();
    }

    public Pair(Pair<K, V> other) {
        this.first = other.first;
        this.second = other.second;
    }

    public Pair<K, V> copy() {
        return new Pair<>(this);
    }

    @SuppressWarnings("unchecked")
    public K getFirst() {
        return (K) first;
    }

    @SuppressWarnings("unchecked")
    public V getSecond() {
        return (V) second;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof Pair<?, ?> pair)) {
            return false;
        }

        return Objects.equals(first, pair.first)
                && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }
}