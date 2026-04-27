package ticketsystem.ConcurrencyTesting;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.InfrastructureLayer.UserRepository;

public class UserServiceTest {
        private static UserService userService;
        private static UserRepository userRepository;
        private static TokenService tokenService;
        private static int numberOfThreads;
    @BeforeEach
    public void setup() {
        System.out.println("Starting UserService concurrency tests...");
        userRepository = new UserRepository();
        tokenService = new TokenService();
        userService = new UserService(userRepository, tokenService);
        numberOfThreads = 100; 
    }
    @Test
    public void testVisitSystem_ConcurrentAccess() throws InterruptedException {   
        //Act: simulate 100 guests visiting the system concurrently
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
        Set<String> generatedTokens = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    String token = userService.visitSystem();
                    generatedTokens.add(token);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(numberOfThreads, generatedTokens.size(), "Should generate unique tokens");
        assertEquals(numberOfThreads, userRepository.getTotalActiveSessions(), "All 100 guests should be saved");
    }
    @Test
    public void testSignUp_ConcurrentAccess() throws InterruptedException {
        // Arrange: simulate 100 guests visiting the system concurrently
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String sessionToken = userService.visitSystem();
                    userService.signUp(sessionToken, "user" + userId, "password" + userId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(numberOfThreads, userRepository.getAllRegisteredMembersCount(), "All 100 guests should be saved");
    }
}
