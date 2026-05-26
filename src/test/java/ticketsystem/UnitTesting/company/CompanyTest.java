package ticketsystem.UnitTesting.company;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.VisibleDiscount;
import ticketsystem.DomainLayer.discount.ConditionalDiscount.Condition;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountKind;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.policy.PurchasePolicy;

class CompanyTest {

    private Company company;
    private final long FOUNDER_ID = 100L;

    @BeforeEach
    void setUp() {
        company = new Company("BGU Productions", FOUNDER_ID, PurchasePolicy.noRestrictions(),
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
        Company secondCompany = new Company("Another Company", FOUNDER_ID, PurchasePolicy.noRestrictions(),
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
    void GivenValidVisibleDiscount_WhenAddVisibleDiscountToCompany_ThenDiscountIsAdded() {
        company.addVisibleDiscountToCompany("Student Discount", BigDecimal.valueOf(10));

        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof VisibleDiscount);
    }

    @Test
    void GivenNullName_WhenAddVisibleDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.addVisibleDiscountToCompany(null, BigDecimal.valueOf(10))
        );
    }

    @Test
    void GivenEmptyName_WhenAddVisibleDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.addVisibleDiscountToCompany("", BigDecimal.valueOf(10))
        );
    }

    @Test
    void GivenNullPercentage_WhenAddVisibleDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.addVisibleDiscountToCompany("Discount", null)
        );
    }

    @Test
    void GivenNegativePercentage_WhenAddVisibleDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.addVisibleDiscountToCompany("Discount", BigDecimal.valueOf(-1))
        );
    }

    @Test
    void GivenPercentageAbove100_WhenAddVisibleDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.addVisibleDiscountToCompany("Discount", BigDecimal.valueOf(101))
        );
    }
    // --- Discount Policy: Add Coupon Discount ---

    @Test
    void GivenValidCouponDiscount_WhenAddCouponDiscountToCompany_ThenDiscountIsAdded() {
        company.addCouponDiscountToCompany(
                "Coupon Discount",
                "BGU10",
                BigDecimal.valueOf(10),
                LocalDateTime.now().plusDays(7)
        );

        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof CouponDiscount);
    }

    @Test
    void GivenNullCouponCode_WhenAddCouponDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.addCouponDiscountToCompany(
                        "Coupon Discount",
                        null,
                        BigDecimal.valueOf(10),
                        LocalDateTime.now().plusDays(7)
                )
        );
    }

    @Test
    void GivenEmptyCouponCode_WhenAddCouponDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.addCouponDiscountToCompany(
                        "Coupon Discount",
                        "",
                        BigDecimal.valueOf(10),
                        LocalDateTime.now().plusDays(7)
                )
        );
    }
    // --- Discount Policy: Add Conditional Discount ---

    @Test
    void GivenValidMinTicketCondition_WhenAddConditionalDiscountToCompany_ThenDiscountIsAdded() {
        company.addConditionalDiscountToCompany(
                "Min Ticket Discount",
                null,
                null,
                BigDecimal.valueOf(15),
                Condition.MIN_TICKET,
                2
        );

        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof ConditionalDiscount);
    }

    @Test
    void GivenValidMaxTicketCondition_WhenAddConditionalDiscountToCompany_ThenDiscountIsAdded() {
        company.addConditionalDiscountToCompany(
                "Max Ticket Discount",
                null,
                null,
                BigDecimal.valueOf(15),
                Condition.MAX_TICKET,
                5
        );

        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof ConditionalDiscount);
    }

    @Test
    void GivenValidDateCondition_WhenAddConditionalDiscountToCompany_ThenDiscountIsAdded() {
        company.addConditionalDiscountToCompany(
                "Date Discount",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7),
                BigDecimal.valueOf(15),
                Condition.DATE,
                null
        );

        assertEquals(1, company.getDiscountPolicy().getDiscounts().size());
        assertTrue(company.getDiscountPolicy().getDiscounts().get(0) instanceof ConditionalDiscount);
    }

    @Test
    void GivenNullCondition_WhenAddConditionalDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.addConditionalDiscountToCompany(
                        "Conditional Discount",
                        null,
                        null,
                        BigDecimal.valueOf(15),
                        null,
                        2
                )
        );
    }

    @Test
    void GivenMinTicketConditionAndNullThreshold_WhenAddConditionalDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.addConditionalDiscountToCompany(
                        "Min Ticket Discount",
                        null,
                        null,
                        BigDecimal.valueOf(15),
                        Condition.MIN_TICKET,
                        null
                )
        );
    }

    @Test
    void GivenMaxTicketConditionAndNegativeThreshold_WhenAddConditionalDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.addConditionalDiscountToCompany(
                        "Max Ticket Discount",
                        null,
                        null,
                        BigDecimal.valueOf(15),
                        Condition.MAX_TICKET,
                        -1
                )
        );
    }

    @Test
    void GivenDateConditionAndNullDates_WhenAddConditionalDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.addConditionalDiscountToCompany(
                        "Date Discount",
                        null,
                        null,
                        BigDecimal.valueOf(15),
                        Condition.DATE,
                        null
                )
        );
    }

    @Test
    void GivenDateConditionAndEndBeforeStart_WhenAddConditionalDiscountToCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.addConditionalDiscountToCompany(
                        "Date Discount",
                        LocalDateTime.now().plusDays(7),
                        LocalDateTime.now().minusDays(1),
                        BigDecimal.valueOf(15),
                        Condition.DATE,
                        null
                )
        );
    }
    // --- Discount Policy: Remove Discount ---

    @Test
    void GivenExistingDiscount_WhenRemoveDiscountFromCompany_ThenDiscountIsRemoved() {
        company.addVisibleDiscountToCompany("Discount", BigDecimal.valueOf(10));

        Long discountId = company.getDiscountPolicy()
                .getDiscounts()
                .get(0)
                .getDiscountId();

        company.removeDiscountFromCompany(discountId);

        assertEquals(0, company.getDiscountPolicy().getDiscounts().size());
    }

    @Test
    void GivenNonExistingDiscount_WhenRemoveDiscountFromCompany_ThenExceptionIsThrown() {
        assertThrows(IllegalArgumentException.class, () ->
                company.removeDiscountFromCompany(999L)
        );
    }
    // --- Discount Policy: Composition Type ---

    @Test
    void GivenCompositionTypeSum_WhenSetDiscountCompositionType_ThenTypeIsUpdated() {
        company.setDiscountCompositionType(DiscountCompositionType.SUM);

        assertEquals(DiscountCompositionType.SUM, company.getDiscountCompositionType());
    }

    @Test
    void GivenCompositionTypeMax_WhenSetDiscountCompositionType_ThenTypeIsUpdated() {
        company.setDiscountCompositionType(DiscountCompositionType.MAX);

        assertEquals(DiscountCompositionType.MAX, company.getDiscountCompositionType());
    }
    
}