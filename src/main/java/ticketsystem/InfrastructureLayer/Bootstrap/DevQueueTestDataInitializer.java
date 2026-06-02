package ticketsystem.InfrastructureLayer.Bootstrap;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SaleStatus;

@Component
@Profile("queue-test")
public class DevQueueTestDataInitializer {

    private static final long COMPANY_ID = 1L;

    private static final long APPROVED_EVENT_ID = 30L;
    private static final long QUEUED_EVENT_ID = 20L;

    public static final String APPROVED_EVENT_NAME = "פסטיבל אורות הלילה";
    public static final String QUEUED_EVENT_NAME = "מרתון צחוק תל אביבי";

    private final IEventRepository eventRepository;

    public DevQueueTestDataInitializer(IEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeQueueTestData() {
        try {
            ensureEvent(
                    APPROVED_EVENT_ID,
                    APPROVED_EVENT_NAME,
                    EventLocation.TEL_AVIV,
                    EventCategory.CONCERT,
                    "Amazing Events",
                    new BigDecimal("249"),
                    0L,
                    SaleStatus.ONGOING
            );

            ensureEvent(
                    QUEUED_EVENT_ID,
                    QUEUED_EVENT_NAME,
                    EventLocation.TEL_AVIV,
                    EventCategory.THEATER,
                    "Laugh Factory",
                    new BigDecimal("119"),
                    0L,
                    SaleStatus.ONGOING
            );

            System.out.println("Temporary queue test data initialized.");
            System.out.println("Approved event id: " + APPROVED_EVENT_ID);
            System.out.println("Queued event id: " + QUEUED_EVENT_ID);

        } catch (Exception e) {
            System.out.println("Failed to initialize temporary queue test data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ensureEvent(
            long eventId,
            String name,
            EventLocation location,
            EventCategory category,
            String artist,
            BigDecimal price,
            long trafficThreshold,
            SaleStatus saleStatus
    ) {
        Event existing = eventRepository.getEventById(eventId);

        if (existing != null) {
            existing.setTrafficThreshold(trafficThreshold);
            existing.setStatus(Event.eventStatus.ACTIVE);
            existing.setSaleStatus(saleStatus);
            eventRepository.updateEvent(existing);

            System.out.println("Updated queue test event: " + eventId + " - " + name);
            return;
        }

        Event event = new Event(
                eventId,
                LocalDateTime.now().plusMonths(2),
                name,
                COMPANY_ID,
                1L,
                location,
                trafficThreshold,
                category,
                artist,
                price,
                new Pair<>(20, 30)
        );

        event.setStatus(Event.eventStatus.ACTIVE);
        event.setSaleStatus(saleStatus);

        eventRepository.addEvent(event);

        System.out.println("Created queue test event: " + eventId + " - " + name);
    }
}