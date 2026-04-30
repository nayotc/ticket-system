package ticketsystem.DomainLayer.IRepository;

import ticketsystem.DomainLayer.Reservation;

public interface IReservationRepository {

    int generateNextId();

    void saveReservation(Reservation reservation);

    void deleteReservation(int reservationId);

    Reservation getReservationByOrderId(int orderId);

    public void deleteReservationByOrderId(int orderId);


}