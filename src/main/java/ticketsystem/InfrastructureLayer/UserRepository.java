package ticketsystem.InfrastructureLayer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ticketsystem.DomainLayer.user.User;

public class UserRepository implements ticketsystem.DomainLayer.IRepository.IUserRepository {
    private Map<String, User> ActiveSessionsMap;

    public UserRepository() {
        this.ActiveSessionsMap = new ConcurrentHashMap<>();
    }

    @Override
    public boolean addGuest(String sessionToken, User user) {
        if (ActiveSessionsMap.containsKey(sessionToken)) {
            return false; // Session token already exists, cannot add guest
        }
        ActiveSessionsMap.put(sessionToken, user);
        return true; // Guest added successfully
    }

    @Override
    public void removeGuest(String sessionToken) {
        ActiveSessionsMap.remove(sessionToken);
    }

    @Override
    public boolean isActiveGuest(String sessionToken) {
        return ActiveSessionsMap.containsKey(sessionToken);
    }
    @Override
    public int getTotalActiveSessions() {
        return ActiveSessionsMap.size();
    }

    
}
