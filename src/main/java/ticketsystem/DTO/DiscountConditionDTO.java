package ticketsystem.DTO;

import java.time.LocalDateTime;

public class DiscountConditionDTO {

    private String type; // MIN_TICKET / MAX_TICKET / DATE
    private Integer ticketThreshold;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public DiscountConditionDTO() {
    }

    public DiscountConditionDTO(String type,
                                Integer ticketThreshold,
                                LocalDateTime startTime,
                                LocalDateTime endTime) {
        this.type = type;
        this.ticketThreshold = ticketThreshold;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getTicketThreshold() {
        return ticketThreshold;
    }

    public void setTicketThreshold(Integer ticketThreshold) {
        this.ticketThreshold = ticketThreshold;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}