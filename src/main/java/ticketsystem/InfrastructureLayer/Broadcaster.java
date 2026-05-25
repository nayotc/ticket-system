package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import ticketsystem.ApplicationLayer.IBrodcaster;
import ticketsystem.DomainLayer.notifications.Notification;

public class Broadcaster implements IBrodcaster {

    private static final Executor executor = Executors.newCachedThreadPool();
    private static NotificationsRepository repository = new NotificationsRepository();
    private static Map<String, List<Consumer<String>>> notifiers = new ConcurrentHashMap<>();

    @Override
    public synchronized Runnable registerListener(String sessionId, Consumer<String> notifier) {
        addListener(sessionId, notifier);
        return () -> removeListener(sessionId, notifier);
    }

    public synchronized Runnable registerListener(Long memberId, Consumer<String> notifier) {
        String sessionId = memberId.toString();
        addListener(sessionId, notifier);
        return () -> removeListener(sessionId, notifier);
    }

    public void addListener(String sessionId, Consumer<String> notifier) {
        notifiers.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(notifier);
    }

    public static void removeListener(String sessionId, Consumer<String> notifier) {
        List<Consumer<String>> listeners = notifiers.get(sessionId);
        if (listeners != null) {
            listeners.remove(notifier);
            if (listeners.isEmpty()) {
                notifiers.remove(sessionId);
            }
        }
    }

    public static void broadcastToGuest(String sessionId, String message) {
        List<Consumer<String>> listeners = notifiers.get(sessionId);

        if (listeners != null) {
            for (Consumer<String> listener : listeners) {
                executor.execute(() -> {
                    try {
                        listener.accept(message);
                    } catch (Exception e) {
                        removeListener(sessionId, listener);
                    }
                });
            }
        }
    }

    public void broadcastToMember(Notification notification) {
        String sessionId = String.valueOf(notification.getRecipientMemberId());
        List<Consumer<String>> listeners = notifiers.get(sessionId);

        if (listeners != null) {
            for (Consumer<String> listener : listeners) {
                executor.execute(() -> {
                    try {
                        listener.accept(notification.getMessage());
                    } catch (Exception e) {
                        removeListener(sessionId, listener);
                    }
                });
            }
        }
    }

}
