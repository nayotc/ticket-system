package ticketsystem.ApplicationLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Collections;
import ticketsystem.DomainLayer.IRepository.ILotteryRepository;
import ticketsystem.DomainLayer.lottery.Lottery;
import ticketsystem.DomainLayer.lottery.LotteryStatus;

public class LotteryService {
    
    private final ILotteryRepository lotteryRepository;
    private final ITokenService tokenService;
    //private final NotificationsService notificationsService;
    //private IEventRepository eventRepository;
    private ISystemLogger logger;

    public LotteryService(ILotteryRepository lotteryRepository, ITokenService tokenService, 
                              ISystemLogger logger) {
        this.lotteryRepository = lotteryRepository;  
        this.tokenService = tokenService; 
        //this.notificationsService = notificationsService;
        //this.eventRepository = eventRepository;
        this.logger = logger;
    }

    // Method to create a new lottery
    public long addLottery(String token, long eventId, long companyId, int winnersNumber) {
        try {
            tokenService.validateToken(token);
            if (winnersNumber <= 0) {
                throw new IllegalArgumentException("Number of winners must be greater than zero.");
            }
            if(!tokenService.isMemberToken(token)){
                throw new IllegalArgumentException("Only members can add lottery to event");
            }
            // TODO:
            //need to ask to add func that check if eventid for this companyid or maybe to take companyid from there
            //if(eventRepository.getEventById(eventId)==null){
            //    throw new IllegalArgumentException("there is no such event");
            //}
            Long memberId = tokenService.extractUserId(token);
            if (memberId == null){
                throw new IllegalArgumentException("Could not extract user id from token");
            }
            // TODO:
            //add a check if memberID is assign to this company

            long lotteryId = lotteryRepository.generateNextLotteryId();
            lotteryRepository.addLottery(new Lottery(lotteryId, eventId, winnersNumber));
            return lotteryId;

        } catch (IllegalArgumentException e) {
            logger.logEvent(
                    "Failed to add lottery: " + e.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );
            throw e;
        }

    }

    // Method to register a member for a lottery
    public boolean registerMemberToLottery(String token, long lotteryId) {
        try{        
            tokenService.validateToken(token);
            if(!tokenService.isMemberToken(token)){
                throw new IllegalArgumentException("Only members can register to lottery");
            }
            long memberId = tokenService.extractUserId(token);
            Lottery lottery =lotteryRepository.findById(lotteryId);
            if (lottery == null) {
                throw new IllegalArgumentException("Lottery with ID " + lotteryId + " not found.");
            }
            lottery.registerMember(memberId);
            lotteryRepository.update(lottery);
            //notificationsService.notifyUser(token, "You have successfully registered for the lottery!");
            return true; 
        } catch(IllegalArgumentException e){
            logger.logEvent(
                    "Failed to register Member To Lottery: " + e.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );
            throw e;
        }

    }

    // Method to close lottery registration
    public boolean closeLotteryRegistration(String token, long lotteryId) {
        try{
            tokenService.validateToken(token);
            Lottery lottery = lotteryRepository.findById(lotteryId);
            if (lottery == null) {
                throw new IllegalArgumentException("Lottery with ID " + lotteryId + " not found.");
            }
            lottery.setStatus(LotteryStatus.CLOSED);
            lotteryRepository.update(lottery);
            return true;
        }
        catch(IllegalArgumentException e){
            logger.logEvent(
                    "Failed to close Lottery Registration: " + e.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );
            throw e;
        }
    }

    // Method to conduct the lottery draw and select winners
    public boolean conductLotteryDraw(String token, long lotteryId) {
        try{
            tokenService.validateToken(token);
            // TODO:
            //need to validate that the user has permission to conduct the lottery draw 
            Lottery lottery = lotteryRepository.findById(lotteryId);
            if (lottery == null) {
                throw new IllegalArgumentException("Lottery with ID " + lotteryId + " not found.");
            }
            if(lottery.getStatus() != LotteryStatus.CLOSED) {
                throw new IllegalArgumentException("Lottery with ID " + lotteryId + " is not closed yet. Please close the lottery registration before conducting the draw.");
            }
            List<Long> allParticipants = lottery.getRegisteredMemberIds();
            List<Long> winningMemberIds = selectRandomWinners(allParticipants, lottery.getWinnersNumber());

        for (long memberId : allParticipants) {
                if (winningMemberIds.contains(memberId)) {
                    //winning member
                    String uniqueCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                    lottery.setWinner(memberId, uniqueCode);
                    //notificationsService.notify(memberId, "Congratulations! You won. Your code is: " + uniqueCode);
                } else {
                //non-winning member
                    //notificationService.sendMessage(memberId, "We are sorry, you were not selected in the lottery.");
                }
            }
            lottery.setStatus(LotteryStatus.COMPLETED);
            lotteryRepository.update(lottery);
            return true;
        }
        catch(IllegalArgumentException e){
            logger.logEvent(
                    "Failed to conduct Lottery Draw: " + e.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );
            throw e;
        }
    }

    //method for tests
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
}
