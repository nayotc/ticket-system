package ticketsystem.IntegrationTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.OptimisticLockException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.lottery.LotteryStatus;
import ticketsystem.InfrastructureLayer.LotteryRepository;
import ticketsystem.InfrastructureLayer.persistence.LotteryJpaRepository;

/**
 * Integration tests for the JPA-backed Lottery repository.
 *
 * <p>These tests use the production {@link LotteryRepository}, the real
 * Hibernate mappings and an embedded H2 database. Their purpose is to verify
 * that lottery aggregates, registrations, winners and winner codes are really
 * stored in the database and survive loading through a fresh persistence
 * context.</p>
 *
 * <p>The tests intentionally do not use an in-memory Lottery repository.
 * Production and tests use the same repository implementation; only the
 * configured datasource is different.</p>
 */
@DataJpaTest(
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY
)
@Import(LotteryRepository.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LotteryRepositoryIntegrationTest {

    private static final long FIRST_EVENT_ID = 1001L;
    private static final long SECOND_EVENT_ID = 1002L;

    private static final long FIRST_MEMBER_ID = 2001L;
    private static final long SECOND_MEMBER_ID = 2002L;

    private static final String WINNER_CODE = "WINNER01";

    @Autowired
    private LotteryRepository lotteryRepository;

    @Autowired
    private LotteryJpaRepository lotteryJpaRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    /**
     * Clears all persisted lotteries before each test.
     *
     * <p>The class does not run each test inside an automatically rolled-back
     * transaction, because every repository operation should execute as a real
     * transaction. Explicit cleanup therefore keeps all tests independent.</p>
     */
    @BeforeEach
    void cleanDatabase() {
        lotteryJpaRepository.deleteAll();
    }

    /**
     * Verifies that the database generates the Lottery identifier and that all
     * basic Lottery fields can be loaded from a fresh persistence context.
     */
    @Test
    void GivenNewLottery_WhenSavedAndReloaded_ThenGeneratedIdAndFieldsPersist() {
        // Arrange
        Lottery lottery = new Lottery(FIRST_EVENT_ID, 3);

        assertNull(
                lottery.getLotteryId(),
                "A new lottery should not have an ID before persistence."
        );

        assertNull(
                lottery.getVersion(),
                "A new lottery should not have a version before persistence."
        );

        // Act
        lotteryRepository.addLottery(lottery);

        Long generatedLotteryId = lottery.getLotteryId();
        Lottery reloadedLottery = loadFreshLottery(generatedLotteryId);

        // Assert
        assertNotNull(
                generatedLotteryId,
                "The database should generate a Lottery ID during persistence."
        );

        assertTrue(
                generatedLotteryId > 0,
                "The generated Lottery ID should be positive."
        );

        assertNotNull(
                lottery.getVersion(),
                "Hibernate should initialize the optimistic-locking version."
        );

        assertNotNull(
                reloadedLottery,
                "The persisted Lottery should be found in a fresh persistence context."
        );

        assertEquals(
                generatedLotteryId,
                reloadedLottery.getLotteryId()
        );

        assertEquals(
                FIRST_EVENT_ID,
                reloadedLottery.getEventId()
        );

        assertEquals(
                3,
                reloadedLottery.getWinnersNumber()
        );

        assertEquals(
                LotteryStatus.OPEN,
                reloadedLottery.getStatus()
        );

        assertTrue(
                reloadedLottery.getRegistrations().isEmpty(),
                "A newly created Lottery should have no registrations."
        );
    }

    /**
     * Verifies that registrations are persisted through the Lottery aggregate
     * and restored together with winner information and authentication codes.
     */
    @Test
    void GivenLotteryWithRegistrations_WhenSavedAndReloaded_ThenParticipantsAndWinnerPersist() {
        // Arrange
        Lottery lottery = new Lottery(FIRST_EVENT_ID, 1);

        lottery.registerMember(FIRST_MEMBER_ID);
        lottery.registerMember(SECOND_MEMBER_ID);
        lottery.setWinner(FIRST_MEMBER_ID, WINNER_CODE);

        // Act
        lotteryRepository.addLottery(lottery);

        Lottery reloadedLottery =
                loadFreshLottery(lottery.getLotteryId());

        // Assert
        assertNotNull(reloadedLottery);

        assertEquals(
                2,
                reloadedLottery.getRegistrations().size()
        );

        assertEquals(
                2,
                reloadedLottery.getRegisteredMemberIds().size()
        );

        assertTrue(
                reloadedLottery
                        .getRegisteredMemberIds()
                        .contains(FIRST_MEMBER_ID)
        );

        assertTrue(
                reloadedLottery
                        .getRegisteredMemberIds()
                        .contains(SECOND_MEMBER_ID)
        );

        assertEquals(
                1,
                reloadedLottery.getWinners().size()
        );

        assertTrue(
                reloadedLottery.getWinners().contains(FIRST_MEMBER_ID)
        );

        assertFalse(
                reloadedLottery.getWinners().contains(SECOND_MEMBER_ID)
        );

        assertTrue(
                reloadedLottery.validateWinnerCode(
                        FIRST_MEMBER_ID,
                        WINNER_CODE
                ),
                "The persisted winner code should be accepted after reload."
        );

        assertFalse(
                reloadedLottery.validateWinnerCode(
                        FIRST_MEMBER_ID,
                        "WRONG-CODE"
                ),
                "A wrong winner code should still be rejected after reload."
        );

        assertFalse(
                reloadedLottery.validateWinnerCode(
                        SECOND_MEMBER_ID,
                        WINNER_CODE
                ),
                "A registered non-winner must not be able to use the winner code."
        );
    }

    /**
     * Verifies that the repository can find a Lottery by its associated event
     * and that its registration collection is available after repository
     * loading.
     */
    @Test
    void GivenPersistedLottery_WhenFindByEventId_ThenLotteryAndRegistrationsAreReturned() {
        // Arrange
        Lottery lottery = new Lottery(FIRST_EVENT_ID, 2);
        lottery.registerMember(FIRST_MEMBER_ID);

        lotteryRepository.addLottery(lottery);

        // Act
        Lottery foundLottery =
                lotteryRepository.findByEventId(FIRST_EVENT_ID);

        // Assert
        assertNotNull(foundLottery);

        assertEquals(
                lottery.getLotteryId(),
                foundLottery.getLotteryId()
        );

        assertEquals(
                FIRST_EVENT_ID,
                foundLottery.getEventId()
        );

        assertEquals(
                1,
                foundLottery.getRegisteredMemberIds().size()
        );

        assertTrue(
                foundLottery
                        .getRegisteredMemberIds()
                        .contains(FIRST_MEMBER_ID)
        );

        assertNull(
                lotteryRepository.findByEventId(SECOND_EVENT_ID),
                "An event without a Lottery should return null."
        );

        assertNull(
                lotteryRepository.findById(Long.MAX_VALUE),
                "A missing Lottery ID should return null."
        );
    }

    /**
     * Verifies that changes made after the initial persistence are stored in
     * the database, including changes inside an existing registration.
     */
    @Test
    void GivenPersistedLottery_WhenUpdatedAndReloaded_ThenStatusAndWinnerChangesPersist() {
        // Arrange
        Lottery lottery = new Lottery(FIRST_EVENT_ID, 1);
        lottery.registerMember(FIRST_MEMBER_ID);

        lotteryRepository.addLottery(lottery);

        Lottery lotteryToUpdate =
                loadFreshLottery(lottery.getLotteryId());

        assertNotNull(lotteryToUpdate);

        // Act
        lotteryToUpdate.setWinnersNumber(2);
        lotteryToUpdate.setWinner(FIRST_MEMBER_ID, WINNER_CODE);
        lotteryToUpdate.setStatus(LotteryStatus.COMPLETED);

        lotteryRepository.update(lotteryToUpdate);

        Lottery reloadedLottery =
                loadFreshLottery(lottery.getLotteryId());

        // Assert
        assertNotNull(reloadedLottery);

        assertEquals(
                2,
                reloadedLottery.getWinnersNumber()
        );

        assertEquals(
                LotteryStatus.COMPLETED,
                reloadedLottery.getStatus()
        );

        assertEquals(
                1,
                reloadedLottery.getWinners().size()
        );

        assertTrue(
                reloadedLottery.getWinners().contains(FIRST_MEMBER_ID)
        );

        assertTrue(
                reloadedLottery.validateWinnerCode(
                        FIRST_MEMBER_ID,
                        WINNER_CODE
                )
        );
    }

    /**
     * Verifies that the database unique constraint prevents two different
     * lotteries from being associated with the same event.
     */
    @Test
    void GivenLotteryForEvent_WhenSavingSecondLotteryForSameEvent_ThenDatabaseRejectsDuplicate() {
        // Arrange
        Lottery firstLottery = new Lottery(FIRST_EVENT_ID, 1);
        Lottery duplicateEventLottery = new Lottery(FIRST_EVENT_ID, 2);

        lotteryRepository.addLottery(firstLottery);

        // Act + Assert
        assertThrows(
                DataIntegrityViolationException.class,
                () -> lotteryRepository.addLottery(duplicateEventLottery),
                "The database should reject two lotteries for the same event."
        );

        assertEquals(
                1L,
                lotteryJpaRepository.count(),
                "The rejected duplicate must not create another Lottery row."
        );

        Lottery storedLottery =
                lotteryRepository.findByEventId(FIRST_EVENT_ID);

        assertNotNull(storedLottery);

        assertEquals(
                firstLottery.getLotteryId(),
                storedLottery.getLotteryId()
        );
    }

    /**
     * Verifies that optimistic locking prevents an older detached copy from
     * overwriting an update that was already persisted by another transaction.
     */
    @Test
    void GivenTwoDetachedCopies_WhenStaleCopyUpdatesAfterFirst_ThenOptimisticLockingRejectsUpdate() {
        // Arrange
        Lottery originalLottery = new Lottery(FIRST_EVENT_ID, 1);

        lotteryRepository.addLottery(originalLottery);

        long lotteryId = originalLottery.getLotteryId();

        Lottery firstCopy = loadFreshLottery(lotteryId);
        Lottery staleSecondCopy = loadFreshLottery(lotteryId);

        assertNotNull(firstCopy);
        assertNotNull(staleSecondCopy);

        assertEquals(
                firstCopy.getVersion(),
                staleSecondCopy.getVersion(),
                "Both detached copies should initially have the same version."
        );

        // Act: first copy updates successfully.
        firstCopy.setWinnersNumber(2);
        lotteryRepository.update(firstCopy);

        // The second copy still contains the previous @Version value.
        staleSecondCopy.setWinnersNumber(3);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> lotteryRepository.update(staleSecondCopy)
        );

        // Assert
        assertTrue(
                hasCause(
                        exception,
                        OptimisticLockingFailureException.class
                ) || hasCause(
                        exception,
                        OptimisticLockException.class
                ),
                "Saving a stale Lottery should fail because of optimistic locking."
        );

        Lottery finalStoredLottery = loadFreshLottery(lotteryId);

        assertNotNull(finalStoredLottery);

        assertEquals(
                2,
                finalStoredLottery.getWinnersNumber(),
                "The stale update must not overwrite the successful update."
        );
    }

    /**
     * Loads a Lottery through a completely new persistence context.
     *
     * <p>The registrations are fetched explicitly so that the returned detached
     * aggregate can still expose participant and winner information after the
     * EntityManager is closed.</p>
     *
     * @param lotteryId Lottery identifier
     * @return freshly loaded Lottery, or null when it does not exist
     */
    private Lottery loadFreshLottery(long lotteryId) {
        EntityManager entityManager =
                entityManagerFactory.createEntityManager();

        try {
            return entityManager.createQuery(
                            """
                            SELECT DISTINCT lottery
                            FROM Lottery lottery
                            LEFT JOIN FETCH lottery.registrations
                            WHERE lottery.lotteryId = :lotteryId
                            """,
                            Lottery.class
                    )
                    .setParameter("lotteryId", lotteryId)
                    .getResultStream()
                    .findFirst()
                    .orElse(null);
        } finally {
            entityManager.close();
        }
    }

    /**
     * Searches the complete exception chain for the expected exception type.
     *
     * <p>Spring Data and Hibernate may wrap optimistic-locking exceptions, so
     * checking only the outer exception would make the test dependent on the
     * exact infrastructure wrapper.</p>
     *
     * @param throwable        exception thrown by the repository
     * @param expectedType     expected exception type
     * @return true when the expected type exists anywhere in the cause chain
     */
    private boolean hasCause(
            Throwable throwable,
            Class<? extends Throwable> expectedType
    ) {
        Throwable current = throwable;

        while (current != null) {
            if (expectedType.isInstance(current)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}