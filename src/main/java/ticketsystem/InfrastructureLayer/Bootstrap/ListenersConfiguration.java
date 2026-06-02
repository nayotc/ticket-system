package ticketsystem.InfrastructureLayer.Bootstrap;

import org.springframework.context.annotation.Configuration;
import ticketsystem.ApplicationLayer.ReservationService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.ApplicationLayer.HistoryService;
import ticketsystem.ApplicationLayer.OrderService;
import ticketsystem.ApplicationLayer.EventService;
// ... שאר ה-imports

@Configuration
public class ListenersConfiguration {

    public ListenersConfiguration(
            ReservationService reservationService, HistoryService historyService,
             UserService userService, EventService eventService, OrderService orderService) {
        reservationService.addOrderListener(historyService);
        userService.addUserLoginListener(orderService);
        userService.addUserExitListener(orderService);
        eventService.addEventUpdatesListener(historyService);
        eventService.addEventUpdatesListener(orderService);
    }
}