package ticketsystem.InfrastructureLayer;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    // id and user's sessions
    private static final ConcurrentHashMap<String, Set<String>> userSessions = new ConcurrentHashMap<>();

    // reverse mapping: session id to user id (for efficient cleanup on logout)
    private static final ConcurrentHashMap<String, String> sessionUserMap = new ConcurrentHashMap<>();

    public static void registerSession(String userId, String sessionId) {
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        sessionUserMap.put(sessionId, userId);
    }

    public static void removeSession(String sessionId) {
        String userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
        }
    }

    public static Set<String> getUserSessions(String userId) {
        return userSessions.getOrDefault(userId, Set.of());
    }

    public static boolean isUserOnline(String userId) {
        return userSessions.containsKey(userId) && !userSessions.get(userId).isEmpty();
    }

    public static String getUserIdForSession(String sessionId) {
        return sessionUserMap.get(sessionId);
    }

    public static String getSessionIdForUser(String userId) {
        Set<String> sessions = userSessions.get(userId);
        if (sessions != null && !sessions.isEmpty()) {
            return sessions.iterator().next(); // return any active session id for the user
        }
        return null;
    }
}
