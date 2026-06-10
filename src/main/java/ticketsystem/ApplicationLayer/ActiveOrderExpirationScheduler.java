package ticketsystem.ApplicationLayer;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;

@Component
public class ActiveOrderExpirationScheduler {

    private final ReservationService reservationService;
    private final ISystemLogger logger;

    public ActiveOrderExpirationScheduler(
            ReservationService reservationService,
            ISystemLogger logger
    ) {
        this.reservationService = reservationService;
        this.logger = logger;
    }

    @Scheduled(
            fixedDelayString = "${ticketsystem.orders.expiration-sweep-ms:10000}",
            initialDelayString = "${ticketsystem.orders.expiration-sweep-initial-delay-ms:10000}"
    )
    public void sweepExpiredAndExpiringOrders() {
        try {
            reservationService.sweepExpiredAndExpiringOrders();
        } catch (Exception exception) {
            logger.logEvent(
                    "Active order expiration scheduler failed: " + exception.getMessage(),
                    LogLevel.WARN
            );
        }
    }
}