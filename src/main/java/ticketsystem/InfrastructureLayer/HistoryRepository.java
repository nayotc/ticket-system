package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.order.Purchase;

public class HistoryRepository implements IHistoryRepository {
    private int counter;
    private static HistoryRepository instance;
    private Map<Integer, Purchase> allPurchases;
    private Map<Integer, List<Purchase>> purchasesByMemberId;
    private Map<Integer, List<Purchase>> purchasesByCompanyId;

    private HistoryRepository() {
        this.counter = 1;
        this.allPurchases = new ConcurrentHashMap<Integer, Purchase>();
        this.purchasesByMemberId = new ConcurrentHashMap<Integer, List<Purchase>>();
        this.purchasesByCompanyId = new ConcurrentHashMap<Integer, List<Purchase>>();
    }

    public static HistoryRepository getInstance() {
        if (instance == null) {
            instance = new HistoryRepository();
        }
        return instance;
    }

    @Override
    public void addPurchase(Purchase purchase) {
        purchase.setId(counter);
        allPurchases.put(counter, purchase);
        counter++;
        purchasesByMemberId.computeIfAbsent(purchase.getMemberId(), k -> new ArrayList<>())
                       .add(purchase);
        purchasesByCompanyId.computeIfAbsent(purchase.getCompanyId(), k -> new ArrayList<>())
                            .add(purchase);
    }

    @Override
    public Purchase findPurchaseById(int purchaseId) {
        return allPurchases.get(purchaseId);
    }

    @Override
    public List<Purchase> getPurchasesByMemberId(int memberId) {
        List<Purchase> purchases = purchasesByMemberId.getOrDefault(memberId, new ArrayList<>());
        return new ArrayList<>(purchases);
    }

    @Override
    public List<Purchase> getPurchasesByCompanyId(int companyId) {
        List<Purchase> purchases = purchasesByCompanyId.getOrDefault(companyId, new ArrayList<>());
        return new ArrayList<>(purchases);
    }

    @Override
    public List<Purchase> getAllPurchases() {
        return new ArrayList<>(allPurchases.values());
    }
    
}
