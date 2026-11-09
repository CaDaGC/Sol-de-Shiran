package pe.edu.upeu.soldeshiran.usuario_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
@RequiredArgsConstructor
public class IdentityFilter extends OncePerRequestFilter {
  private final TokenVerifier verifier;
  private final pe.edu.upeu.soldeshiran.usuario_service.repository.UsuarioRepository usuarios;

  @Value("${seguridad.internal-key:sol-shiran-interno-desarrollo-cambiar}")
  private String internalKey;

  private Actor cargar(Long id) {
    var user = usuarios.findById(id).orElseThrow(() -> new IllegalArgumentException());
    if (Boolean.FALSE.equals(user.getActivo())) throw new IllegalArgumentException();
    return new Actor(user.getId(), Actor.normalizar(user.getRol().getNombre()));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    String path = req.getRequestURI();
    try {
      if (path.startsWith("/internal/")) {
        String supplied = req.getHeader("X-Internal-Key");
        if (supplied == null
            || !MessageDigest.isEqual(
                internalKey.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
          res.sendError(403);
          return;
        }
      } else if (path.startsWith("/api/v1/") && !publica(req)) {
        req.setAttribute("actor", cargar(verifier.verificar(req.getHeader("Authorization"))));
      }
    } catch (IllegalArgumentException ex) {
      res.sendError(401, "Cuenta o token inválidos");
      return;
    } catch (org.springframework.web.server.ResponseStatusException ex) {
      res.sendError(ex.getStatusCode().value(), ex.getReason());
      return;
    }
    chain.doFilter(req, res);
  }

  private boolean publica(HttpServletRequest r) {
    String p = r.getRequestURI();
    return "OPTIONS".equals(r.getMethod())
        || ("POST".equals(r.getMethod()) && p.equals("/api/v1/usuarios/register"))
        || ("GET".equals(r.getMethod())
            && (p.equals("/api/v1/cabanas")
                || p.matches("/api/v1/cabanas/[0-9]+")
                || p.matches("/api/v1/reservas/cabanas/[0-9]+/(disponibilidad|fechas-ocupadas)")));
  }
}
