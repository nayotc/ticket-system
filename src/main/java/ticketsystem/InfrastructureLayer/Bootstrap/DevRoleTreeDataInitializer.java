package ticketsystem.InfrastructureLayer.Bootstrap;

import java.util.Set;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.MembershipService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.Permission;

@Component
@Profile("dev")
public class DevRoleTreeDataInitializer {

    private static final long TEST_COMPANY_ID = 1L;

    private static final String FOUNDER_USERNAME = "founder@test.com";
    private static final String FOUNDER_PASSWORD = "123456";

    private static final String OWNER_USERNAME = "owner@test.com";
    private static final String OWNER_PASSWORD = "123456";

    private static final String EVENT_MANAGER_USERNAME = "eventmanager@test.com";
    private static final String EVENT_MANAGER_PASSWORD = "123456";

    private static final String SALES_MANAGER_USERNAME = "salesmanager@test.com";
    private static final String SALES_MANAGER_PASSWORD = "123456";

    private final UserService userService;
    private final IUserRepository userRepository;
    private final MembershipService membershipService;

    public DevRoleTreeDataInitializer(
            UserService userService,
            IUserRepository userRepository,
            MembershipService membershipService
    ) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.membershipService = membershipService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createRoleTreeDemoData() {
        try {
            Member founder = userRepository.getMemberByUsername(FOUNDER_USERNAME);

            if (founder == null || founder.getRoleInCompany(TEST_COMPANY_ID) == null) {
                System.out.println("Skipping role tree demo data: founder/company was not initialized yet.");
                return;
            }

            createMemberIfMissing(
                    OWNER_USERNAME,
                    OWNER_PASSWORD,
                    "Test Owner",
                    "0500000002"
            );

            createMemberIfMissing(
                    EVENT_MANAGER_USERNAME,
                    EVENT_MANAGER_PASSWORD,
                    "Event Manager",
                    "0500000003"
            );

            createMemberIfMissing(
                    SALES_MANAGER_USERNAME,
                    SALES_MANAGER_PASSWORD,
                    "Sales Manager",
                    "0500000004"
            );

            Member owner = userRepository.getMemberByUsername(OWNER_USERNAME);
            Member eventManager = userRepository.getMemberByUsername(EVENT_MANAGER_USERNAME);
            Member salesManager = userRepository.getMemberByUsername(SALES_MANAGER_USERNAME);

            if (owner == null || eventManager == null || salesManager == null) {
                System.out.println("Skipping role tree demo data: one of the demo members is missing.");
                return;
            }

            String founderToken = login(FOUNDER_USERNAME, FOUNDER_PASSWORD);

            if (!memberHasRole(owner, TEST_COMPANY_ID)) {
                membershipService.requestOwnerAssignment(
                        founderToken,
                        TEST_COMPANY_ID,
                        owner.getId()
                );

                String ownerToken = login(OWNER_USERNAME, OWNER_PASSWORD);
                membershipService.approveAssignment(ownerToken, TEST_COMPANY_ID);
            }

            if (!memberHasRole(eventManager, TEST_COMPANY_ID)) {
                membershipService.requestManagerAssignment(
                        founderToken,
                        TEST_COMPANY_ID,
                        eventManager.getId(),
                        Set.of(
                                Permission.MANAGE_EVENT_INVENTORY,
                                Permission.CONFIGURE_HALL_AND_MAP,
                                Permission.SET_PURCHASING_POLICY
                        )
                );

                String eventManagerToken = login(EVENT_MANAGER_USERNAME, EVENT_MANAGER_PASSWORD);
                membershipService.approveAssignment(eventManagerToken, TEST_COMPANY_ID);
            }

            if (!memberHasRole(salesManager, TEST_COMPANY_ID)) {
                String ownerToken = login(OWNER_USERNAME, OWNER_PASSWORD);

                membershipService.requestManagerAssignment(
                        ownerToken,
                        TEST_COMPANY_ID,
                        salesManager.getId(),
                        Set.of(
                                Permission.VIEW_PURCHASE_HISTORY,
                                Permission.GENERATE_SALES_REPORT
                        )
                );

                String salesManagerToken = login(SALES_MANAGER_USERNAME, SALES_MANAGER_PASSWORD);
                membershipService.approveAssignment(salesManagerToken, TEST_COMPANY_ID);
            }

            System.out.println("Role tree demo data initialized successfully for company ID: " + TEST_COMPANY_ID);

        } catch (Exception e) {
            System.out.println("Failed to initialize role tree demo data: " + e.getMessage());
        }
    }

    private void createMemberIfMissing(String username, String password, String fullName, String phone) {
        if (userRepository.isUsernameTaken(username)) {
            return;
        }

        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, username, password, fullName, phone);

        System.out.println("Dev role-tree user created:");
        System.out.println("username: " + username);
        System.out.println("password: " + password);
    }

    private String login(String username, String password) {
        String guestToken = userService.visitSystem();
        return userService.login(guestToken, username, password);
    }

    private boolean memberHasRole(Member member, Long companyId) {
        return member != null && member.getRoleInCompany(companyId) != null;
    }
}