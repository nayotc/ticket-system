package DomainLayer.company;

import java.time.LocalDateTime;

public abstract class DiscountTypes {
    protected String name;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;

    public DiscountTypes(String name, LocalDateTime startTime, LocalDateTime endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getName() {
        return name;
    }
}
