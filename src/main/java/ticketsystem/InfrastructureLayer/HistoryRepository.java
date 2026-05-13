package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.history.Purchase;

public class HistoryRepository implements IHistoryRepository {
    private AtomicLong counter;
    private static HistoryRepository instance;
    private Map<Long, Purchase> allPurchases;
    private Map<Long, List<Purchase>> purchasesByMemberId;
    private Map<Long, List<Purchase>> purchasesByCompanyId;

    public HistoryRepository() {
        this.counter = new AtomicLong(1);
        this.allPurchases = new ConcurrentHashMap<>();
        this.purchasesByMemberId = new ConcurrentHashMap<>();
        this.purchasesByCompanyId = new ConcurrentHashMap<>();
    }

    @Override
    public void addPurchase(Purchase purchase) {
        allPurchases.put(purchase.getPurchaseId(), purchase);

        if (purchase.getMemberId() != null) { //if it is null it is guest purchase and we do not want to add it to the purchasesByMemberId map
            purchasesByMemberId
                    .computeIfAbsent(purchase.getMemberId(), k -> new CopyOnWriteArrayList<>())
                    .add(purchase);//
        }

        purchasesByCompanyId
                .computeIfAbsent(purchase.getCompanyId(), k -> new CopyOnWriteArrayList<>())
                .add(purchase);
    }

    @Override
    public Purchase findPurchaseById(long purchaseId) {
        return allPurchases.get(purchaseId);
    }

    @Override
    public List<Purchase> getPurchasesByMemberId(long memberId) {
        List<Purchase> purchases = purchasesByMemberId.getOrDefault(memberId, new CopyOnWriteArrayList<>());
        return new ArrayList<>(purchases);
    }

    @Override
    public List<Purchase> getPurchasesByCompanyId(long companyId) {
        List<Purchase> purchases = purchasesByCompanyId.getOrDefault(companyId, new ArrayList<>());
        return new ArrayList<>(purchases);
    }

    @Override
    public List<Purchase> getAllPurchases() {
        return new ArrayList<>(allPurchases.values());
    }
    
    @Override
    public long generateNextId() {
        return counter.getAndIncrement(); 
    }
}
