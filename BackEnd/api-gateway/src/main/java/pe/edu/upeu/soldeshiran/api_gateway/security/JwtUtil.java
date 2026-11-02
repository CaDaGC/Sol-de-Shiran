package pe.edu.upeu.soldeshiran.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception exception) {
            System.out.println("TOKEN INVALIDO: " + exception.getMessage());
            return false;
        }
    }

    public String getRol(String token) {
        return getClaims(token).get("rol", String.class);
    }

    public Long getUserId(String token) {
        Object value = getClaims(token).get("userId");

        if (value instanceof Number number) {
            return number.longValue();
        }

        throw new IllegalArgumentException("El token no contiene el claim userId.");
    }
}