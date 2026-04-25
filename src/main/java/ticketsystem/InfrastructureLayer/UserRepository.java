package ticketsystem.InfrastructureLayer;

public class UserRepository implements ticketsystem.DomainLayer.IRepository.IUserRepository {
    private java.util.Set<String> activeGuests;

    public UserRepository() {
        this.activeGuests = new java.util.HashSet<>();
    }

    @Override
    public void addGuest(String sessionToken) {
        if (activeGuests.contains(sessionToken)) {
            throw new IllegalArgumentException("Session token already exists.");
        }
        activeGuests.add(sessionToken);
    }

    @Override
    public void removeGuest(String sessionToken) {
        activeGuests.remove(sessionToken);
    }

    @Override
    public boolean isActiveGuest(String sessionToken) {
        return activeGuests.contains(sessionToken);
    }
    
}
