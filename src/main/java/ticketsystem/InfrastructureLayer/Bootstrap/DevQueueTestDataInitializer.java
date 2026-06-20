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

    public static final String PRE_SALE_QUEUED_EVENT_NAME = "ליין שישי אלקטרוני";
    public static final String APPROVED_EVENT_NAME = "פסטיבל אורות הלילה";
    public static final String QUEUED_EVENT_NAME = "מרתון צחוק תל אביבי";

    private final IEventRepository eventRepository;

    public DevQueueTestDataInitializer(IEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeQueueTestData() {
        try {
            Event approvedEvent = ensureEvent(
                    APPROVED_EVENT_NAME,
                    EventLocation.TEL_AVIV,
                    EventCategory.CONCERT,
                    "Amazing Events",
                    new BigDecimal("249"),
                    1000L,
                    SaleStatus.ONGOING
            );

            Event queuedEvent = ensureEvent(
                    QUEUED_EVENT_NAME,
                    EventLocation.TEL_AVIV,
                    EventCategory.THEATER,
                    "Laugh Factory",
                    new BigDecimal("119"),
                    0L,
                    SaleStatus.ONGOING
            );

            Event preSaleQueuedEvent = ensureEvent(
                    PRE_SALE_QUEUED_EVENT_NAME,
                    EventLocation.TEL_AVIV,
                    EventCategory.CONCERT,
                    "Electronic Line",
                    new BigDecimal("180"),
                    0L,
                    SaleStatus.PRE_SALE
            );

            System.out.println("Temporary queue test data initialized.");
            System.out.println("Approved event id: " + approvedEvent.getId());
            System.out.println("Queued event id: " + queuedEvent.getId());
            System.out.println("Pre-sale queued event id: " + preSaleQueuedEvent.getId());

        } catch (Exception e) {
            System.out.println("Failed to initialize temporary queue test data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Event ensureEvent(
            String name,
            EventLocation location,
            EventCategory category,
            String artist,
            BigDecimal price,
            long trafficThreshold,
            SaleStatus saleStatus
    ) {
        Event existing = eventRepository.getAllEvents().stream()
                .filter(event -> name.equals(event.getName()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setTrafficThreshold(trafficThreshold);
            existing.setStatus(Event.eventStatus.ACTIVE);
            existing.setSaleStatus(saleStatus);
            eventRepository.updateEvent(existing);

            System.out.println("Updated queue test event: " + existing.getId() + " - " + name);
            return existing;
        }

        Event event = new Event(
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

        System.out.println("Created queue test event: " + event.getId() + " - " + name);
        return event;
    }
}
