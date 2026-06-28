package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;
import ticketsystem.ApplicationLayer.IBrodcaster;
import ticketsystem.DomainLayer.notifications.Notification;

@Component
public class Broadcaster implements IBrodcaster {

    private static final Executor executor = Executors.newCachedThreadPool();
    private static final Map<String, List<Consumer<Notification>>> notifiers = new ConcurrentHashMap<>();

    @Override
    public synchronized Runnable registerListener(String sessionId, Consumer<Notification> notifier) {
        addListener(sessionId, notifier);
        return () -> removeListener(sessionId, notifier);
    }

    public synchronized Runnable registerListener(Long memberId, Consumer<Notification> notifier) {
        String sessionId = memberId.toString();
        addListener(sessionId, notifier);
        return () -> removeListener(sessionId, notifier);
    }

    public void addListener(String sessionId, Consumer<Notification> notifier) {
        if (sessionId == null || sessionId.isBlank() || notifier == null) {
            return;
        }

        notifiers.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(notifier);
    }

    public static boolean hasListeners(String targetId) {
        if (targetId == null || targetId.isBlank()) {
            return false;
        }

        List<Consumer<Notification>> listeners = notifiers.get(targetId);

        return listeners != null && !listeners.isEmpty();
    }

    public static void removeListener(String sessionId, Consumer<Notification> notifier) {
        List<Consumer<Notification>> listeners = notifiers.get(sessionId);
        if (listeners != null) {
            listeners.remove(notifier);
            if (listeners.isEmpty()) {
                notifiers.remove(sessionId);
            }
        }
    }

    public static void broadcast(String targetId, Notification notification) {
        List<Consumer<Notification>> listeners = notifiers.get(targetId);
        if (listeners != null) {
            for (Consumer<Notification> listener : listeners) {
                executor.execute(() -> {
                    try {
                        listener.accept(notification);
                    } catch (RuntimeException ignored) {
                        /*
                         * Do not permanently unregister the UI because of one temporary
                         * delivery failure. NotificationCenter unregisters the listener
                         * when its UI is detached.
                         */
                    }
                });
            }
        }
    }
}
