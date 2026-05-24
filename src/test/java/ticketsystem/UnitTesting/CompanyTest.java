package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DTO.DiscountRequestDTO;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.company.ConditionalDiscount;
import ticketsystem.DomainLayer.company.CouponDiscount;
import ticketsystem.DomainLayer.company.DiscountPolicy;
import ticketsystem.DomainLayer.company.PurchasePolicy;
import ticketsystem.DomainLayer.company.VisibleDiscount;
import ticketsystem.DomainLayer.company.ConditionalDiscount.Condition;
import ticketsystem.DomainLayer.company.DiscountPolicy.DiscountCompositionType;
import ticketsystem.DomainLayer.company.DiscountPolicy.DiscountKind;

class CompanyTest {

    private Company company;
    private final long FOUNDER_ID = 100L;

    @BeforeEach
    void setUp() {
        company = new Company("BGU Productions", FOUNDER_ID, new PurchasePolicy(),
                new DiscountPolicy(DiscountCompositionType.MAX));
    }

    // --- UC 3.2: Create Company & ID Generation ---

    @Test
    void GivenNewCompany_WhenCreated_ThenCompanyIsActiveAndFounderIdIsSaved() {
        assertTrue(company.isActive(), "A new company should be active by default.");
        assertEquals(FOUNDER_ID, company.getFounderId(), "Founder id should be saved on company creation.");
        assertEquals("BGU Productions", company.getName(), "Company name should be saved.");
    }

    @Test
    void GivenNewCompanies_WhenCreated_ThenIdsAreUniqueAndIncremented() {
        Company secondCompany = new Company("Another Company", FOUNDER_ID, new PurchasePolicy(),
                new DiscountPolicy(DiscountCompositionType.MAX));

        assertTrue(company.getId() > 0, "Company ID should be a positive number.");
        assertTrue(secondCompany.getId() > company.getId(), "New company should receive a higher incremented ID.");
        assertNotEquals(company.getId(), secondCompany.getId(), "Every company should have a unique ID.");
    }

    // --- UC 4.13: Close/Suspend Company state ---

    @Test
    void GivenActiveCompany_WhenCloseOrSuspend_ThenStatusIsInactive() throws Exception {
        company.closeOrSuspend();

        assertFalse(company.isActive(), "Company should be inactive after closing.");
    }

    @Test
    void GivenInactiveCompany_WhenCloseOrSuspendAgain_ThenExceptionIsThrown() throws Exception {
        company.closeOrSuspend();

        Exception exception = assertThrows(Exception.class, () -> company.closeOrSuspend());

        assertTrue(exception.getMessage().contains("already inactive"));
    }

    // --- UC 4.14: Reopen Company state ---

    @Test
    void GivenInactiveCompany_WhenReopenCompany_ThenStatusIsActive() throws Exception {
        company.closeOrSuspend();

        company.reopenCompany();

        assertTrue(company.isActive(), "Company should be active after reopening.");
    }

    @Test
    void GivenActiveCompany_WhenReopenCompany_ThenExceptionIsThrown() {
        Exception exception = assertThrows(Exception.class, () -> company.reopenCompany());

        assertTrue(exception.getMessage().contains("already Active"));
    }

    // --- UC 6.1: Close by System Admin state ---

    @Test
    void GivenActiveCompany_WhenCloseBySystemAdmin_ThenStatusIsInactive() throws Exception {
        company.closeBySystemAdmin();

        assertFalse(company.isActive(), "Company should be inactive after system admin closes it.");
    }

    @Test
    void GivenInactiveCompany_WhenCloseBySystemAdminAgain_ThenExceptionIsThrown() throws Exception {
        company.closeBySystemAdmin();

        Exception exception = assertThrows(Exception.class, () -> company.closeBySystemAdmin());

        assertTrue(exception.getMessage().contains("already inactive"));
    }

    // --- Copy Constructor ---

    @Test
    void GivenCompany_WhenCopyConstructorCalled_ThenDetachedCopyIsCreated() {
        company.setVersion(5);

        Company copy = new Company(company);

        assertEquals(company.getId(), copy.getId());
        assertEquals(company.getName(), copy.getName());
        assertEquals(company.getFounderId(), copy.getFounderId());
        assertEquals(company.isActive(), copy.isActive());
        assertEquals(5, copy.getVersion(), "Version should be copied correctly.");

        copy.setVersion(6);

        assertNotEquals(company.getVersion(), copy.getVersion(),
                "Modifying detached copy should not affect the original.");
    }

    // --- Discount Policy: Add Discount To Company ---

    @Test
    void GivenVisibleDiscountDTO_WhenAddDiscountToCompany_ThenVisibleDiscountIsAdded() {
        DiscountRequestDTO dto = createVisibleDiscountDTO();

        boolean result = company.addDiscountToCompany(dto);

        assertTrue(result);
        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof VisibleDiscount);
    }

    @Test
    void GivenConditionalDiscountDTO_WhenAddDiscountToCompany_ThenConditionalDiscountIsAdded() {
        DiscountRequestDTO dto = createConditionalDiscountDTO();

        boolean result = company.addDiscountToCompany(dto);

        assertTrue(result);
        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof ConditionalDiscount);
    }

    @Test
    void GivenCouponDiscountDTO_WhenAddDiscountToCompany_ThenCouponDiscountIsAdded() {
        DiscountRequestDTO dto = createCouponDiscountDTO();

        boolean result = company.addDiscountToCompany(dto);

        assertTrue(result);
        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof CouponDiscount);
    }

    @Test
    void GivenNullDiscountDTO_WhenAddDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(NullPointerException.class, () -> company.addDiscountToCompany(null));
    }

    private DiscountRequestDTO createVisibleDiscountDTO() {
        DiscountRequestDTO dto = new DiscountRequestDTO();

        dto.setDiscountType(DiscountKind.VISIBLE);
        dto.setName("Summer Sale");
        dto.setStartTime(LocalDateTime.now().minusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(7));
        dto.setPercentage(10.0);
        dto.setTargetTicketType("REGULAR");

        return dto;
    }

    private DiscountRequestDTO createConditionalDiscountDTO() {
        DiscountRequestDTO dto = new DiscountRequestDTO();

        dto.setDiscountType(DiscountKind.CONDITIONAL);
        dto.setName("Buy 2 Tickets Discount");
        dto.setStartTime(LocalDateTime.now().minusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(7));
        dto.setPercentage(15.0);
        dto.setTargetTicketType("REGULAR");
        dto.setCondition(Condition.MIN_TICKET);
        dto.setTicketThreshold(2);

        return dto;
    }

    private DiscountRequestDTO createCouponDiscountDTO() {
        DiscountRequestDTO dto = new DiscountRequestDTO();

        dto.setDiscountType(DiscountKind.COUPON);
        dto.setName("Coupon Discount");
        dto.setStartTime(LocalDateTime.now().minusDays(1));
        dto.setEndTime(LocalDateTime.now().plusDays(7));
        dto.setCouponCode("BGU10");
        dto.setPercentage(10.0);
        dto.setFixedAmount(0.0);

        return dto;
    }
}