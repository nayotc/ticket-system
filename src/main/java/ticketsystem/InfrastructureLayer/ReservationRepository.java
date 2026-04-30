package ticketsystem.InfrastructureLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import ticketsystem.DomainLayer.Reservation;
import ticketsystem.DomainLayer.IRepository.IReservationRepository;

public class ReservationRepository implements IReservationRepository {

    private AtomicInteger counter;
    private static ReservationRepository instance;

    private Map<Integer, Reservation> reservationsById;
    private Map<Integer, Reservation> ReservationByOrderId;

    private ReservationRepository() {
        this.counter = new AtomicInteger(1);
        this.reservationsById = new ConcurrentHashMap<>();
        this.ReservationByOrderId = new ConcurrentHashMap<>();
    }

    public static ReservationRepository getInstance() {
        if (instance == null) {
            instance = new ReservationRepository();
        }
        return instance;
    }

    @Override
    public int generateNextId() {
        return counter.getAndIncrement();
    }

    @Override
    public Reservation getReservationByOrderId(int orderId) {
        return ReservationByOrderId.get(orderId);
    }

    @Override
    public void saveReservation(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("Reservation cannot be null");
        }

        reservationsById.put(reservation.getReservationId(), reservation);
        ReservationByOrderId.put(reservation.getOrderId(), reservation);
    }

    @Override
    public void deleteReservation(int reservationId) {
        Reservation reservation = reservationsById.remove(reservationId);

        if (reservation != null) {
            ReservationByOrderId.remove(reservation.getOrderId());
        }
    }

    public void deleteReservationByOrderId(int orderId) {
        Reservation reservation = ReservationByOrderId.remove(orderId);

        if (reservation != null) {
            reservationsById.remove(reservation.getReservationId());
        }
    }
}