package ticketsystem.DomainLayer.IRepository;

import java.util.List;

import ticketsystem.DomainLayer.history.Purchase;

public interface IHistoryRepository {
    void addPurchase(Purchase purchase); 
    Purchase findPurchaseById(long purchaseId);
    List<Purchase> getPurchasesByMemberId(long memberId);
    List<Purchase> getPurchasesByCompanyId(long companyId);
    List<Purchase> getAllPurchases();
    long generateNextId();
    
}
