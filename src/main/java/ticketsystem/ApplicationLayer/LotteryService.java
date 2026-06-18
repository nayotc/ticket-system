package ticketsystem.ApplicationLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.lottery.LotteryStatus;
import ticketsystem.DomainLayer.user.Permission;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LotteryService {

    private final ILotteryRepository lotteryRepository;
    private final ITokenService tokenService;
    private final INotifier notificationsService;
    private final UserAccessService userAccessService;
    private final MembershipDomainService membershipDomainService;
    private final ISystemLogger logger;

    public LotteryService(
            ILotteryRepository lotteryRepository,
            ITokenService tokenService,
            INotifier notificationsService, UserAccessService userAccessService, MembershipDomainService membershipDomainService, ISystemLogger logger
    ) {
        this.lotteryRepository = lotteryRepository;
        this.tokenService = tokenService;
        this.notificationsService = notificationsService;
        this.userAccessService = userAccessService;
        this.membershipDomainService = membershipDomainService;
        this.logger = logger;
    }

    /**
     * Creates and persists a new purchase lottery for an event.
     *
     * <p>The lottery identifier is assigned by the database during persistence.
     * Only one lottery may be associated with a specific event.</p>
     *
     * @param token         active member token
     * @param eventId       event associated with the lottery
     * @param companyId     company that manages the event
     * @param winnersNumber number of winners to select
     * @return database-generated lottery identifier
     */
    public long addLottery(
            String token,
            long eventId,
            long companyId,
            int winnersNumber
    ) {
        try {
            tokenService.validateToken(token);

            if (!tokenService.isMemberToken(token)) {
                throw new IllegalArgumentException(
                        "Only members can add lotteries"
                );
            }

            Long memberId = tokenService.extractUserId(token);

            if (memberId == null) {
                throw new IllegalArgumentException(
                        "Could not extract user id from token"
                );
            }

            if (!membershipDomainService.validatePermission(
                    memberId,
                    companyId,
                    Permission.MANAGE_EVENT_INVENTORY
            )) {
                throw new IllegalArgumentException(
                        "Insufficient permissions to add lottery"
                );
            }

            userAccessService.validateCanPerformNonViewAction(memberId);

            if (eventId <= 0) {
                throw new IllegalArgumentException(
                        "Event ID must be positive."
                );
            }

            if (winnersNumber <= 0) {
                throw new IllegalArgumentException(
                        "Number of winners must be greater than zero."
                );
            }

            if (lotteryRepository.findByEventId(eventId) != null) {
                throw new IllegalArgumentException(
                        "A lottery already exists for this event."
                );
            }

            Lottery lottery = new Lottery(eventId, winnersNumber);

            lotteryRepository.addLottery(lottery);

            Long generatedLotteryId = lottery.getLotteryId();

            if (generatedLotteryId == null) {
                throw new IllegalStateException(
                        "Lottery ID was not generated after persistence."
                );
            }

            logger.logEvent(
                    "Completed - Add Lottery. lotteryId="
                            + generatedLotteryId
                            + ", eventId=" + eventId
                            + ", winnerNumber=" + winnersNumber,
                    ISystemLogger.LogLevel.INFO
            );

            return generatedLotteryId;

        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.logEvent(
                    "Failed to add lottery: " + e.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );
            throw e;
        }
    }

    // Method to register a member for a lottery
    public boolean registerMemberToLottery(String token, long lotteryId) {
        try {
            tokenService.validateToken(token);
            long memberId = tokenService.extractUserId(token);
            Lottery lottery = lotteryRepository.findById(lotteryId);
            userAccessService.validateCanPerformNonViewAction(memberId);
            if (lottery == null) {
                throw new IllegalArgumentException("Lottery with ID " + lotteryId + " not found.");
            }
            lottery.registerMember(memberId);
            lotteryRepository.update(lottery);
            notificationsService.notifyMember(
                    memberId,
                    "You have successfully registered for the lottery!"
            );
            return true;
        } catch (IllegalArgumentException e) {
            logger.logEvent(
                    "Failed to register member to lottery: " + e.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );
            throw (e);
        }

    }

    // Method to close lottery registration
    public boolean closeLotteryRegistration(String token, long lotteryId, long companyId) {
        try {
            tokenService.validateToken(token);
            Long memberId = tokenService.extractUserId(token);
            if (memberId == null) {
                throw new IllegalArgumentException("Could not extract user id from token");
            }
            if (!membershipDomainService.validatePermission(memberId, companyId, Permission.MANAGE_EVENT_INVENTORY)) {
                throw new IllegalArgumentException("Insufficient permissions to close lottery registration");
            }
            userAccessService.validateCanPerformNonViewAction(memberId);
            //need to validate that the user has permission to close the lottery registration
            Lottery lottery = lotteryRepository.findById(lotteryId);
            if (lottery == null) {
                throw new IllegalArgumentException("Lottery with ID " + lotteryId + " not found.");
            }
            lottery.setStatus(LotteryStatus.CLOSED);
            lotteryRepository.update(lottery);
            return true;
        } catch (IllegalArgumentException e) {
            logger.logEvent(
                    "Failed to close lottery registration: " + e.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );
            throw (e);
        }
    }

    // Method to conduct the lottery draw and select winners
    public boolean conductLotteryDraw(String token, long lotteryId, long companyId) {
        try {
            tokenService.validateToken(token);
            Long member = tokenService.extractUserId(token);
            if (member == null) {
                throw new IllegalArgumentException("Could not extract user id from token");
            }
            if (!membershipDomainService.validatePermission(member, companyId, Permission.MANAGE_EVENT_INVENTORY)) {
                throw new IllegalArgumentException("Insufficient permissions to conduct lottery draw");
            }
            userAccessService.validateCanPerformNonViewAction(member);
            //need to validate that the user has permission to conduct the lottery draw 
            Lottery lottery = lotteryRepository.findById(lotteryId);
            if (lottery == null) {
                throw new IllegalArgumentException("Lottery with ID " + lotteryId + " not found.");
            }
            if (lottery.getStatus() != LotteryStatus.CLOSED) {
                throw new IllegalArgumentException("Lottery with ID " + lotteryId + " is not closed yet. Please close the lottery registration before conducting the draw.");
            }
            List<Long> allParticipants = lottery.getRegisteredMemberIds();
            List<Long> winningMemberIds = selectRandomWinners(allParticipants, lottery.getWinnersNumber());

            for (long memberId : allParticipants) {
                if (winningMemberIds.contains(memberId)) {
                    //winning member
                    String uniqueCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    lottery.setWinner(memberId, uniqueCode);
                    notificationsService.notifyMember(
                            memberId,
                            "Congratulations! You won the purchase lottery. Your purchase code is: " + uniqueCode + "."
                    );
                } else {
                    //non-winning member
                    notificationsService.notifyMember(
                            memberId,
                            "Thank you for participating in the lottery. Unfortunately, you did not win this time. Better luck next time!"
                    );
                }
            }
            lottery.setStatus(LotteryStatus.COMPLETED);
            lotteryRepository.update(lottery);
            return true;
        } catch (IllegalArgumentException e) {
            logger.logEvent(
                    "Failed to conduct lottery draw: " + e.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );
            throw (e);
        }
    }

    //method for tests
    @Transactional(readOnly = true)
    public List<Long> getWinners(long lotteryId) {
        Lottery lottery = lotteryRepository.findById(lotteryId);
        if (lottery == null) {
            throw new IllegalArgumentException("Lottery with ID " + lotteryId + " does not exist.");
        }
        return lottery.getWinners();
    }

    // Helper method to select random winners from the list of registered members
    private List<Long> selectRandomWinners(List<Long> allRegisteredMembers, int numberOfWinners) {
        if (allRegisteredMembers == null || allRegisteredMembers.isEmpty() || numberOfWinners <= 0) {
            return new ArrayList<>();
        }

        if (allRegisteredMembers.size() <= numberOfWinners) {
            return new ArrayList<>(allRegisteredMembers);
        }
        // Shuffle the list of registered members and select the top N winners
        List<Long> winnersPool = new ArrayList<>(allRegisteredMembers);
        Collections.shuffle(winnersPool);

        return new ArrayList<>(winnersPool.subList(0, numberOfWinners));
    }

    /**
     * Checks whether a lottery exists for the given event.
     *
     * This is used by the UI when displaying event cards, so it only checks
     * whether a lottery is attached to the event and does not expose lottery
     * details.
     *
     * @param token active session token
     * @param eventId event id to check
     * @return true if a lottery exists for the event, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasLotteryForEvent(String token, long eventId) {
        tokenService.validateToken(token);

        if (eventId <= 0) {
            return false;
        }

        return lotteryRepository.findByEventId(eventId) != null;
    }

    @Transactional(readOnly = true)
    public long getLotteryIdByEventId(long eventId) {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Event ID is invalid.");
        }

        Lottery lottery = lotteryRepository.findByEventId(eventId);

        if (lottery == null) {
            throw new IllegalArgumentException("Lottery for event not found.");
        }

        return lottery.getLotteryId();
    }

    public boolean registerMemberToLotteryByEventId(String token, long eventId) {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Event ID is invalid.");
        }

        Lottery lottery = lotteryRepository.findByEventId(eventId);

        if (lottery == null) {
            throw new IllegalArgumentException("Lottery for event not found.");
        }

        return registerMemberToLottery(token, lottery.getLotteryId());
    }

    @Transactional(readOnly = true)
    public boolean validateWinnerCodeForEvent(String token, long eventId, String authCode) {
        try {
            tokenService.validateToken(token);

            Long memberId = tokenService.extractUserId(token);
            if (memberId == null) {
                throw new IllegalArgumentException("Member must be logged in to use a lottery code.");
            }

            userAccessService.validateCanPerformNonViewAction(memberId);

            if (eventId <= 0) {
                throw new IllegalArgumentException("Event ID is invalid.");
            }

            if (authCode == null || authCode.isBlank()) {
                return false;
            }

            Lottery lottery = lotteryRepository.findByEventId(eventId);
            if (lottery == null) {
                throw new IllegalArgumentException("Lottery for event not found.");
            }

            return lottery.validateWinnerCode(memberId, authCode.trim());

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        }
    }

}
