package ticketsystem.ApplicationLayer;

import java.util.ArrayList;
import java.util.List;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.TicketDTO;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.history.Purchase;
import ticketsystem.DomainLayer.history.PurchasedTicket;

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
            List<PurchasedTicket> tickets = new ArrayList<>();
            for (TicketDTO tDto : order.getTickets()) {
                PurchasedTicket ticket = new PurchasedTicket(
                    tDto.getTicketId(),
                    tDto.getEventId(),
                    tDto.getRow(),
                    tDto.getChair(),
                    tDto.getPrice()
                );
            tickets.add(ticket);
            }
            Purchase purchase = new Purchase(order.getOrderId(), tickets, order.getEventName(), order.getLocation(), order.getMemberId(), order.getCompanyId());
            historyRepository.addPurchase(purchase);
        } 
        catch (IllegalArgumentException e) {
            throw e;
        }
    }

    public List<OrderDTO> getHistoryForUser(int memberId, String token) {
        try{
            // Validate token
            if (!tokenService.validateToken(token)) {
                throw new IllegalArgumentException("Invalid or expired token");
            }
            List<Purchase> purchases = historyRepository.getPurchasesByMemberId(memberId);
            List<OrderDTO> historyDtoList = new ArrayList<>();
            for (Purchase p : purchases) {
                List<TicketDTO> ticketDtos = new ArrayList<>();
                for (PurchasedTicket t : p.getTickets()) {
                    ticketDtos.add(new TicketDTO(
                        t.getTicketId(),
                        t.getEventId(),
                        t.getRow(),
                        t.getChair(),
                        t.getPrice(),
                        t.getStatus().name()
                    ));
                }

                OrderDTO orderDto = new OrderDTO(
                    p.getId(),          
                    ticketDtos,         
                    p.getEventName(),   
                    p.getLocation(),    
                    p.getMemberId(),   
                    p.getCompanyId()
                );
                historyDtoList.add(orderDto);
            }
            return historyDtoList;
        }
        catch (IllegalArgumentException e) {
            throw e;
        }
    }



}
