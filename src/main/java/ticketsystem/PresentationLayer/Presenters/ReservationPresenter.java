package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.ReservationService;

@Component
public class ReservationPresenter {

    private final ReservationService reservationService;

    public ReservationPresenter(ReservationService reservationService) {
        this.reservationService = reservationService;
    }
}
