package ticketsystem.ApplicationLayer;

import java.util.ArrayList;
import java.util.List;

import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.TicketDTO;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.order.Purchase;
import ticketsystem.DomainLayer.order.Ticket;

public class HistoryService {
    private final IHistoryRepository historyRepository;

    public HistoryService(IHistoryRepository historyRepository){
        this.historyRepository = historyRepository;
    }
    
    public void addPurchase(OrderDTO order) {
        List<Ticket> tickets = new ArrayList<>();
        
        for (TicketDTO tDto : order.getTickets()) {
            Ticket ticket = new Ticket(
                tDto.getTicketId(),
                tDto.getEventId(),
                tDto.getRow(),
                tDto.getChair(),
                tDto.getPrice()
            );
        
        ticket.setStatusToActive();;
        
        tickets.add(ticket);
    }
        Purchase purchase = new Purchase(order.getOrderId(), tickets, order.getEventName(), order.getLocation(), order.getMemberId(), order.getCompanyId());
        historyRepository.addPurchase(purchase);
    }

    public List<OrderDTO> getHistoryForUser(int memberId) {
        List<Purchase> purchases = historyRepository.getPurchasesByMemberId(memberId);
        List<OrderDTO> historyDtoList = new ArrayList<>();
        for (Purchase p : purchases) {
            List<TicketDTO> ticketDtos = new ArrayList<>();
            for (Ticket t : p.getTickets()) {
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



}
