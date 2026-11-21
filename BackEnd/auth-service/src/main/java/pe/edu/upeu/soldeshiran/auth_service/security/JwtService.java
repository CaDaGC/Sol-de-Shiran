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
        Date emitido = new Date();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("userId", usuario.getId())
                .claim("rol", usuario.getRol())
                .issuedAt(emitido)
                .expiration(new Date(emitido.getTime() + Math.min(expiration, 3_600_000L)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
