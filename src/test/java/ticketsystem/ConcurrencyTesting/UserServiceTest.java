package ticketsystem.ConcurrencyTesting;

import java.util.ArrayList;
import static java.util.Collections.synchronizedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ticketsystem.ApplicationLayer.TokenService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.InfrastructureLayer.TokenRepository;
import ticketsystem.InfrastructureLayer.UserRepository;

public class UserServiceTest {
    private static UserService userService;
    private static UserRepository userRepository;
    private static TokenService tokenService;
    private static ITokenRepository tokenRepository;

    @BeforeEach
    public void setup() {
        userRepository = new UserRepository();
        tokenRepository = new TokenRepository();
        tokenService = new TokenService("manual_test_secret_32_chars_long", tokenRepository);
        userService = new UserService(userRepository, tokenService);
    }

    @Test
    public void testVisitSystem_ConcurrentAccess() throws InterruptedException {
        // Act: simulate 100 guests visiting the system concurrently
        int numberOfThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
        Set<String> generatedTokens = ConcurrentHashMap.newKeySet();
        List<Throwable> exceptions = synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    String token = userService.visitSystem();
                    generatedTokens.add(token);

                } catch (Throwable t) {
                    exceptions.add(t);
                    if (t instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out!");
        assertTrue(exceptions.isEmpty(), "Tasks failed with exceptions: " + exceptions);

        assertEquals(numberOfThreads, generatedTokens.size(), "Should generate unique tokens");
        assertEquals(numberOfThreads, tokenService.getTotalActiveSessions(), "All 100 guests should be saved");

    }

    @Test
    public void testSignUp_ConcurrentAccess() throws InterruptedException {
        // Arrange: simulate 100 sign up attempts with 10 different usernames (10
        // threads per username)
        int numberOfDiffrentUsernames = 10;
        int numberOfThreadPerUsername = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfDiffrentUsernames * numberOfThreadPerUsername);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfDiffrentUsernames * numberOfThreadPerUsername);

        List<Throwable> exceptions = synchronizedList(new ArrayList<>());

        for (int i = 0; i < numberOfDiffrentUsernames; i++) {
            final int userId = i;
            for (int j = 0; j < numberOfThreadPerUsername; j++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        String sessionToken = userService.visitSystem();
                        userService.signUp(sessionToken, "user" + userId, "password" + userId);
                    } catch (Throwable t) {
                        exceptions.add(t);
                        if (t instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
        }
        startLatch.countDown();
        boolean completed = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out!");
        assertTrue(exceptions.isEmpty(), "Tasks failed with exceptions: " + exceptions);

        assertEquals(numberOfDiffrentUsernames, userRepository.getAllRegisteredMembersCount(),
                "10 members should be saved");
        for (int i = 0; i < numberOfDiffrentUsernames; i++) {
            assertNotNull(userRepository.getMemberByUsername("user" + i), "User" + i + " should exist");
        }

    }

    @Test
    public void testLogin_ConcurrentAccess() throws InterruptedException {
        // Arrange: simulate 100 login attempts for the same user concurrently
        int numberOfThreads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);
        List<String> generatedTokens = synchronizedList(new ArrayList<>());
        List<Throwable> exceptions = synchronizedList(new ArrayList<>());
        String InitialSessionToken = userService.visitSystem();
        userService.signUp(InitialSessionToken, "username", "password");
        // Act: simulate 100 login attempts for the same user concurrently
        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String sessionToken = userService.visitSystem();
                    sessionToken=userService.login(sessionToken, "username", "password");
                    generatedTokens.add(sessionToken);
                } catch (Throwable t) {
                    exceptions.add(t);
                    if (t instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        boolean completed = doneLatch.await(50, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Test timed out!");
        assertTrue(exceptions.isEmpty(), "Tasks failed with exceptions: " + exceptions);
        assertEquals(numberOfThreads, generatedTokens.size(), "Should generate a token for each login attempt");
        for (String token : generatedTokens) {
            assertNotNull(token, "Login token should not be null");
            assertTrue(tokenService.isActiveSession(token), "Login token should be active");
            assertTrue(tokenService.validateToken(token), "Login token should be valid");
        }

    }
}
