package ticketsystem.ApplicationLayer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ticketsystem.ApplicationLayer.ISystemLogger.LogLevel;
import ticketsystem.DomainLayer.IRepository.ITokenRepository;
import ticketsystem.DomainLayer.user.Guest;
import ticketsystem.DomainLayer.user.Member;
import ticketsystem.DomainLayer.user.User;

@Service
public class TokenService implements ITokenService {

    private final SecretKey key;
    private final long expirationTime = 1000 * 60; // 1 minute in milliseconds
    private final ITokenRepository tokenRepository;
    private final ISystemLogger logger;

    @Autowired
    public TokenService(
            @Value("${jwt.secret:default_secret_key_for_development_purposes_only_32_chars}") String secret,
            ITokenRepository tokenRepository,
            ISystemLogger logger) {

        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.tokenRepository = tokenRepository;
        this.logger = logger;
    }

    @Override
    public String addActiveSession(User user) {
        if (user instanceof Guest) {
            String sessionToken = generateNewGuestToken();
            while (!tokenRepository.addActiveSession(sessionToken, user)) {
                sessionToken = generateNewGuestToken();
            }
            logger.logEvent("New guest session created with session token: " + maskToken(sessionToken), LogLevel.INFO);
            return sessionToken;
        }
        if (user instanceof Member member) {
            String sessionToken = generateNewMemberToken(member.getId());
            while (!tokenRepository.addActiveSession(sessionToken, user)) {
                sessionToken = generateNewMemberToken(member.getId());
            }
            logger.logEvent("New member session created with session token: " + maskToken(sessionToken), LogLevel.INFO);
            return sessionToken;
        }
        logger.logEvent("Failed to create session: unknown user type", LogLevel.WARN);
        return null;
    }

    @Override
    public boolean isActiveSession(String sessionToken) {
        if (sessionToken == null) {
            return false;
        }
        return tokenRepository.isActiveSession(sessionToken);
    }

    @Override
    public int getTotalActiveSessions() {
        return tokenRepository.getTotalActiveSessions();
    }

    @Override
    public void removeActiveSession(String sessionToken) {
        if (sessionToken == null) {
            return;
        }
        tokenRepository.removeActiveSession(sessionToken);
    }

    @Override
    public String generateNewGuestToken() {
        long guestId = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "GUEST");
        return generateToken(claims, String.valueOf(guestId));
    }

    @Override
    public String generateNewMemberToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "MEMBER");
        return generateToken(claims, String.valueOf(userId));
    }

    public String generateToken(Map<String, Object> claims, String subject) {
        {
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(subject)
                    .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                    .signWith(key)
                    .compact();
        }
    }

    @Override
    public boolean validateToken(String token) {
        if (token == null || token.isEmpty()) {
            logger.logEvent("Token validation failed: token is missing or null", LogLevel.WARN);
            throw new IllegalArgumentException("Token is missing or null");
        }
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            if (!isActiveSession(token)) {
                logger.logEvent("Token validation failed: session is no longer active", LogLevel.WARN);
                throw new IllegalArgumentException("Session is no longer active");
            }
        } catch (JwtException | IllegalArgumentException e) {
            logger.logEvent("Token validation failed: " + e.getMessage(), LogLevel.WARN);
            throw new IllegalArgumentException("Invalid or expired security token");
        }
        logger.logEvent("Token validated successfully for token: " + maskToken(token), LogLevel.INFO);
        return true;
    }

    @Override
    public String extractRole(String token) {
        if (token == null) {
            return null;
        }
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    @Override
    public boolean isGuestToken(String token) {
        if (token == null) {
            return false;
        }
        return "GUEST".equals(extractRole(token));
    }

    @Override
    public boolean isMemberToken(String token) {
        if (token == null) {
            return false;
        }
        return "MEMBER".equals(extractRole(token));
    }

    @Override
    public Long extractUserId(String token) {
        if (token == null) {
            return null;
        }
        String subject = extractSubject(token);
        if (subject == null || subject.isBlank() || isGuestToken(token)) {
            return null;
        }
        return Long.valueOf(subject);
    }

    private String extractSubject(String token) {
        if (token == null) {
            return null;
        }
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        if (token == null) {
            return null;
        }
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        if (token == null) {
            return null;
        }
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Override
    public String maskToken(String token) {
        if (token == null) {
            return "null";
        }

        if (token.length() <= 12) {
            return "***";
        }

        return token.substring(0, 6) + "..." + token.substring(token.length() - 6);
    }
}
