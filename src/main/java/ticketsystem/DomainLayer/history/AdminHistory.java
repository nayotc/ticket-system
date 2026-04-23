package ticketsystem.DomainLayer.history;
import ticketsystem.DomainLayer.order.Purchase;
import java.util.List;

public class AdminHistory {
    private int id;

    public AdminHistory(int id) {
        this.id = id;
    }

    public List<Purchase> getHistoryByMembers() {
        return null;
    }

    public List<Purchase> getHistoryByCompanies() {
        return null;
    }

}
