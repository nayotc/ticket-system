package ticketsystem.PresentationLayer.Presenters;

import java.util.List;
import java.io.InputStream;
import java.math.BigDecimal;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.HistoryService;
import ticketsystem.ApplicationLayer.UserService;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DTO.OrderDTO;
import ticketsystem.DTO.SalesReportDTO;

/**
 * Presenter that connects the sales report view with the business logic.
 */
@Component
public class SalesReportPresenter {

    private final HistoryService historyService;
    private final UserService userService;

    // Dependency injection of the history service from the application layer
    public SalesReportPresenter(HistoryService historyService, UserService userService) {
        this.historyService = historyService;
        this.userService = userService;
    }

    public SalesReportDTO generateSalesReport(String token, long companyId) {
        try {
            // Forwarding the request to the Service, which performs the permission validation and data calculation itself
            return historyService.generateSalesReport(token, companyId);
            
        } catch (PresentationException e) {
            throw e;

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
            return historyService.getManagerFilteredHistoryForCompany(token, companyId);
            
        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Similar to the previous method, converting logic errors into an exception that the View understands
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            // Handling a general error to protect the screen
            throw new PresentationException("An error occurred while loading the company transaction history.");
        }
    }

    public InputStream exportTransactionsToCsv(String token, long companyId) {
        try {
            List<OrderDTO> transactions = historyService.getManagerFilteredHistoryForCompany(token, companyId);
            
            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append('\ufeff'); // קידוד לעברית באקסל
            csvBuilder.append("מספר עסקה,שם אירוע,מיקום,שם רוכש,סכום לתשלום\n");
            
            for (OrderDTO order : transactions) {
                BigDecimal totalAmount = order.getTickets().stream()
                    .map(ticketsystem.DTO.PurchaseDTO::getPrice)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
                String buyerName = "משתמש לא ידוע";
                try {
                    if (order.getMemberId() != null) {
                        var userName = getBuyerName(order.getMemberId());
                        if (userName != null) {
                            buyerName = userName;
                        }
                    }
                } catch (Exception ignored) {}
                
                csvBuilder.append(order.getPurchaseId()).append(",")
                          .append(escapeCsv(order.getEventName())).append(",")
                          .append(escapeCsv(order.getLocation())).append(",")
                          .append(escapeCsv(buyerName)).append(",")
                          .append(totalAmount).append("\n");
            }
            
            return new ByteArrayInputStream(csvBuilder.toString().getBytes(StandardCharsets.UTF_8));
            
        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(e.getMessage());
        } catch (Exception e) {
            throw new PresentationException("An error occurred while generating the CSV export file.");
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",")) {
            return "\"" + value + "\"";
        }
        return value;
    }

    public String getBuyerName(Long memberId) {
        if (memberId == null) {
            return "משתמש לא ידוע";
        }
        try {
            Member buyer = userService.getMemberById(memberId);
            if (buyer != null && buyer.getFullName() != null && !buyer.getFullName().isBlank()) {
                return buyer.getFullName();
            }
            return "משתמש לא ידוע";
        } catch (Exception e) {
            return "משתמש לא ידוע";
        }
    }
}