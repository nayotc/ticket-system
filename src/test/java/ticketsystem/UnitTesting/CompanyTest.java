package ticketsystem.UnitTesting;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.discount.ConditionalDiscount;
import ticketsystem.DomainLayer.discount.CouponDiscount;
import ticketsystem.DomainLayer.discount.VisibleDiscount;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountKind;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.policy.PolicyResult;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.policy.PurchaseRule;

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

        assertEquals(
                company.getIdOrNull(),
                copy.getIdOrNull(),
                "The copy should preserve the original nullable ID state."
        );
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
    void GivenCompositionTypeSum_WhenSetDiscountCompositionType_ThenTypeIsUpdated() {
        company.setDiscountCompositionType(DiscountCompositionType.SUM);

        assertEquals(DiscountCompositionType.SUM, company.getDiscountCompositionType());
    }

    @Test
    void GivenCompositionTypeMax_WhenSetDiscountCompositionType_ThenTypeIsUpdated() {
        company.setDiscountCompositionType(DiscountCompositionType.MAX);

        assertEquals(DiscountCompositionType.MAX, company.getDiscountCompositionType());
    }
       @Test
    void GivenAllowedPurchasePolicy_WhenCompanyCanPurchase_ThenDoesNotThrow() {
        Company company = createCompanyWithPolicyResult(PolicyResult.allowed());

        assertDoesNotThrow(() -> company.canPurchase(2, 20));
    }

    @Test
    void GivenDeniedPurchasePolicy_WhenCompanyCanPurchase_ThenThrowIllegalArgumentException() {
        Company company = createCompanyWithPolicyResult(
                PolicyResult.denied("Purchase policy violation")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> company.canPurchase(2, 20)
        );
    }

    @Test
    void GivenDeniedPurchasePolicy_WhenCompanyCanPurchase_ThenExceptionContainsPolicyMessage() {
        String expectedMessage = "Cannot purchase more than 5 tickets.";

        Company company = createCompanyWithPolicyResult(
                PolicyResult.denied(expectedMessage)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> company.canPurchase(6, 20)
        );

        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void GivenDeniedPurchasePolicyWithNullMessage_WhenCompanyCanPurchase_ThenThrowDefaultMessage() {
        Company company = createCompanyWithPolicyResult(
                PolicyResult.denied(null)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> company.canPurchase(2, 20)
        );

        assertEquals(
                "User does not satisfy the purchase policy",
                exception.getMessage()
        );
    }

    @Test
    void GivenDeniedPurchasePolicyWithBlankMessage_WhenCompanyCanPurchase_ThenThrowDefaultMessage() {
        Company company = createCompanyWithPolicyResult(
                PolicyResult.denied("   ")
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> company.canPurchase(2, 20)
        );

        assertEquals(
                "User does not satisfy the purchase policy",
                exception.getMessage()
        );
    }

    @Test
    void GivenPurchasePolicyReturnsNull_WhenCompanyCanPurchase_ThenThrowIllegalStateException() {
        Company company = createCompanyWithPolicyResult(null);

        assertThrows(
                IllegalStateException.class,
                () -> company.canPurchase(2, 20)
        );
    }

    @Test
    void GivenNullPurchasePolicy_WhenSetPurchasePolicy_ThenThrowIllegalArgumentException() {
        Company company = createCompanyWithPolicyResult(PolicyResult.allowed());

        assertThrows(
                IllegalArgumentException.class,
                () -> company.setPurchasePolicy(null)
        );
    }

    private Company createCompanyWithPolicyResult(PolicyResult result) {
        PurchasePolicy purchasePolicy = new PurchasePolicy(
                new FixedResultPurchaseRule(result)
        );

        return new Company(
                "Test Company",
                1L,
                purchasePolicy,
                new DiscountPolicy(DiscountCompositionType.MAX)
        );
    }

    private static class FixedResultPurchaseRule extends PurchaseRule {

        private final PolicyResult result;

        private FixedResultPurchaseRule(PolicyResult result) {
            this.result = result;
        }

        @Override
        public PolicyResult isValid(int quantity, int age) {
            return result;
        }
    }
    
}