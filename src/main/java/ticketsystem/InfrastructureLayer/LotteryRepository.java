package ticketsystem.InfrastructureLayer;

import org.springframework.stereotype.Repository;

import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.InfrastructureLayer.persistence.LotteryJpaRepository;

/**
 * JPA-backed repository adapter for lottery aggregates.
 *
 * <p>This class implements the domain repository interface while delegating
 * database operations to Spring Data JPA. The same adapter is used by the
 * application and by persistence tests; only the configured datasource
 * changes.</p>
 */
@Repository
public class LotteryRepository implements ILotteryRepository {

    private final LotteryJpaRepository lotteryJpaRepository;

    /**
     * Creates the repository adapter.
     *
     * @param lotteryJpaRepository Spring Data repository used for persistence
     */
    public LotteryRepository(LotteryJpaRepository lotteryJpaRepository) {
        this.lotteryJpaRepository = lotteryJpaRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Lottery findById(long lotteryId) {
        return lotteryJpaRepository
                .findById(lotteryId)
                .orElse(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLottery(Lottery lottery) {
        if (lottery == null) {
            throw new IllegalArgumentException(
                    "Lottery cannot be null."
            );
        }

        if (lottery.getLotteryId() != null) {
            throw new IllegalArgumentException(
                    "A new lottery must not already have an ID."
            );
        }

        lotteryJpaRepository.saveAndFlush(lottery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(Lottery lottery) {
        if (lottery == null) {
            throw new IllegalArgumentException(
                    "Lottery cannot be null."
            );
        }

        Long lotteryId = lottery.getLotteryId();

        if (lotteryId == null
                || !lotteryJpaRepository.existsById(lotteryId)) {
            throw new IllegalArgumentException(
                    "Lottery does not exist."
            );
        }

        lotteryJpaRepository.saveAndFlush(lottery);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Lottery findByEventId(long eventId) {
        return lotteryJpaRepository
                .findByEventId(eventId)
                .orElse(null);
    }
}