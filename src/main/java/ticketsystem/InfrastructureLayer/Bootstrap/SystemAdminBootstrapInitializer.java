package ticketsystem.InfrastructureLayer.Bootstrap;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import ticketsystem.ApplicationLayer.SystemAdminService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.user.Member;

/**
 * Ensures that the system always has at least one active system administrator.
 *
 * <p>This bootstrap is intentionally independent from the Version 3 initial-state
 * file. The initial-state file creates the demo/test scenario, while this class
 * enforces the system invariant that an empty database must still be initialized
 * with a system admin user.</p>
 *
 * <p>The admin credentials are read from configuration properties, so they are
 * not hard-coded in the main method or in the application logic. If the admin
 * already exists, the initializer does nothing. If the member exists but is not
 * a system admin, the initializer promotes the member. If the member does not
 * exist, it registers the member first and then promotes it.</p>
 */
@Component
@Order(0)
@ConditionalOnProperty(
        prefix = "ticketsystem.system-admin.bootstrap",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SystemAdminBootstrapInitializer implements CommandLineRunner {

    private final UserService userService;
    private final SystemAdminService systemAdminService;

    @Value("${ticketsystem.system-admin.username}")
    private String adminUsername;

    @Value("${ticketsystem.system-admin.password}")
    private String adminPassword;

    @Value("${ticketsystem.system-admin.full-name}")
    private String adminFullName;

    @Value("${ticketsystem.system-admin.phone}")
    private String adminPhone;

    @Value("${ticketsystem.system-admin.birth-date}")
    private String adminBirthDate;

    public SystemAdminBootstrapInitializer(
            UserService userService,
            SystemAdminService systemAdminService
    ) {
        this.userService = userService;
        this.systemAdminService = systemAdminService;
    }

    /**
     * Runs on application startup and guarantees that the configured system admin exists.
     *
     * @param args command-line arguments supplied by Spring Boot
     * @throws Exception if signup or promotion to system admin fails
     */
    @Override
    public void run(String... args) throws Exception {
        Optional<Member> existingAdminMember = findMemberByUsername(adminUsername);

        Member adminMember;
        if (existingAdminMember.isPresent()) {
            adminMember = existingAdminMember.get();
            System.out.println("System admin bootstrap: member already exists: " + adminUsername);
        } else {
            registerAdminMember();
            adminMember = findMemberByUsername(adminUsername)
                    .orElseThrow(() -> new IllegalStateException(
                            "System admin bootstrap failed: admin member was not found after signup: "
                                    + adminUsername
                    ));
            System.out.println("System admin bootstrap: member registered: " + adminUsername);
        }

       try {
        systemAdminService.promoteMemberToSystemAdmin(adminMember.getId());
        System.out.println("System admin bootstrap: member promoted to system admin: " + adminUsername);
    } catch (Exception e) {
        if (e.getMessage() != null && e.getMessage().contains("already an active System Admin")) {
            System.out.println("System admin bootstrap: system admin already exists: " + adminUsername);
            return;
        }

        throw e;
    }
    }

    private Optional<Member> findMemberByUsername(String username) {
        return userService.findMemberByUsername(username);
    }

    private void registerAdminMember() {
        String guestToken = userService.visitSystem();

        userService.signUp(
                guestToken,
                adminUsername,
                adminPassword,
                adminFullName,
                adminPhone,
                LocalDate.parse(adminBirthDate)
        );

        userService.exit(guestToken);
    }
}