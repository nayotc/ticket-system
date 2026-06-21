package ticketsystem.DomainLayer.discount;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("DATE_RANGE")
public class DateRangeCondition extends DiscountCondition {

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    protected DateRangeCondition() {
    }

    public DateRangeCondition(
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required");
        }

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException(
                    "End time cannot be before start time");
        }

        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }

    @Override
    public boolean isSatisfied(DiscountConditionContext context) {

        LocalDateTime now = context.getCurrentTime();

        return !now.isBefore(startTime)
                && !now.isAfter(endTime);
    }
}