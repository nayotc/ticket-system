package ticketsystem.DTO;

import java.time.LocalDateTime;

public class SuspentionUserDTO {
    private long memberId;
    private String reason;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long duration;


    public SuspentionUserDTO(long memberId,  String reason, LocalDateTime startDate, LocalDateTime endDate, Long duration) {
        this.memberId = memberId;
        this.reason = reason;
        this.startDate = startDate;
        this.endDate = endDate;
        this.duration = duration;


    }

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
}
