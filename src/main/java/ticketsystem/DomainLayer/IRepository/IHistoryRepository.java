package ticketsystem.DomainLayer.IRepository;

import java.util.List;

import ticketsystem.DomainLayer.order.Purchase;

public interface IHistoryRepository {
    void addPurchase(Purchase purchase); 
    Purchase findPurchaseById(int purchaseId);
    List<Purchase> getPurchasesByMemberId(int memberId);
    List<Purchase> getPurchasesByCompanyId(int companyId);
    List<Purchase> getAllPurchases();
    
}
