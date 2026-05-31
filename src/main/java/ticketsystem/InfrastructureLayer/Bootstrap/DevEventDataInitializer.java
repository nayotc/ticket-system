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

    private static final Long STAGE_ELEMENT_ID = 1L;
    private static final Long SEATING_AREA_ID = 2L;
    private static final Long STANDING_AREA_ID = 3L;
    private static final Long BAR_ELEMENT_ID = 4L;
    private static final Long EXIT_ELEMENT_ID = 5L;
    private static final Long FIRST_AID_ELEMENT_ID = 6L;

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
                LocalDateTime.of(2026, 6, 5, 0, 45),
                "omer adam show",
                DEMO_COMPANY_ID,
                DEMO_OWNER_ID,
                EventLocation.HOUSTON,
                200L,
                EventCategory.THEATER,
                "omer adam",
                new BigDecimal("350.00"),
                new Pair<>(40, 60)
        );

        addLoggedMap(event);
        eventRepository.addEvent(event);

        System.out.println("Dev event created from logged map:");
        System.out.println("event id: " + DEMO_EVENT_ID);
        System.out.println("map size: 40x60");
        System.out.println("seating area id: " + SEATING_AREA_ID);
        System.out.println("standing area id: " + STANDING_AREA_ID);
    }

    private void addLoggedMap(Event event) {
        event.getMap().addElement(new Element(
                STAGE_ELEMENT_ID,
                "במה מרכזית",
                new Pair<>(25, 4),
                new Pair<>(20, 5)
        ));

        event.getMap().addElement(new SeatingArea(
                SEATING_AREA_ID,
                "אזור ישיבה",
                new Pair<>(30, 12),
                new Pair<>(10, 5),
                10,
                20
        ));

        event.getMap().addElement(new StandingArea(
                STANDING_AREA_ID,
                "אזור עמידה",
                new Pair<>(25, 19),
                new Pair<>(20, 10),
                500
        ));

        event.getMap().addElement(new Element(
                BAR_ELEMENT_ID,
                "בר",
                new Pair<>(44, 30),
                new Pair<>(5, 4)
        ));

        event.getMap().addElement(new Element(
                EXIT_ELEMENT_ID,
                "יציאת חירום",
                new Pair<>(15, 11),
                new Pair<>(4, 3)
        ));

        event.getMap().addElement(new Element(
                FIRST_AID_ELEMENT_ID,
                "עזרה ראשונה",
                new Pair<>(38, 30),
                new Pair<>(5, 4)
        ));
    }
}
