package ticketsystem.ConcurrencyTesting;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.IUserRepository;
import ticketsystem.InfrastructureLayer.UserRepository;

public class UserServiceTest {
    @Test
    public void testVisitSystem_ConcurrentAccess() throws InterruptedException {
        // Arrange: use the real repository to test actual concurrent behavior
        IUserRepository userRepository = new UserRepository();
        UserService userService = new UserService(userRepository);    
        int numberOfThreads = 100;

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
}
