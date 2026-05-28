package ticketsystem.ApplicationLayer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import ticketsystem.ApplicationLayer.Events.OrderCompletedListener;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.SalesReportDTO;
import ticketsystem.DomainLayer.MembershipDomainService;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.history.PurchasedTicket;
import ticketsystem.DomainLayer.history.TicketStatus;
import ticketsystem.DomainLayer.user.Permission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class HistoryService implements OrderCompletedListener {
    private final IHistoryRepository historyRepository;
    private final ITokenService tokenService;
    private ObjectMapper objectMapper = new ObjectMapper();
    private MembershipDomainService membershipDomainService;
    private ISystemLogger logger;
    private final UserAccessService userAccessService; 

    public HistoryService(IHistoryRepository historyRepository, ITokenService tokenService, MembershipDomainService membershipDomainService, ISystemLogger logger,UserAccessService userAccessService) {
        this.historyRepository = historyRepository;
        this.tokenService = tokenService;
        this.membershipDomainService = membershipDomainService;
        this.logger = logger;
        this.userAccessService=userAccessService;
}

    
    @Override
    // This method is called when an order is completed. It takes the order details, converts them into a Purchase object, and stores it in the history repository.
    public void onOrderCompleted(OrderDTO order) {
        try{
            //we don't need to validate the token here because this method is called after the order is completed, and we assume that the order completion process has already validated the token. However, if you want to add an extra layer of security, you can validate the token here as well before processing the order details.
            long newPurchaseId = historyRepository.generateNextId();
            order.setPurchaseId(newPurchaseId);
            ObjectMapper objectMapper = new ObjectMapper();
            Purchase purchase = objectMapper.convertValue(order, Purchase.class);
            historyRepository.addPurchase(purchase);     //purchase is the object after you pay 
        } 
        catch (IllegalArgumentException e) {
            throw e;
        }
    }

    public List<OrderDTO> getHistoryForUser(String token) {
        try{
            // Validate token
            if (!tokenService.validateToken(token)) {
                throw new IllegalArgumentException("Invalid or expired token");
            }

            if(!tokenService.isMemberToken(token)){
                throw new IllegalArgumentException("Only members can view personal purchase history");
            }

            Long memberId = tokenService.extractUserId(token);
            if (memberId == null){
                throw new IllegalArgumentException("Could not extract user id from token");
            }

            List<Purchase> purchases = historyRepository.getPurchasesByMemberId(memberId);
            if(purchases.isEmpty()){
                //notification
            }
            List<OrderDTO> historyDtoList = objectMapper.convertValue(
                purchases, 
                new TypeReference<List<OrderDTO>>() {}
            );
            return historyDtoList;
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
    }
    
    // This method retrieves the purchase history for a specific company. It validates the token, checks permissions, and then fetches the purchase history from the repository.
    public List<OrderDTO> getHistoryForCompany(String token, long companyId) {
        try{
            // Validate token
            if (!tokenService.validateToken(token)) {
                throw new IllegalArgumentException("Invalid or expired token");
            }

            if(!tokenService.isMemberToken(token)){
                throw new IllegalArgumentException("Only members can view personal purchase history");
            }

            Long memberId = tokenService.extractUserId(token);
            if (memberId == null){
                throw new IllegalArgumentException("Could not extract user id from token");
            }
            if(!membershipDomainService.validatePermission(memberId, companyId, Permission.VIEW_PURCHASE_HISTORY)) {
                throw new IllegalArgumentException("Insufficient permissions to view company purchase history");
            }
            List<Purchase> purchases = historyRepository.getPurchasesByCompanyId(companyId);
            if(purchases.isEmpty()){
                //notification
            }
            List<OrderDTO> historyDtoList = objectMapper.convertValue(
                purchases, 
                new TypeReference<List<OrderDTO>>() {}
            );
            return historyDtoList;
        }
        catch (IllegalArgumentException e) {
            logger.logEvent("Failed to retrieve company purchase history: " + e.getMessage(), ISystemLogger.LogLevel.WARN);
            throw e;
        }
    }
    public SalesReportDTO generateSalesReport(String token, long companyId) {
        try {
            if (!tokenService.validateToken(token)) {
                throw new IllegalArgumentException("Invalid or expired token");
            }

            if (!tokenService.isMemberToken(token)) {
                throw new IllegalArgumentException("Only members can generate sales reports");
            }

            Long memberId = tokenService.extractUserId(token);
            if (memberId == null) {
                throw new IllegalArgumentException("Could not extract user id from token");
            }
            userAccessService.validateCanPerformNonViewAction(memberId);
            if (!membershipDomainService.validatePermission(
                    memberId,
                    companyId,
                    Permission.GENERATE_SALES_REPORT
            )) {
                throw new IllegalArgumentException("Insufficient permissions to generate sales report");
            }

            Set<Long> allowedManagers =
                    membershipDomainService.getManagementSubTreeMemberIds(memberId, companyId);

            List<Purchase> companyPurchases =
                    historyRepository.getPurchasesByCompanyId(companyId);

            int totalTicketsSold = 0;
            BigDecimal totalRevenue = BigDecimal.ZERO;

            for (Purchase purchase : companyPurchases) {
                if (!allowedManagers.contains(purchase.getManagedByMemberId())) {
                    continue;
                }

                List<PurchasedTicket> tickets = purchase.getTickets();

                for (PurchasedTicket ticket : tickets) {
                    if(!ticket.getStatus().equals(TicketStatus.CANCELED) ){
                        totalRevenue = totalRevenue.add(BigDecimal.valueOf(ticket.getPrice()));
                        totalTicketsSold++;

                    }
                }
            }

            if (totalTicketsSold == 0) {
                return new SalesReportDTO(
                        0,
                        BigDecimal.ZERO,
                        "No sales data was found"
                );
            }

            return new SalesReportDTO(
                    totalTicketsSold,
                    totalRevenue,
                    "Sales report generated successfully"
            );

        } catch (IllegalArgumentException e) {
            logger.logEvent(
                    "Failed to generate sales report: " + e.getMessage(),
                    ISystemLogger.LogLevel.WARN
            );
            throw e;
        }
    }

    public void onEventCanceled(Long eventId) {
        List<Purchase> purchases =
                historyRepository.getAllPurchases();
        List<PurchasedTicket> purchasedTickets; 

        for (Purchase purchase : purchases) {
            if (purchase.getEventId().equals(eventId)) {
                purchasedTickets =purchase.getTickets();
                for(PurchasedTicket ticket : purchasedTickets){
                    ticket.setStatus(TicketStatus.CANCELED);
                }
            }
        }
    }

}
