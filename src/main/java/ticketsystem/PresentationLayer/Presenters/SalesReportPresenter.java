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

    public SalesReportPresenter(HistoryService historyService, UserService userService) {
        this.historyService = historyService;
        this.userService = userService;
    }

    /**
     * Generates a sales-report summary and prepares its status message for display.
     *
     * <p>The application service remains responsible for permission validation
     * and sales calculations. The presenter creates a presentation-ready DTO so
     * that service messages are not displayed directly to the user.</p>
     *
     * @param token current member-session token
     * @param companyId identifier of the requested production company
     * @return sales-report data with a user-facing Hebrew message
     * @throws PresentationException when report generation fails
     */
    public SalesReportDTO generateSalesReport(String token, long companyId) {
        String fallback = "הפקת דוח המכירות נכשלה. אנא נסו שוב.";
        try {
            SalesReportDTO report =
                    historyService.generateSalesReport(token, companyId);

            if (report == null) {
                throw new PresentationException(
                        "הפקת דוח המכירות נכשלה. יש לנסות שוב."
                );
            }

            return new SalesReportDTO(
                    report.getTotalTicketsSold(),
                    report.getTotalRevenue(),
                    translateReportMessage(report.getMessage())
            );

        } catch (PresentationException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(
                    translateSalesReportError(
                            e.getMessage(),
                            "הפקת דוח המכירות נכשלה. יש לנסות שוב."
                    )
            );

        } catch (Exception e) {
            throw new PresentationException(
                    "אירעה שגיאה במהלך הפקת דוח המכירות. יש לנסות שוב."
            );
        }
    }

    public List<OrderDTO> getCompanyHistoryUnfiltered(String token, Long companyId) {
        String fallback = "טעינת היסטוריית החברה נכשלה. אנא נסו שוב.";
        try {
            return historyService.getHistoryForCompany(token, companyId);

            } catch (PresentationException e) {
                throw e;
            } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(translateError(e.getMessage(), fallback));
            } catch (Exception e) {
            throw new PresentationException(translateError(extractUsefulMessage(e), fallback));
            }
    }

    public List<OrderDTO> getCompanyTransactions(String token, long companyId) {
        String fallback = "טעינת עסקאות החברה נכשלה. אנא נסו שוב.";
        try {
            return historyService.getManagerFilteredHistoryForCompany(token, companyId);
            
        } catch (PresentationException e) {
            throw e;

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new PresentationException(
                    translateSalesReportError(
                            e.getMessage(),
                            "טעינת עסקאות החברה נכשלה. יש לנסות שוב."
                    )
            );

        } catch (Exception e) {
            throw new PresentationException(
                    "אירעה שגיאה במהלך טעינת עסקאות החברה. יש לנסות שוב."
            );
        }
    }

    public InputStream exportTransactionsToCsv(String token, long companyId) {
        String fallback = "יצירת קובץ ה-CSV נכשלה. אנא נסו שוב.";
        try {
            List<OrderDTO> transactions = historyService.getManagerFilteredHistoryForCompany(token, companyId);
            
            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append('\ufeff');
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
            throw new PresentationException(
                    translateSalesReportError(
                            e.getMessage(),
                            "ייצוא דוח המכירות נכשל. יש לנסות שוב."
                    )
            );

        } catch (Exception e) {
            throw new PresentationException(
                    "אירעה שגיאה במהלך ייצוא דוח המכירות. יש לנסות שוב."
            );
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

    /**
     * Translates the report-result message returned by the application service.
     *
     * <p>Only messages that are displayed as part of the report summary are
     * translated here. Unknown or missing messages receive a neutral Hebrew
     * description instead of being exposed directly to the user.</p>
     *
     * @param message message returned inside the sales-report DTO
     * @return a Hebrew message suitable for display in the sales-report view
     */
    private String translateReportMessage(String message) {
        if (message == null || message.isBlank()) {
            return "נתוני המכירות נטענו.";
        }

        return switch (message.trim()) {
            case "No sales data was found" ->
                    "לא נמצאו נתוני מכירות.";

            case "Sales report generated successfully" ->
                    "דוח המכירות הופק בהצלחה.";

            default ->
                    "נתוני המכירות נטענו.";
        };
    }

    /**
     * Translates known sales-report errors into user-facing Hebrew messages.
     *
     * <p>The presenter translates application-layer error messages before they
     * reach the view. Unknown or missing messages are replaced with the supplied
     * action-specific fallback so technical details are not exposed to the user.</p>
     *
     * @param message original exception message received from the application layer
     * @param fallback safe Hebrew message describing the failed action
     * @return translated user-facing message, or the supplied fallback
     */
    private String translateSalesReportError(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }

        return switch (message.trim()) {
            case "Invalid or expired token",
                 "Invalid or expired token.",
                 "Error: Invalid or expired session token." ->
                    "לא נמצאה פעילות משתמש. יש לרענן את העמוד ולנסות שוב.";

            case "Only members can generate sales reports" ->
                    "יש להתחבר כמנוי כדי להפיק דוח מכירות.";

            case "Could not extract user id from token" ->
                    "לא ניתן לטעון את פרטי המשתמש. יש לרענן את העמוד ולנסות שוב.";

            case "Member not found",
                 "Member not found." ->
                    "לא נמצאו פרטי המשתמש במערכת.";

            case "Suspended users can only perform view actions" ->
                    "משתמש מושהה יכול לצפות במידע בלבד ולא לבצע פעולות במערכת.";

            case "Insufficient permissions to generate sales report" ->
                    "אין לך הרשאה להפיק דוח מכירות עבור החברה הזו.";

            default ->
                    fallback;
        };
    }
}
