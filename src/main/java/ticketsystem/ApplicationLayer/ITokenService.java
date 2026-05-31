package ticketsystem.ApplicationLayer;

import ticketsystem.DomainLayer.user.User;

public interface ITokenService {

    String generateNewGuestToken();

    String generateNewMemberToken(Long userId);

    boolean validateToken(String token);

    Long extractUserId(String token);

    String extractRole(String token);

    boolean isGuestToken(String token);

    boolean isMemberToken(String token);

    String addActiveSession(User user);

    boolean isActiveSession(String sessionToken);

    int getTotalActiveSessions();

    void removeActiveSession(String sessionToken);

    String maskToken(String token);
}
