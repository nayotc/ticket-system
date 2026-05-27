package ticketsystem.InfrastructureLayer.Bootstrap;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.IUserRepository;

@Component
@Profile("dev")
public class DevDataInitializer implements CommandLineRunner {

    private static final String TEST_USERNAME = "test@test.com";
    private static final String TEST_PASSWORD = "123456";

    private final UserService userService;
    private final IUserRepository userRepository;

    public DevDataInitializer(UserService userService, IUserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        createTestMember();
    }

    private void createTestMember() {
        if (userRepository.isUsernameTaken(TEST_USERNAME)) {
            System.out.println("Dev user already exists: " + TEST_USERNAME);
            return;
        }

        String guestToken = userService.visitSystem();
        userService.signUp(guestToken, TEST_USERNAME, TEST_PASSWORD, "Test User", "0500000000");

        System.out.println("Dev user created:");
        System.out.println("username: " + TEST_USERNAME);
        System.out.println("password: " + TEST_PASSWORD);
    }
}