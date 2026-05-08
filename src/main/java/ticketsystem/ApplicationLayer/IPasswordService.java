package ticketsystem.ApplicationLayer;

public interface IPasswordService {
        String hashPassword(String password);
        boolean verifyPassword(String password, String hashedPassword);
}
