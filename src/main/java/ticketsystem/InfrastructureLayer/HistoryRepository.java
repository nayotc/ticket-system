package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import ticketsystem.DomainLayer.IRepository.IHistoryRepository;
import ticketsystem.DomainLayer.history.Purchase;

public class HistoryRepository implements IHistoryRepository {
    private AtomicInteger counter;
    private static HistoryRepository instance;
    private Map<Integer, Purchase> allPurchases;
    private Map<Long, List<Purchase>> purchasesByMemberId;
    private Map<Integer, List<Purchase>> purchasesByCompanyId;

    private HistoryRepository() {
        this.counter = new AtomicInteger(1);
        this.allPurchases = new ConcurrentHashMap<>();
        this.purchasesByMemberId = new ConcurrentHashMap<>();
        this.purchasesByCompanyId = new ConcurrentHashMap<>();
    }

    public static HistoryRepository getInstance() {
        if (instance == null) {
            instance = new HistoryRepository();
        }
        return instance;
    }

    @Override
    public void addPurchase(Purchase purchase) {
        allPurchases.put(purchase.getPurchaseId(), purchase);
        purchasesByMemberId.computeIfAbsent(purchase.getMemberId(), k -> new CopyOnWriteArrayList<>())
                       .add(purchase);
        purchasesByCompanyId.computeIfAbsent(purchase.getCompanyId(), k -> new CopyOnWriteArrayList<>())
                            .add(purchase);
    }

    @Override
    public Purchase findPurchaseById(int purchaseId) {
        return allPurchases.get(purchaseId);
    }

    @Override
    public List<Purchase> getPurchasesByMemberId(long memberId) {
        List<Purchase> purchases = purchasesByMemberId.getOrDefault(memberId, new CopyOnWriteArrayList<>());
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
    
    @Override
    public int generateNextId() {
        return counter.getAndIncrement(); 
    }
}
