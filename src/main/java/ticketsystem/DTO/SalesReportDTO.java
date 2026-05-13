package ticketsystem.DTO;

import java.math.BigDecimal;

public class SalesReportDTO {

    private int totalTicketsSold;
    private BigDecimal totalRevenue;
    private String message;

    public SalesReportDTO() {
    }

    public SalesReportDTO(int totalTicketsSold, BigDecimal totalRevenue, String message) {
        this.totalTicketsSold = totalTicketsSold;
        this.totalRevenue = totalRevenue;
        this.message = message;
    }

    public int getTotalTicketsSold() {
        return totalTicketsSold;
    }

    public void setTotalTicketsSold(int totalTicketsSold) {
        this.totalTicketsSold = totalTicketsSold;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}