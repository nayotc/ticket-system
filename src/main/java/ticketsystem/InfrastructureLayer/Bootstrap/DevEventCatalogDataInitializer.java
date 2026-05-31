package ticketsystem.InfrastructureLayer.Bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IEventRepository;
import ticketsystem.DomainLayer.company.Company;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import ticketsystem.DomainLayer.event.Event;
import ticketsystem.DomainLayer.event.EventCategory;
import ticketsystem.DomainLayer.event.EventLocation;
import ticketsystem.DomainLayer.event.Pair;
import ticketsystem.DomainLayer.event.SaleStatus;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.lottery.Lottery;

@Component
@Profile("dev")
@Order(2)
public class DevEventCatalogDataInitializer implements CommandLineRunner {

    private static final long TEST_COMPANY_ID = 1L;
    private static final long AMAZING_EVENTS_COMPANY_ID = 2L;
    private static final long LAUGH_FACTORY_COMPANY_ID = 3L;

    private static final String AMAZING_EVENTS_COMPANY_NAME = "Amazing Events";
    private static final String LAUGH_FACTORY_COMPANY_NAME = "Laugh Factory";

    private static final long NIGHT_LIGHTS_EVENT_ID = 30L;
    private static final long STANDUP_EVENT_ID = 20L;
    private static final long ELECTRONIC_EVENT_ID = 15L;

    private static final long ELECTRONIC_EVENT_LOTTERY_ID = 100L;
    private static final int ELECTRONIC_EVENT_LOTTERY_WINNERS = 10;

    private final IEventRepository eventRepository;
    private final ICompanyRepository companyRepository;
    private final ILotteryRepository lotteryRepository;

    public DevEventCatalogDataInitializer(IEventRepository eventRepository, ICompanyRepository companyRepository, ILotteryRepository lotteryRepository) {
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

        addLotteryIfMissing(
                ELECTRONIC_EVENT_LOTTERY_ID,
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

    private void addLotteryIfMissing(long lotteryId, long eventId, int winnersNumber) {
        if (lotteryRepository.findById(lotteryId) != null) {
            System.out.println("Dev lottery already exists for event ID: " + eventId);
            return;
        }

        if (lotteryRepository.findByEventId(eventId) != null) {
            System.out.println("Dev lottery already exists for event ID: " + eventId);
            return;
        }

        lotteryRepository.addLottery(new Lottery(lotteryId, eventId, winnersNumber));
        System.out.println("Dev lottery created for event ID: " + eventId + " [Lottery ID: " + lotteryId + "]");
    }

    private void addEventIfMissing(Event event) {
        if (eventRepository.getEventById(event.getId()) != null) {
            System.out.println("Dev event already exists: " + event.getName() + " [ID: " + event.getId() + "]");
            return;
        }

        eventRepository.addEvent(event);
        System.out.println("Dev event created: " + event.getName() + " [ID: " + event.getId() + "]");
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

        event.setSaleStatus(SaleStatus.NOT_STARTED);
        event.setRate(4.3);
        return event;
    }
}