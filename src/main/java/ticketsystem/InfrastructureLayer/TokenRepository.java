package ticketsystem.InfrastructureLayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.user.User;

@Repository
public class TokenRepository implements ITokenRepository {
    private final Map<String, User> ActiveSessionsMap;

    public TokenRepository() {
        this.ActiveSessionsMap = new ConcurrentHashMap<>();
    }
    @Override
    public boolean addActiveSession(String sessionToken, User user) {
        if (ActiveSessionsMap.containsKey(sessionToken)) {
            return false; // Session token already exists, cannot add guest
        }
        ActiveSessionsMap.put(sessionToken, user);
        return true;
    }

    @Override
    public boolean isActiveSession(String sessionToken) {
        return ActiveSessionsMap.containsKey(sessionToken);
    }

    @Override
    public int getTotalActiveSessions() {
        return ActiveSessionsMap.size();
    }

    @Override
    public void removeActiveSession(String sessionToken) {
        ActiveSessionsMap.remove(sessionToken);
    }
}
