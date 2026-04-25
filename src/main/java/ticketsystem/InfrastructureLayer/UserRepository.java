package ticketsystem.InfrastructureLayer;
import java.util.Set;

public class UserRepository implements ticketsystem.DomainLayer.IRepository.IUserRepository {
    private Set<String> activeUsersTokens;

    public UserRepository() {
        this.activeUsersTokens = new java.util.HashSet<>();
    }

    @Override
    public void addGuest(String sessionToken) {
        if (activeUsersTokens.contains(sessionToken)) {
            throw new IllegalArgumentException("Session token already exists.");
        }
        activeUsersTokens.add(sessionToken);
    }

    @Override
    public void removeGuest(String sessionToken) {
        activeUsersTokens.remove(sessionToken);
    }

    @Override
    public boolean isActiveGuest(String sessionToken) {
        return activeUsersTokens.contains(sessionToken);
    }
    @Override
    public int getTotalActiveSessions() {
        return activeUsersTokens.size();
    }
    
    
}
