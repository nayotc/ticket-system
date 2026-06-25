package ticketsystem.PersistenceTesting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.history.PurchasedTicket;
import ticketsystem.DomainLayer.history.TicketStatus;
import ticketsystem.InfrastructureLayer.HistoryRepository;

/**

* Integration tests for the JPA-based history repository.
*
* <p>These tests use the same {@link HistoryRepository} implementation used
* by the production application, while Spring supplies an embedded H2
* database for the test environment.</p>
*
* <p>The persistence context is explicitly cleared after writes. This ensures
* that assertions are performed on entities reloaded from the database rather
* than on the original Java objects that were passed to the repository.</p>
  */
  @DataJpaTest(
  properties = {
  "spring.jpa.hibernate.ddl-auto=create-drop",
  "spring.jpa.properties.hibernate.generate_statistics=true"
  }
  )
  @AutoConfigureTestDatabase(
  replace = AutoConfigureTestDatabase.Replace.ANY
  )
  @Import(HistoryRepository.class)
  class HistoryRepositoryIntegrationTest {

  @Autowired
  private HistoryRepository historyRepository;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  /**

  * Verifies that a purchase and all of its purchased-ticket snapshots are
  * inserted into the database and can be loaded again after the persistence
  * context is cleared.
    */
    @Test
    void givenNewPurchase_whenSavedAndReloaded_thenPurchaseAndTicketsArePersisted() {
    // Arrange
    Purchase purchase = new Purchase(
    List.of(
    new PurchasedTicket(
    1001L,"VIP",
    3,
    7,
    new BigDecimal("150.00"),
    "BARCODE-1001"
    ),
    new PurchasedTicket(
    1002L,"VIP",
    3,
    8,
    new BigDecimal("100.00"),
    "BARCODE-1002"
    )
    ),
    "New York Music Festival",
    "Madison Square Garden",
    21L,
    31L,
    41L,
    51L,
    new BigDecimal("250.00"),
    7001
    );

    assertNull(
    purchase.getPurchaseId(),
    "A new purchase must not receive an ID before it is persisted."
    );

    // Act
    historyRepository.addPurchase(purchase);

    Long generatedPurchaseId = purchase.getPurchaseId();

    assertNotNull(
    generatedPurchaseId,
    "The database must generate the purchase ID."
    );

    /*

    * Remove all managed entities from Hibernate's first-level cache.
    * The following repository call must therefore query the database.
      */
      entityManager.flush();
      entityManager.clear();

    Purchase reloadedPurchase =
    historyRepository.findPurchaseById(generatedPurchaseId);

    // Assert
    assertNotNull(
    reloadedPurchase,
    "The saved purchase should be found after clearing "
    + "the persistence context."
    );

    assertNotSame(
    purchase,
    reloadedPurchase,
    "The loaded purchase should be a new object reconstructed "
    + "from the database."
    );

    assertEquals(
    generatedPurchaseId,
    reloadedPurchase.getPurchaseId()
    );

    assertEquals(
    "New York Music Festival",
    reloadedPurchase.getEventName()
    );

    assertEquals(
    "Madison Square Garden",
    reloadedPurchase.getLocation()
    );

    assertEquals(
    21L,
    reloadedPurchase.getMemberId()
    );

    assertEquals(
    31L,
    reloadedPurchase.getCompanyId()
    );

    assertEquals(
    41L,
    reloadedPurchase.getManagedByMemberId()
    );

    assertEquals(
    51L,
    reloadedPurchase.getEventId()
    );

    assertEquals(
    0,
    new BigDecimal("250.00").compareTo(
    reloadedPurchase.getTotalPrice()
    )
    );

    assertEquals(
    7001,
    reloadedPurchase.getTransactionId()
    );

    assertFalse(
    reloadedPurchase.isRefunded(),
    "A new purchase should not be marked as refunded."
    );

    assertEquals(
    2,
    reloadedPurchase.getTickets().size(),
    "Both purchased tickets should be loaded from the database."
    );

    PurchasedTicket firstTicket =
    reloadedPurchase.getTickets().get(0);

    assertEquals(
    1001L,
    firstTicket.getTicketId()
    );

    assertEquals(
    3,
    firstTicket.getRow()
    );

    assertEquals(
    7,
    firstTicket.getChair()
    );

    assertEquals(
    0,
    new BigDecimal("150.00").compareTo(
    firstTicket.getPrice()
    )
    );

    assertEquals(
    TicketStatus.ACTIVE,
    firstTicket.getStatus()
    );

    assertEquals(
    "BARCODE-1001",
    firstTicket.getSecureBarcode()
    );
    }

  /**

  * Verifies that repository queries filter persisted purchase history by
  * member, company and event identifiers.
    */
    @Test
    void givenSeveralPurchases_whenQueryingHistory_thenOnlyMatchingPurchasesAreReturned() {
    // Arrange
    Purchase firstPurchase = createPurchase(
    101L,
    201L,
    301L,
    401L,
    "First Event",
    8001,
    1101L,
    "BARCODE-A",
    new BigDecimal("100.00")
    );

    Purchase secondPurchase = createPurchase(
    102L,
    201L,
    302L,
    402L,
    "Second Event",
    8002,
    1102L,
    "BARCODE-B",
    new BigDecimal("120.00")
    );

    Purchase thirdPurchase = createPurchase(
    101L,
    202L,
    303L,
    401L,
    "First Event",
    8003,
    1103L,
    "BARCODE-C",
    new BigDecimal("140.00")
    );

    historyRepository.addPurchase(firstPurchase);
    historyRepository.addPurchase(secondPurchase);
    historyRepository.addPurchase(thirdPurchase);

    Long firstPurchaseId = firstPurchase.getPurchaseId();
    Long secondPurchaseId = secondPurchase.getPurchaseId();
    Long thirdPurchaseId = thirdPurchase.getPurchaseId();

    entityManager.flush();
    entityManager.clear();

    // Act
    List<Purchase> memberPurchases =
    historyRepository.getPurchasesByMemberId(101L);

    List<Purchase> companyPurchases =
    historyRepository.getPurchasesByCompanyId(201L);

    List<Purchase> eventPurchases =
    historyRepository.getPurchasesByEventId(401L);

    List<Purchase> allPurchases =
    historyRepository.getAllPurchases();

    // Assert
    assertEquals(
    Set.of(firstPurchaseId, thirdPurchaseId),
    purchaseIds(memberPurchases),
    "Member history should contain only purchases made "
    + "by that member."
    );

    assertEquals(
    Set.of(firstPurchaseId, secondPurchaseId),
    purchaseIds(companyPurchases),
    "Company history should contain only purchases belonging "
    + "to that company."
    );

    assertEquals(
    Set.of(firstPurchaseId, thirdPurchaseId),
    purchaseIds(eventPurchases),
    "Event history should contain only purchases belonging "
    + "to that event."
    );

    assertEquals(
    Set.of(
    firstPurchaseId,
    secondPurchaseId,
    thirdPurchaseId
    ),
    purchaseIds(allPurchases),
    "The global history should contain all persisted purchases."
    );
    }

  /**

  * Verifies that updates to the refund flag and historical ticket status
  * are written to the database.
    */
    @Test
    void givenPersistedPurchase_whenRefundedAndCanceled_thenChangesArePersisted() {
    // Arrange
    Purchase purchase = createPurchase(
    501L,
    601L,
    701L,
    801L,
    "Canceled Event",
    9001,
    1201L,
    "BARCODE-CANCELED",
    new BigDecimal("180.00")
    );

    historyRepository.addPurchase(purchase);

    Long purchaseId = purchase.getPurchaseId();

    entityManager.flush();
    entityManager.clear();

    Purchase managedPurchase =
    historyRepository.findPurchaseById(purchaseId);

    assertNotNull(managedPurchase);

    // Act
    managedPurchase.setRefunded(true);

    managedPurchase
    .getTickets()
    .get(0)
    .setStatus(TicketStatus.CANCELED);

    entityManager.flush();
    entityManager.clear();

    Purchase reloadedPurchase =
    historyRepository.findPurchaseById(purchaseId);

    // Assert
    assertNotNull(reloadedPurchase);

    assertTrue(
    reloadedPurchase.isRefunded(),
    "The refunded flag should remain true after reloading."
    );

    assertEquals(
    TicketStatus.CANCELED,
    reloadedPurchase.getTickets().get(0).getStatus(),
    "The canceled ticket status should remain persisted."
    );
    }

  /**

  * Verifies that a guest purchase can be stored without a member
  * identifier.
    */
    @Test
    void givenGuestPurchase_whenSavedAndReloaded_thenNullMemberIdIsPreserved() {
    // Arrange
    Purchase guestPurchase = createPurchase(
    null,
    901L,
    902L,
    903L,
    "Guest Event",
    9101,
    1301L,
    "GUEST-BARCODE",
    new BigDecimal("90.00")
    );

    // Act
    historyRepository.addPurchase(guestPurchase);

    Long purchaseId = guestPurchase.getPurchaseId();

    entityManager.flush();
    entityManager.clear();

    Purchase reloadedPurchase =
    historyRepository.findPurchaseById(purchaseId);

    // Assert
    assertNotNull(reloadedPurchase);

    assertNull(
    reloadedPurchase.getMemberId(),
    "A guest purchase should remain without a member ID."
    );

    assertEquals(
    901L,
    reloadedPurchase.getCompanyId()
    );

    assertEquals(
    903L,
    reloadedPurchase.getEventId()
    );
    }

  /**

  * Verifies that repository queries return empty collections when no
  * matching purchase history exists.
    */
    @Test
    void givenEmptyDatabase_whenQueryingHistory_thenEmptyResultsAreReturned() {
    assertTrue(
    historyRepository.getAllPurchases().isEmpty()
    );

    assertTrue(
    historyRepository
    .getPurchasesByMemberId(999_001L)
    .isEmpty()
    );

    assertTrue(
    historyRepository
    .getPurchasesByCompanyId(999_002L)
    .isEmpty()
    );

    assertTrue(
    historyRepository
    .getPurchasesByEventId(999_003L)
    .isEmpty()
    );

    assertNull(
    historyRepository.findPurchaseById(999_004L)
    );
    }

  /**

  * Verifies that loading many purchases together with their ticket
  * snapshots uses a bounded number of SQL statements and does not create
  * an N+1 query pattern.
    */
    @Test
    void givenOneHundredPurchases_whenLoadingByEvent_thenTicketsAreLoadedWithoutNPlusOneQueries() {
    // Arrange
    long eventId = 7_001L;
    int purchaseCount = 100;

    for (int index = 0; index < purchaseCount; index++) {
    Purchase purchase = createPurchase(
    20_000L + index,
    30_000L,
    40_000L,
    eventId,
    "N Plus One Test Event",
    50_000 + index,
    60_000L + index,
    "BARCODE-" + index,
    new BigDecimal("75.00")
    );

     historyRepository.addPurchase(purchase);


    }

    /*

    * Flush all inserts and clear Hibernate's first-level cache before
    * measuring the repository query.
      */
      entityManager.flush();
      entityManager.clear();

    SessionFactory sessionFactory =
    entityManagerFactory.unwrap(SessionFactory.class);

    Statistics statistics =
    sessionFactory.getStatistics();

    statistics.clear();

    // Act
    List<Purchase> purchases =
    historyRepository.getPurchasesByEventId(eventId);

    int loadedTicketCount = purchases.stream()
    .mapToInt(purchase ->
    purchase.getTickets().size()
    )
    .sum();

    long preparedStatementCount =
    statistics.getPrepareStatementCount();

    // Assert
    assertEquals(
    purchaseCount,
    purchases.size(),
    "All matching purchases should be loaded."
    );

    assertEquals(
    purchaseCount,
    loadedTicketCount,
    "Each purchase should contain its persisted ticket snapshot."
    );

    assertTrue(
    preparedStatementCount <= 2,
    "Loading purchases and tickets should use a bounded number "
    + "of queries instead of one query per purchase. "
    + "Actual prepared statements: "
    + preparedStatementCount
    );
    }

  /**

  * Verifies that the database rejects a purchase whose required event
  * identifier is missing, even when domain validation is bypassed.
  *
  * <p>Reflection is used only in this persistence test so that the database
  * NOT NULL constraint itself is exercised. Production code cannot normally
  * create this invalid state because the Purchase constructor validates the
  * event identifier.</p>
    */
    @Test
    void givenPurchaseWithoutEventId_whenPersisted_thenDatabaseRejectsConstraintViolation() {
    // Arrange
    Purchase invalidPurchase = createPurchase(
    101L,
    201L,
    301L,
    401L,
    "Constraint Test Event",
    8_001,
    9_001L,
    "CONSTRAINT-BARCODE",
    new BigDecimal("100.00")
    );

    /*

    * Bypass the domain validation only to verify that the database
    * constraint provides an additional protection layer.
      */
      ReflectionTestUtils.setField(
      invalidPurchase,
      "eventId",
      null
      );

    // Act + Assert
    assertThrows(
    DataIntegrityViolationException.class,
    () -> historyRepository.addPurchase(invalidPurchase),
    "The database should reject a purchase without an event ID."
    );
    }

  /**

  * Creates a valid purchase aggregate for persistence tests.
  *
  * @param memberId buyer member ID, or {@code null} for a guest
  * @param companyId production company ID
  * @param managedByMemberId member responsible for the event
  * @param eventId event ID
  * @param eventName historical event name
  * @param transactionId external payment transaction ID
  * @param ticketId purchased ticket ID
  * @param barcode secure ticket barcode
  * @param price ticket and purchase price
  * @return a new purchase without a database-generated ID
    */
    private Purchase createPurchase(
    Long memberId,
    Long companyId,
    Long managedByMemberId,
    Long eventId,
    String eventName,
    int transactionId,
    Long ticketId,
    String barcode,
    BigDecimal price
    ) {
    return new Purchase(
    List.of(
    new PurchasedTicket(
    ticketId,"VIP",
    1,
    1,
    price,
    barcode
    )
    ),
    eventName,
    "Test Location",
    memberId,
    companyId,
    managedByMemberId,
    eventId,
    price,
    transactionId
    );
    }

  /**

  * Extracts purchase IDs without relying on database result ordering.
  *
  * @param purchases purchases returned by the repository
  * @return set of purchase IDs
    */
    private Set<Long> purchaseIds(List<Purchase> purchases) {
    return purchases.stream()
    .map(Purchase::getPurchaseId)
    .collect(Collectors.toSet());
    }
    }
