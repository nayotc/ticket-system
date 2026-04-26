package ticketsystem.ApplicationLayer;

public interface ITokenService {
        String generateNewGuestToken();
        String generateNewMemberToken(Long userId);
        boolean validateToken(String token);
        String extractSubject(String token);
}
