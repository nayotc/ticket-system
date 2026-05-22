package ticketsystem.DomainLayer.IRepository;

import java.util.List;

public interface INotificationsRepository {

    void save(String userId, String message);

    List<String> getAndClear(String userId);
}
