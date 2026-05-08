package ticketsystem.DomainLayer.IRepository;

import java.util.List;

import ticketsystem.DomainLayer.history.Purchase;

public interface IHistoryRepository {
    void addPurchase(Purchase purchase); 
    Purchase findPurchaseById(int purchaseId);
    List<Purchase> getPurchasesByMemberId(long memberId);
    List<Purchase> getPurchasesByCompanyId(int companyId);
    List<Purchase> getAllPurchases();
    int generateNextId();
    
}
