package ticketsystem.InfrastructureLayer.Bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.event.Element;
import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.StandingArea;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.policy.PurchasePolicy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
@Profile("dev")
@Order(2)
public class DevEventCatalogDataInitializer implements CommandLineRunner {

    private static final long TEST_COMPANY_ID = 1L;
    private static final long AMAZING_EVENTS_COMPANY_ID = 2L;
    private static final long LAUGH_FACTORY_COMPANY_ID = 3L;

    private static final String AMAZING_EVENTS_COMPANY_NAME = "Amazing Events";
    private static final String LAUGH_FACTORY_COMPANY_NAME = "Laugh Factory";

    private static final long ELECTRONIC_EVENT_ID = 15L;
    private static final long STANDUP_EVENT_ID = 20L;
    private static final long NIGHT_LIGHTS_EVENT_ID = 30L;
    private static final long BASKETBALL_EVENT_ID = 40L;
    private static final long ART_EXHIBITION_EVENT_ID = 50L;
    private static final long HAIFA_LIVE_EVENT_ID = 60L;
    private static final long BROADWAY_EVENT_ID = 70L;
    private static final long LA_SPORTS_EVENT_ID = 80L;
    private static final long MIAMI_FOOD_EVENT_ID = 90L;
    private static final long CHICAGO_JAZZ_EVENT_ID = 100L;
    private static final long JERUSALEM_THEATER_EVENT_ID = 110L;
    private static final long HOUSTON_TECH_EXPO_EVENT_ID = 120L;
    private static final long BEER_SHEVA_STANDUP_EVENT_ID = 130L;
    private static final long NEW_YORK_ART_EVENT_ID = 140L;
    private static final long OTHER_FAMILY_EVENT_ID = 150L;

    private static final int ELECTRONIC_EVENT_LOTTERY_WINNERS = 10;

    private final IEventRepository eventRepository;
    private final ICompanyRepository companyRepository;
    private final ILotteryRepository lotteryRepository;

    public DevEventCatalogDataInitializer(
            IEventRepository eventRepository,
            ICompanyRepository companyRepository,
            ILotteryRepository lotteryRepository
    ) {
        this.eventRepository = eventRepository;
        this.companyRepository = companyRepository;
        this.lotteryRepository = lotteryRepository;
    }

    @Override
    public void run(String... args) {
        Company tixNowCompany = companyRepository.findById(TEST_COMPANY_ID).orElse(null);

        if (tixNowCompany == null) {
            System.out.println("Skipping dev event catalog data: test company was not found.");
            return;
        }

        long founderId = tixNowCompany.getFounderId();

        Company amazingEventsCompany = findOrCreateCompany(
                AMAZING_EVENTS_COMPANY_ID,
                AMAZING_EVENTS_COMPANY_NAME,
                founderId
        );

        Company laughFactoryCompany = findOrCreateCompany(
                LAUGH_FACTORY_COMPANY_ID,
                LAUGH_FACTORY_COMPANY_NAME,
                founderId
        );

        System.out.println("Dev event catalog data initializer is ready.");

        addEventIfMissing(createNightLightsEvent(amazingEventsCompany));
        addEventIfMissing(createStandupEvent(laughFactoryCompany));
        addEventIfMissing(createElectronicEvent(tixNowCompany));
        addEventIfMissing(createBasketballEvent(tixNowCompany));
        addEventIfMissing(createArtExhibitionEvent(amazingEventsCompany));
        addEventIfMissing(createHaifaLiveEvent(amazingEventsCompany));
        addEventIfMissing(createBroadwayEvent(laughFactoryCompany));
        addEventIfMissing(createLosAngelesSportsEvent(tixNowCompany));
        addEventIfMissing(createMiamiFoodEvent(amazingEventsCompany));
        addEventIfMissing(createChicagoJazzEvent(amazingEventsCompany));
        addEventIfMissing(createJerusalemTheaterEvent(laughFactoryCompany));
        addEventIfMissing(createHoustonTechExpoEvent(tixNowCompany));
        addEventIfMissing(createBeerShevaStandupEvent(laughFactoryCompany));
        addEventIfMissing(createNewYorkArtEvent(amazingEventsCompany));
        addEventIfMissing(createOtherFamilyEvent(tixNowCompany));

        addLotteryIfMissing(
                ELECTRONIC_EVENT_ID,
                ELECTRONIC_EVENT_LOTTERY_WINNERS
        );
    }

    private Company findOrCreateCompany(long companyId, String companyName, long founderId) {
        Company existingCompany = companyRepository.findById(companyId).orElse(null);

        if (existingCompany != null) {
            return existingCompany;
        }

        Company company = new Company(
                companyName,
                founderId,
                PurchasePolicy.noRestrictions(),
                new DiscountPolicy(DiscountCompositionType.MAX)
        );

        company.setId(companyId);
        companyRepository.save(company);

        System.out.println("Dev company created: " + company.getName() + " [ID: " + company.getId() + "]");

        return company;
    }

