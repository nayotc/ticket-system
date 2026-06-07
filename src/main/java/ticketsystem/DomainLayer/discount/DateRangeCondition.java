package ticketsystem.DomainLayer.discount;

import java.time.LocalDateTime;

public class DateRangeCondition implements DiscountCondition {

    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    public DateRangeCondition(LocalDateTime startTime,
                              LocalDateTime endTime) {

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