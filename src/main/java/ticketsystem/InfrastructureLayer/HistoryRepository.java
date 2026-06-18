package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Repository;

import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.InfrastructureLayer.persistence.HistoryJpaRepository;

/**
 * JPA-based adapter for purchase-history persistence.
 *
 * <p>This is the single concrete history-repository implementation used by
 * both the production application and database-backed tests. The environment
 * changes the database configuration, not the repository implementation.</p>
 */
@Repository
public class HistoryRepository implements IHistoryRepository {

    private final HistoryJpaRepository historyJpaRepository;

    /**
     * Creates the history repository adapter.
     *
     * @param historyJpaRepository Spring Data repository used for database
     *                             access
     */
    public HistoryRepository(
            HistoryJpaRepository historyJpaRepository
    ) {
        this.historyJpaRepository = Objects.requireNonNull(
                historyJpaRepository,
                "History JPA repository cannot be null."
        );
    }

    /**
     * Persists a purchase aggregate.
     *
     * <p>{@code saveAndFlush} ensures that a database-generated identifier is
     * assigned before control returns to the application service. Flushing
     * does not commit the surrounding transaction.</p>
     *
     * @param purchase purchase to persist
     */
    @Override
    public void addPurchase(Purchase purchase) {
        Objects.requireNonNull(
                purchase,
                "Purchase cannot be null."
        );

        historyJpaRepository.saveAndFlush(purchase);
    }

    /**
     * Finds a purchase by identifier.
     *
     * @param purchaseId purchase identifier
     * @return matching purchase, or {@code null}
     */
    @Override
    public Purchase findPurchaseById(long purchaseId) {
        return historyJpaRepository.findById(purchaseId)
                .orElse(null);
    }

    /**
     * Returns the history of a specific buyer.
     *
     * @param memberId buyer member identifier
     * @return buyer purchases
     */
    @Override
    public List<Purchase> getPurchasesByMemberId(long memberId) {
        return historyJpaRepository
                .findAllByMemberIdOrderByPurchaseIdAsc(memberId);
    }

    /**
     * Returns the history associated with a company.
     *
     * @param companyId company identifier
     * @return company purchases
     */
    @Override
    public List<Purchase> getPurchasesByCompanyId(long companyId) {
        return historyJpaRepository
                .findAllByCompanyIdOrderByPurchaseIdAsc(companyId);
    }

    /**
     * Returns all persisted purchases.
     *
     * @return all purchases
     */
    @Override
    public List<Purchase> getAllPurchases() {
        return historyJpaRepository
                .findAllByOrderByPurchaseIdAsc();
    }

    /**
     * Returns the history associated with an event.
     *
     * @param eventId event identifier
     * @return event purchases
     */
    @Override
    public List<Purchase> getPurchasesByEventId(long eventId) {
        return historyJpaRepository
                .findAllByEventIdOrderByPurchaseIdAsc(eventId);
    }
}