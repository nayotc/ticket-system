package ticketsystem.DomainLayer.IRepository;

import java.util.List;
import java.util.Set;

import ticketsystem.DomainLayer.lottery.Lottery;

/**
 * Repository abstraction for persisting and retrieving lottery aggregates.
 */
public interface ILotteryRepository {

    /**
     * Finds a lottery by its database identifier.
     *
     * @param lotteryId lottery identifier
     * @return matching lottery, or null when no lottery exists
     */
    Lottery findById(long lotteryId);

    /**
     * Persists a new lottery.
     *
     * <p>The database-generated identifier is assigned to the supplied entity
     * during this operation.</p>
     *
     * @param lottery new lottery to persist
     */
    void addLottery(Lottery lottery);

    /**
     * Persists changes made to an existing lottery.
     *
     * @param lottery existing lottery to update
     */
    void update(Lottery lottery);

    /**
     * Finds the lottery associated with the specified event.
     *
     * @param eventId event identifier
     * @return associated lottery, or null when the event has no lottery
     */
    Lottery findByEventId(long eventId);

    Set<Long> findEventIdsWithLottery(List<Long> eventIds);
}