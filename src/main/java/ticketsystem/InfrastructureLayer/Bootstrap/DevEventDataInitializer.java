package ticketsystem.InfrastructureLayer.Bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.event.Element;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.StandingArea;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile("dev")
@Order(20)
public class DevEventDataInitializer implements CommandLineRunner {

    private static final String DEMO_EVENT_NAME = "omer adam show";
    private static final Long DEMO_COMPANY_ID = 1L;
    private static final Long DEMO_OWNER_ID = 1L;

    private final IEventRepository eventRepository;

    public DevEventDataInitializer(IEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void run(String... args) {
        createTicketSelectionDemoEvent();
    }

    private void createTicketSelectionDemoEvent() {
        Event existing = eventRepository.getAllEvents().stream()
                .filter(event -> DEMO_EVENT_NAME.equals(event.getName()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            System.out.println("Dev event already exists: " + existing.getId());
            printGeneratedAreaIds(existing);
            return;
        }

        Event event = new Event(
                LocalDateTime.of(2026, 6, 5, 0, 45),
                DEMO_EVENT_NAME,
                DEMO_COMPANY_ID,
                DEMO_OWNER_ID,
                EventLocation.HOUSTON,
                100L,
                EventCategory.CONCERT,
                "Omer Adam",
                BigDecimal.valueOf(200),
                new Pair<>(40, 60)
        );

        event.setStatus(Event.eventStatus.ACTIVE);

        addLoggedMap(event);

        eventRepository.addEvent(event);

        // Only now have JPA and the database generated the IDs
        printGeneratedAreaIds(event);

        System.out.println("Dev event created from logged map:");
        System.out.println("event id: " + event.getId());
        System.out.println("map size: 40x60");
    }

    private void addLoggedMap(Event event) {
        event.getMap().addElement(new Element(
                "במה מרכזית",
                new Pair<>(25, 4),
                new Pair<>(20, 5)
        ));

        event.getMap().addElement(new SeatingArea(
                "אזור ישיבה",
                new Pair<>(30, 12),
                new Pair<>(10, 5),
                10,
                20,
                new BigDecimal("120.00")
        ));

        event.getMap().addElement(new StandingArea(
                "אזור עמידה",
                new Pair<>(25, 19),
                new Pair<>(20, 10),
                500,
                new BigDecimal("80.00")
        ));

        event.getMap().addElement(new Element(
                "בר",
                new Pair<>(44, 30),
                new Pair<>(5, 4)
        ));

        event.getMap().addElement(new Element(
                "יציאת חירום",
                new Pair<>(15, 11),
                new Pair<>(4, 3)
        ));

        event.getMap().addElement(new Element(
                "עזרה ראשונה",
                new Pair<>(38, 30),
                new Pair<>(5, 4)
        ));
    }

    private void printGeneratedAreaIds(Event event) {
        Long seatingAreaId = event.getMap().getElements().stream()
                .filter(SeatingArea.class::isInstance)
                .map(Element::getId)
                .findFirst()
                .orElse(null);

        Long standingAreaId = event.getMap().getElements().stream()
                .filter(StandingArea.class::isInstance)
                .map(Element::getId)
                .findFirst()
                .orElse(null);

        System.out.println("seating area id: " + seatingAreaId);
        System.out.println("standing area id: " + standingAreaId);
    }
}
