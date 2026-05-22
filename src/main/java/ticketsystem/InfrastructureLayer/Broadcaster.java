package ticketsystem.InfrastructureLayer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Broadcaster {

    private static final Executor executor = Executors.newCachedThreadPool();

    private static final Map<String, List<Consumer<String>>> notifiers = new ConcurrentHashMap<>();

    public static synchronized Runnable registerListener(String sessionId, Consumer<String> notifier) {
        addListener(sessionId, notifier);
        return () -> removeListener(sessionId, notifier);
    }

    public static void addListener(String sessionId, Consumer<String> notifier) {
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

    public static void broadcast(String sessionId, String message, NotificationsRepository repository) {
        List<Consumer<String>> listeners = notifiers.get(sessionId);
        if (listeners != null) {
            for (Consumer<String> listener : listeners) {
                executor.execute(() -> {
                    try {
                        listener.accept(message);
                    } catch (Exception e) {
                        // to avoid memory leak
                        removeListener(sessionId, listener);

                        // extract user id from session manager and save the message to the repository for later retrieval
                        String userId = SessionManager.getUserIdForSession(sessionId);

                        if (userId != null && repository != null) {
                            repository.save(userId, message);
                            // add log!
                            //System.out.println("Listener disconnected. Message saved to fallback repository for user: " + userId);
                        }
                    }
                });
            }
        }
    }
}
