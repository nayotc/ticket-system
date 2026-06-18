package ticketsystem.InfrastructureLayer.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ticketsystem.DomainLayer.history.Purchase;

/**
 * Spring Data repository for persisted purchase-history aggregates.
 *
 * <p>Every query loads the ticket snapshots together with the purchase. This
 * keeps each {@link Purchase} aggregate complete when it leaves the repository
 * and avoids lazy-loading failures when application services inspect its
 * tickets.</p>
 */
@Repository
public interface HistoryJpaRepository
        extends JpaRepository<Purchase, Long> {

    /**
     * Finds one purchase together with its purchased-ticket snapshots.
     *
     * @param purchaseId purchase identifier
     * @return matching purchase
     */
    @Override
    @EntityGraph(attributePaths = "tickets")
    Optional<Purchase> findById(Long purchaseId);

    /**
     * Finds all purchases made by a member.
     *
     * @param memberId buyer member identifier
     * @return matching purchases
     */
    @EntityGraph(attributePaths = "tickets")
    List<Purchase> findAllByMemberIdOrderByPurchaseIdAsc(
            Long memberId
    );

    /**
     * Finds all purchases associated with a company.
     *
     * @param companyId company identifier
     * @return matching purchases
     */
    @EntityGraph(attributePaths = "tickets")
    List<Purchase> findAllByCompanyIdOrderByPurchaseIdAsc(
            Long companyId
    );

    /**
     * Finds all purchases associated with an event.
     *
     * @param eventId event identifier
     * @return matching purchases
     */
    @EntityGraph(attributePaths = "tickets")
    List<Purchase> findAllByEventIdOrderByPurchaseIdAsc(
            Long eventId
    );

    /**
     * Finds all purchases in deterministic identifier order.
     *
     * @return all persisted purchases
     */
    @EntityGraph(attributePaths = "tickets")
    List<Purchase> findAllByOrderByPurchaseIdAsc();
}