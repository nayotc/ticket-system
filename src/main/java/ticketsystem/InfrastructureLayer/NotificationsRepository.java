package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import ticketsystem.DomainLayer.IRepository.INotificationsRepository;

public class NotificationsRepository implements INotificationsRepository {

    private final ConcurrentHashMap<Long, List<String>> pendingNotifications = new ConcurrentHashMap<>();

    @Override
    public void save(Long memberId, String message) {
        pendingNotifications.computeIfAbsent(memberId, k -> new CopyOnWriteArrayList<>()).add(message);
    }

    @Override
    // show member's pending notifications and mark them as delivered
    public List<String> getAndClear(Long memberId) {
        List<String> memberNotifications = pendingNotifications.get(memberId);

        if (memberNotifications == null || memberNotifications.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> pendingNotifications = memberNotifications.stream()
                .filter(message -> !message.isEmpty())
                .collect(Collectors.toList());

        for (String message : pendingNotifications) {
            pendingNotifications.remove(message);

        }

        return memberNotifications;
    }

}
