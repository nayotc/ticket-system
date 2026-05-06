package ticketsystem.ApplicationLayer;

import java.util.ArrayList;
import java.util.List;

import ticketsystem.ApplicationLayer.Events.OrderCompletedListener;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.history.Purchase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class HistoryService implements OrderCompletedListener {
    private final IHistoryRepository historyRepository;
    private final TokenService tokenService;

    public HistoryService(IHistoryRepository historyRepository, TokenService tokenService) {
        this.historyRepository = historyRepository;
        this.tokenService = tokenService;
    }
    
    @Override
    public void onOrderCompleted(OrderDTO order) {
        try{
            //we don't need to validate the token here because this method is called after the order is completed, and we assume that the order completion process has already validated the token. However, if you want to add an extra layer of security, you can validate the token here as well before processing the order details.
            int newPurchaseId = historyRepository.generateNextId();
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
            int memberId = Integer.parseInt(tokenService.extractSubject(token));
            List<Purchase> purchases = historyRepository.getPurchasesByMemberId(memberId);
            ObjectMapper objectMapper = new ObjectMapper();
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



}