    /**
     * Creates the development lottery only when the event does not already have
     * one.
     *
     * @param eventId       event associated with the lottery
     * @param winnersNumber number of lottery winners
     */
    private void addLotteryIfMissing(
            long eventId,
            int winnersNumber
    ) {
        Lottery existingLottery =
                lotteryRepository.findByEventId(eventId);

        if (existingLottery != null) {
            System.out.println(
                    "Dev lottery already exists for event ID: " + eventId
            );
            return;
        }

        Lottery lottery = new Lottery(eventId, winnersNumber);

        lotteryRepository.addLottery(lottery);

        System.out.println(
                "Dev lottery created for event ID: "
                        + eventId
                        + " [Lottery ID: "
                        + lottery.getLotteryId()
                        + "]"
        );
    }

    private void addEventIfMissing(Event event) {
        if (eventRepository.getEventById(event.getId()) != null) {
            System.out.println("Dev event already exists: " + event.getName() + " [ID: " + event.getId() + "]");
            return;
        }

        eventRepository.addEvent(event);
        System.out.println("Dev event created: " + event.getName() + " [ID: " + event.getId() + "]");
    }

    private void addDefaultTicketMap(Event event) {
        event.getMap().addElement(new Element(
                1L,
                "במה מרכזית",
                new Pair<>(8, 2),
                new Pair<>(8, 3)
        ));

        event.getMap().addElement(new SeatingArea(
                2L,
                "יציע ישיבה",
                new Pair<>(4, 7),
                new Pair<>(10, 6),
                6,
                10
        ));

        event.getMap().addElement(new StandingArea(
                3L,
                "רחבת עמידה",
                new Pair<>(16, 7),
                new Pair<>(8, 8),
                120
        ));

        event.getMap().addElement(new Element(
                4L,
                "כניסה",
                new Pair<>(1, 1),
                new Pair<>(3, 3)
        ));

        event.setStatus(Event.eventStatus.ACTIVE);
    }

    private Event createNightLightsEvent(Company company) {
        Event event = new Event(
                NIGHT_LIGHTS_EVENT_ID,
                LocalDateTime.of(2026, 10, 24, 21, 0),
                "פסטיבל אורות הלילה",
                company.getId(),
                company.getFounderId(),
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "The Night Lights",
                BigDecimal.valueOf(249),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.ONGOING);
        event.setRate(4.8);
        return event;
    }

    private Event createStandupEvent(Company company) {
        Event event = new Event(
                STANDUP_EVENT_ID,
                LocalDateTime.of(2026, 11, 15, 22, 30),
                "מרתון צחוק תל אביבי",
                company.getId(),
                company.getFounderId(),
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.THEATER,
                "צוות Laugh Factory",
                BigDecimal.valueOf(119),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.SOLD_OUT);
        event.setRate(4.1);
        return event;
    }

    private Event createElectronicEvent(Company company) {
        Event event = new Event(
                ELECTRONIC_EVENT_ID,
                LocalDateTime.of(2026, 10, 20, 23, 55),
                "ליין שישי אלקטרוני",
                company.getId(),
                company.getFounderId(),
                EventLocation.TEL_AVIV,
                100L,
                EventCategory.CONCERT,
                "DJ Nova",
                BigDecimal.valueOf(90),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.NOT_STARTED);
        event.setRate(4.3);
        return event;
    }

    private Event createBasketballEvent(Company company) {
        Event event = new Event(
                BASKETBALL_EVENT_ID,
                LocalDateTime.of(2026, 12, 3, 20, 30),
                "דרבי הכדורסל של באר שבע",
                company.getId(),
                company.getFounderId(),
                EventLocation.BEER_SHEVA,
                120L,
                EventCategory.SPORTS,
                "הפועל באר שבע נגד מכבי דרום",
                BigDecimal.valueOf(75),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.ONGOING);
        event.setRate(4.4);
        return event;
    }

    private Event createArtExhibitionEvent(Company company) {
        Event event = new Event(
                ART_EXHIBITION_EVENT_ID,
                LocalDateTime.of(2026, 9, 12, 18, 0),
                "לילה במוזיאון ירושלים",
                company.getId(),
                company.getFounderId(),
                EventLocation.JERUSALEM,
                80L,
                EventCategory.EXHIBITION,
                "אוצרי מוזיאון העיר",
                BigDecimal.valueOf(55),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.ONGOING);
        event.setRate(4.6);
        return event;
    }

    private Event createHaifaLiveEvent(Company company) {
        Event event = new Event(
                HAIFA_LIVE_EVENT_ID,
                LocalDateTime.of(2026, 8, 7, 21, 15),
                "ים של מוזיקה בחיפה",
                company.getId(),
                company.getFounderId(),
                EventLocation.HAIFA,
                150L,
                EventCategory.CONCERT,
                "להקת הכרמל",
                BigDecimal.valueOf(130),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.NOT_STARTED);
        event.setRate(4.2);
        return event;
    }

