package ticketsystem.InfrastructureLayer.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ticketsystem.DomainLayer.lottery.Lottery;

/**
 * Spring Data JPA repository for lottery aggregate persistence.
 *
 * <p>The registration collection is loaded together with the lottery because
 * the application and domain layers immediately use participant, winner and
 * authentication-code information after repository lookup.</p>
 */
public interface LotteryJpaRepository extends JpaRepository<Lottery, Long> {

    /**
     * Loads a lottery and all of its registrations by lottery identifier.
     *
     * @param lotteryId lottery identifier
     * @return matching lottery
     */
    @Override
    @EntityGraph(attributePaths = "registrations")
    Optional<Lottery> findById(Long lotteryId);

    /**
     * Loads a lottery and all of its registrations by event identifier.
     *
     * @param eventId event identifier
     * @return lottery associated with the event
     */
    @EntityGraph(attributePaths = "registrations")
    Optional<Lottery> findByEventId(Long eventId);

    
        @Query("""
        select l.eventId
        from Lottery l
        where l.eventId in :eventIds
        """)
    Set<Long> findEventIdsWithLottery(@Param("eventIds") List<Long> eventIds);
}