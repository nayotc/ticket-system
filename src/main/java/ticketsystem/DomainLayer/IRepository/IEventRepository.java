package ticketsystem.DomainLayer.IRepository;

import java.time.LocalDateTime;
import java.util.List;
import ticketsystem.DomainLayer.event.Event;

public interface IEventRepository {
    void addEvent(Event event);
    long getNextId();
    
}
