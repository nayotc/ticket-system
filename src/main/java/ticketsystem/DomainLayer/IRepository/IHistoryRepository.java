package ticketsystem.DomainLayer.IRepository;

import java.util.List;

import ticketsystem.DomainLayer.history.Purchase;

/**
 * Repository contract for purchase-history aggregates.
 *
 * <p>The domain and application layers depend only on this interface and do
 * not know whether the data is stored in H2, a production database, or any
 * other persistence technology.</p>
 */
public interface IHistoryRepository {

    /**
     * Persists a completed purchase together with all of its ticket snapshots.
     *
     * <p>For a new purchase, the database assigns the purchase identifier.</p>
     *
     * @param purchase purchase aggregate to persist
     */
    void addPurchase(Purchase purchase);

    /**
     * Finds a purchase by its database-generated identifier.
     *
     * @param purchaseId purchase identifier
     * @return matching purchase, or {@code null} when no purchase exists
     */
    Purchase findPurchaseById(long purchaseId);

    /**
     * Returns all purchases made by a specific member.
     *
     * @param memberId buyer member identifier
     * @return matching purchases ordered by purchase identifier
     */
    List<Purchase> getPurchasesByMemberId(long memberId);

    /**
     * Returns all purchases associated with a production company.
     *
     * @param companyId company identifier
     * @return matching purchases ordered by purchase identifier
     */
    List<Purchase> getPurchasesByCompanyId(long companyId);

    /**
     * Returns all persisted purchases.
     *
     * @return all purchases ordered by purchase identifier
     */
    List<Purchase> getAllPurchases();

    /**
     * Returns all purchases associated with an event.
     *
     * @param eventId event identifier
     * @return matching purchases ordered by purchase identifier
     */
    List<Purchase> getPurchasesByEventId(long eventId);

    /**
     * Updates an existing purchase.
     * @param purchase purchase to update
     */
    void updatePurchase(Purchase purchase);
}