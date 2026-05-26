package ticketsystem.PresentationLayer.Presenters;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.HistoryService;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.SalesReportDTO;

import java.util.List;

/**
 * Presenter that connects the sales report view with the business logic.
 */
@Component
public class SalesReportPresenter {

    private final HistoryService historyService;

    // Dependency injection of the history service from the application layer
    public SalesReportPresenter(HistoryService historyService) {
        this.historyService = historyService;
    }

    public SalesReportDTO generateSalesReport(String token, long companyId) {
        try {
            // Forwarding the request to the Service, which performs the permission validation and data calculation itself
            return historyService.generateSalesReport(token, companyId);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Catching permission or validation errors from the Service and translating them into a PresentationException
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            // Smart protection: unexpected system exceptions are translated into a friendly message that doesn't crash the UI
            throw new PresentationException("An error occurred while generating the summary report. Please try again.");
        }
    }

    public List<OrderDTO> getCompanyTransactions(String token, long companyId) {
        try {
            // Calling the Service to fetch the raw purchase history for the company
            return historyService.getHistoryForCompany(token, companyId);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Similar to the previous method, converting logic errors into an exception that the View understands
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            // Handling a general error to protect the screen
            throw new PresentationException("An error occurred while loading the company transaction history.");
        }
    }
}