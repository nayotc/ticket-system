package ticketsystem.ApplicationLayer;

import java.util.ArrayList;
import java.util.List;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.TicketDTO;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.history.PurchasedTicket;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

public class HistoryService {
    private final IHistoryRepository historyRepository;
    private final TokenService tokenService;

    public HistoryService(IHistoryRepository historyRepository, TokenService tokenService) {
        this.historyRepository = historyRepository;
        this.tokenService = tokenService;
    }
    
    public void addPurchase(OrderDTO order, String token) {
        try{
            // Validate token
            if (!tokenService.validateToken(token)) {
                throw new IllegalArgumentException("Invalid or expired token");
            }
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
