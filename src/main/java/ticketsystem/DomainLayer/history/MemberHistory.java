package ticketsystem.DomainLayer.history;

import ticketsystem.DomainLayer.order.Purchase;
import ticketsystem.DomainLayer.order.Ticket;
import java.util.List;

public class MemberHistory {
    private int id;

    public MemberHistory(int id) {
        this.id = id;
    }
    public List<Ticket> getHistoryTickets() {
        return null;
    }

    public void addHistoryTicket(Ticket ticket) {
    }

    public void removeHistoryTicket(Ticket ticket) {
    }

    public List<Purchase> getHistoryPurchases() {
        return null;
    }

    public void addHistoryPurchase(Purchase purchase) {
    }

    public void removeHistoryPurchase(Purchase purchase) {
    }

}
