package ticketsystem.DomainLayer.IRepository;

import java.util.List;

public interface INotificationsRepository {

    void save(Long memberId, String message);

    List<String> getAndClear(Long memberId);

}
