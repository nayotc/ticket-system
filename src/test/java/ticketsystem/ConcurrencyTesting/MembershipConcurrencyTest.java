package ticketsystem.ConcurrencyTesting;

import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.ApplicationLayer.ITokenService;
import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.ICompanyRepository;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.company.Company;
import ticketsystem.DomainLayer.discount.DiscountCompositionType;
import ticketsystem.DomainLayer.discount.DiscountPolicy;
import ticketsystem.DomainLayer.policy.PurchasePolicy;
import ticketsystem.DomainLayer.user.*;
import ticketsystem.InfrastructureLayer.CompanyRepository;
import ticketsystem.InfrastructureLayer.LogbackSystemLogger;
import ticketsystem.InfrastructureLayer.UserRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class MembershipConcurrencyTest {

    private IUserRepository userRepository;
    private MembershipService membershipService;

    private final Long companyId = 1L;
    private final Long targetMemberId = 200L;

    private String founderToken; // Appointer 1
    private String ownerToken;   // Appointer 2

    @BeforeEach
    void setUp() {
        ITokenRepository tokenRepo = new TokenRepository();
        ITokenService tokenService = new TokenService("my_very_long_secret_key_for_testing_purposes_only_32_chars", tokenRepo);
        this.userRepository = new UserRepository();
        ICompanyRepository companyRepository = new CompanyRepository();
        MembershipDomainService domainService = new MembershipDomainService(userRepository);
        ISystemLogger logger = new LogbackSystemLogger();

        // Initialize Application Service
        this.membershipService = new MembershipService(tokenService, userRepository, companyRepository, domainService, null, logger);
        
        // Setup Company
        Company company = new Company("BGU Productions", 100L, PurchasePolicy.noRestrictions(), new DiscountPolicy(DiscountCompositionType.MAX));
        try { company.setId(companyId); } catch (Exception e) {}
        companyRepository.save(company);

        // Setup Founder (Appointer 1) - Has version 0
        Member founder = new Member(100L, "Founder");
        founder.addFounderRole(companyId);
        userRepository.addRegisteredMember(100L, founder, "pass");
        founderToken = tokenService.addActiveSession(founder);

        // Setup active Owner (Appointer 2) - Has version 0
        Member owner = new Member(101L, "Owner");
        owner.addOwnerRole(companyId, 100L);
        owner.getRoleInCompany(companyId).setStatus(RoleStatus.ACTIVE);
        userRepository.addRegisteredMember(101L, owner, "pass");
        ownerToken = tokenService.addActiveSession(owner);

        // Setup Target Member - Starts with no roles, version 0
        Member target = new Member(targetMemberId, "TargetUser");
        userRepository.addRegisteredMember(targetMemberId, target, "pass");
    }

    /**
     * Test 1: Two actors concurrently attempt to assign an OWNER role to the same user.
     */
    @Test
    public void GivenConcurrentRequests_WhenAssigningSameOwner_ThenOnlyOneSucceeds() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1); 
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads); 

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        Runnable task1 = () -> {
            try {
                latch.await(); 
                membershipService.requestOwnerAssignment(founderToken, companyId, targetMemberId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                latch.await(); 
                membershipService.requestOwnerAssignment(ownerToken, companyId, targetMemberId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        // Fire the starting gun!
        latch.countDown(); 
        doneLatch.await();
        executor.shutdown();

        // Asserts
        assertEquals(1, successCount.get(), "Exactly one assignment should succeed.");
        assertEquals(1, exceptionCount.get(), "Exactly one assignment should throw an exception due to concurrent access.");
        
        Member updatedTarget = userRepository.getMemberById(targetMemberId);
        assertNotNull(updatedTarget.getRoleInCompany(companyId));
        assertTrue(updatedTarget.getRoleInCompany(companyId) instanceof Owner);
    }

    /**
     * Test 2: Two actors concurrently attempt to assign a MANAGER role to the same user.
     */
    @Test
    public void GivenConcurrentRequests_WhenAssigningSameManager_ThenOnlyOneSucceeds() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        
        Set<Permission> perms = new HashSet<>();
        perms.add(Permission.MANAGE_INQUIRIES);

        Runnable task1 = () -> {
            try {
                latch.await();
                membershipService.requestManagerAssignment(founderToken, companyId, targetMemberId, perms);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                latch.await();
                membershipService.requestManagerAssignment(ownerToken, companyId, targetMemberId, perms);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        latch.countDown(); 
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one manager appointment should succeed.");
        assertEquals(1, exceptionCount.get(), "Exactly one manager appointment should fail.");
        
        Member updatedTarget = userRepository.getMemberById(targetMemberId);
        assertTrue(updatedTarget.getRoleInCompany(companyId) instanceof Manager);
    }

    /**
     * Test 3: One actor assigns an Owner role, the other assigns a Manager role concurrently.
     */
    @Test
    public void GivenConcurrentRequests_WhenOneAssignsOwnerAndOtherAssignsManager_ThenOnlyOneSucceeds() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        
        Set<Permission> perms = new HashSet<>();
        perms.add(Permission.MANAGE_INQUIRIES);

        // Task 1 attempts to assign Owner
        Runnable task1 = () -> {
            try {
                latch.await();
                membershipService.requestOwnerAssignment(founderToken, companyId, targetMemberId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        // Task 2 attempts to assign Manager
        Runnable task2 = () -> {
            try {
                latch.await();
                membershipService.requestManagerAssignment(ownerToken, companyId, targetMemberId, perms);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        latch.countDown(); 
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Only one role assignment should succeed due to DB locking.");
        assertEquals(1, exceptionCount.get(), "The concurrent operation must be rejected.");
        
        Member updatedTarget = userRepository.getMemberById(targetMemberId);
        assertNotNull(updatedTarget.getRoleInCompany(companyId), "Target must have received exactly one role.");
    }

    /**
     * Test 4: Two actors concurrently assign a MANAGER role but with DIFFERENT permissions.
     */
    @Test
    public void GivenConcurrentRequests_WhenAssigningManagerWithDifferentPermissions_ThenOnlyOneSucceeds() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        
        // Task 1 Permissions
        Set<Permission> perms1 = new HashSet<>();
        perms1.add(Permission.MANAGE_INQUIRIES);

        // Task 2 Permissions
        Set<Permission> perms2 = new HashSet<>();
        perms2.add(Permission.CONFIGURE_HALL_AND_MAP);

        Runnable task1 = () -> {
            try {
                latch.await();
                membershipService.requestManagerAssignment(founderToken, companyId, targetMemberId, perms1);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                latch.await();
                membershipService.requestManagerAssignment(ownerToken, companyId, targetMemberId, perms2);
                successCount.incrementAndGet();
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        latch.countDown(); 
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one permission set should be assigned.");
        assertEquals(1, exceptionCount.get(), "Concurrent assignment should fail.");
        
        Member updatedTarget = userRepository.getMemberById(targetMemberId);
        Manager managerRole = (Manager) updatedTarget.getRoleInCompany(companyId);
        assertEquals(1, managerRole.getPermissions().size(), "Manager should end up with exactly 1 permission from the winning thread.");
    }
}