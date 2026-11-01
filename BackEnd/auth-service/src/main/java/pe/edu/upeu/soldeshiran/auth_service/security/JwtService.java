package pe.edu.upeu.soldeshiran.auth_service.security;

import pe.edu.upeu.soldeshiran.auth_service.dto.UsuarioResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String generateToken(UsuarioResponse usuario) {
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("userId", usuario.getId())
                .claim("rol", usuario.getRol())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