    private Event createBroadwayEvent(Company company) {
        Event event = new Event(
                BROADWAY_EVENT_ID,
                LocalDateTime.of(2026, 7, 18, 20, 0),
                "Broadway Night בניו יורק",
                company.getId(),
                company.getFounderId(),
                EventLocation.NEW_YORK,
                90L,
                EventCategory.THEATER,
                "Broadway Cast",
                BigDecimal.valueOf(320),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.PRE_SALE);
        event.setRate(4.9);
        return event;
    }

    private Event createLosAngelesSportsEvent(Company company) {
        Event event = new Event(
                LA_SPORTS_EVENT_ID,
                LocalDateTime.of(2026, 9, 28, 19, 45),
                "גמר החוף בלוס אנג׳לס",
                company.getId(),
                company.getFounderId(),
                EventLocation.LOS_ANGELES,
                200L,
                EventCategory.SPORTS,
                "LA Beach League",
                BigDecimal.valueOf(210),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.ONGOING);
        event.setRate(3.7);
        return event;
    }

    private Event createMiamiFoodEvent(Company company) {
        Event event = new Event(
                MIAMI_FOOD_EVENT_ID,
                LocalDateTime.of(2026, 6, 22, 17, 30),
                "פסטיבל טעמים במיאמי",
                company.getId(),
                company.getFounderId(),
                EventLocation.MIAMI,
                60L,
                EventCategory.OTHER,
                "Miami Food Crew",
                BigDecimal.valueOf(25),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.ONGOING);
        event.setRate(3.3);
        return event;
    }

    private Event createChicagoJazzEvent(Company company) {
        Event event = new Event(
                CHICAGO_JAZZ_EVENT_ID,
                LocalDateTime.of(2026, 10, 2, 21, 30),
                "Jazz Night בשיקגו",
                company.getId(),
                company.getFounderId(),
                EventLocation.CHICAGO,
                110L,
                EventCategory.CONCERT,
                "Chicago Jazz Trio",
                BigDecimal.valueOf(160),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.SOLD_OUT);
        event.setRate(4.7);
        return event;
    }

    private Event createJerusalemTheaterEvent(Company company) {
        Event event = new Event(
                JERUSALEM_THEATER_EVENT_ID,
                LocalDateTime.of(2026, 11, 9, 19, 0),
                "הצגה ירושלמית מקורית",
                company.getId(),
                company.getFounderId(),
                EventLocation.JERUSALEM,
                95L,
                EventCategory.THEATER,
                "אנסמבל הבירה",
                BigDecimal.valueOf(85),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.NOT_STARTED);
        event.setRate(3.9);
        return event;
    }

    private Event createHoustonTechExpoEvent(Company company) {
        Event event = new Event(
                HOUSTON_TECH_EXPO_EVENT_ID,
                LocalDateTime.of(2026, 5, 14, 10, 0),
                "תערוכת חדשנות ביוסטון",
                company.getId(),
                company.getFounderId(),
                EventLocation.HOUSTON,
                180L,
                EventCategory.EXHIBITION,
                "Houston Innovation Center",
                BigDecimal.valueOf(0),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.ENDED);
        event.setRate(2.8);
        return event;
    }

    private Event createBeerShevaStandupEvent(Company company) {
        Event event = new Event(
                BEER_SHEVA_STANDUP_EVENT_ID,
                LocalDateTime.of(2026, 12, 20, 22, 0),
                "סטנדאפ במדבר",
                company.getId(),
                company.getFounderId(),
                EventLocation.BEER_SHEVA,
                70L,
                EventCategory.THEATER,
                "קומדי דרום",
                BigDecimal.valueOf(65),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.ONGOING);
        event.setRate(4.0);
        return event;
    }

    private Event createNewYorkArtEvent(Company company) {
        Event event = new Event(
                NEW_YORK_ART_EVENT_ID,
                LocalDateTime.of(2026, 8, 30, 12, 0),
                "גלריית קיץ בניו יורק",
                company.getId(),
                company.getFounderId(),
                EventLocation.NEW_YORK,
                50L,
                EventCategory.EXHIBITION,
                "NYC Art Collective",
                BigDecimal.valueOf(45),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.PRE_SALE);
        event.setRate(4.5);
        return event;
    }

    private Event createOtherFamilyEvent(Company company) {
        Event event = new Event(
                OTHER_FAMILY_EVENT_ID,
                LocalDateTime.of(2026, 7, 5, 11, 0),
                "יום משפחות בפארק",
                company.getId(),
                company.getFounderId(),
                EventLocation.OTHER,
                100L,
                EventCategory.OTHER,
                "Family Fun Team",
                BigDecimal.valueOf(35),
                new Pair<>(20, 30)
        );

        addDefaultTicketMap(event);
        event.setSaleStatus(SaleStatus.ONGOING);
        event.setRate(3.5);
        return event;
    }
}
