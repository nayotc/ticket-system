package ticketsystem.DTO;

import java.math.BigDecimal;

public class PurchaseDTO {
    private Long ticketId;
    private Integer row;
    private Integer chair;
    private String areaName;
    private BigDecimal price;
    private String status;
    private String secureBarcode;

    //for json
    public PurchaseDTO() {}

    public PurchaseDTO(Long ticketId, Integer row, Integer chair,String areaName, BigDecimal price, String status, String secureBarcode) {
        this.ticketId = ticketId;
        this.row = row;
        this.chair = chair;
        this.areaName=areaName;
        this.price = price;
        this.status = status;
        this.secureBarcode = secureBarcode;
    }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    
    public Integer getRow() { return row; }
    public void setRow(Integer row) { this.row = row; }
    
    public Integer getChair() { return chair; }
    public void setChair(Integer chair) { this.chair = chair; }
    
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSecureBarcode() { return secureBarcode; }
    public void setSecureBarcode(String secureBarcode) { this.secureBarcode = secureBarcode; }
    public String getAreaName() {
            return areaName;
        }

        public void setAreaName(String areaName) {
            this.areaName = areaName;
        }

}