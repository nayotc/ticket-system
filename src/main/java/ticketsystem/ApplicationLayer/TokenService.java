package ticketsystem.ApplicationLayer;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import javax.crypto.SecretKey;

public class TokenService {
    @Value("${jwt.secret}")
    private String secret;
    private final long expirationTime = 1000 * 60 * 60; // 1 hour
    private SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    public String generateNewGuestToken() {
        String guestId = "GUEST_" + UUID.randomUUID().toString();
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "GUEST");
        return generateToken(claims, guestId);
    }

    public String generateNewMemberToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "REGISTERED");
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

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    public boolean isGuestToken(String token) {
        return "GUEST".equals(extractRole(token));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}
