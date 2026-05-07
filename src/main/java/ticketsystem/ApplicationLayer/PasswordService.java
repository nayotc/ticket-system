package ticketsystem.ApplicationLayer;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordService implements IPasswordService {
    private BCryptPasswordEncoder passwordEncoder;

    public PasswordService() {
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    public String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Override
    public boolean verifyPassword(String password, String hashedPassword) {
        return passwordEncoder.matches(password, hashedPassword);
    }

}
