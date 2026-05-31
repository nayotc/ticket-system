package ticketsystem.InfrastructureLayer.Bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Element;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.StandingArea;
import ticketsystem.DomainLayer.event.Pair;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile("dev")
@Order(20)
public class DevEventDataInitializer implements CommandLineRunner {

    private static final Long DEMO_EVENT_ID = 1L;
    private static final Long DEMO_COMPANY_ID = 1L;
    private static final Long DEMO_OWNER_ID = 1L;

    private static final Long VIP_AREA_ID = 10L;
    private static final Long HALL_AREA_ID = 11L;
    private static final Long STANDING_AREA_ID = 20L;

    private final IEventRepository eventRepository;

    public DevEventDataInitializer(IEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void run(String... args) {
        createTicketSelectionDemoEvent();
    }

    private void createTicketSelectionDemoEvent() {
        if (eventRepository.getEventById(DEMO_EVENT_ID) != null) {
            System.out.println("Dev event already exists: " + DEMO_EVENT_ID);
            return;
        }

        Event event = new Event(
                DEMO_EVENT_ID,
                LocalDateTime.of(2026, 10, 24, 21, 0),
                "פסטיבל אורות הלילה",
                DEMO_COMPANY_ID,
                DEMO_OWNER_ID,
                EventLocation.TEL_AVIV,
                1000L,
                EventCategory.CONCERT,
                "אורות הלילה",
                new BigDecimal("220"),
                new Pair<>(20, 30)
        );

        event.getMap().addElement(new Element(
                1L,
                "במה",
                new Pair<>(3, 7),
                new Pair<>(2, 10)
        ));

        event.getMap().addElement(new Element(
                2L,
                "כניסה ראשית",
                new Pair<>(18, 8),
                new Pair<>(1, 4)
        ));

        event.getMap().addElement(new SeatingArea(
                VIP_AREA_ID,
                "אזור A - VIP",
                new Pair<>(6, 5),
                new Pair<>(5, 14),
                4,
                12
        ));

        event.getMap().addElement(new SeatingArea(
                HALL_AREA_ID,
                "אזור B - אולם",
                new Pair<>(12, 3),
                new Pair<>(5, 18),
                5,
                16
        ));

        event.getMap().addElement(new StandingArea(
                STANDING_AREA_ID,
                "רחבת עמידה",
                new Pair<>(6, 20),
                new Pair<>(9, 8),
                300
        ));

        eventRepository.addEvent(event);

        System.out.println("Dev event created:");
        System.out.println("event id: " + DEMO_EVENT_ID);
        System.out.println("VIP area id: " + VIP_AREA_ID);
        System.out.println("Hall area id: " + HALL_AREA_ID);
        System.out.println("Standing area id: " + STANDING_AREA_ID);
    }
}
