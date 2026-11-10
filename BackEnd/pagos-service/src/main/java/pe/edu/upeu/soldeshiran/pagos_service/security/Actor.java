package pe.edu.upeu.soldeshiran.pagos_service.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record Actor(Long id, String rol) {
  public Actor {
    rol = normalizar(rol);
  }

  public static String normalizar(String rol) {
    if (rol == null) return "";
    return switch (rol.trim().toUpperCase(java.util.Locale.ROOT)) {
      case "USUARIO" -> "CLIENTE";
      case "DUEÑO", "PROPIETARIO" -> "DUENO";
      case "EMPLEADO", "GUARDIAN" -> "TRABAJADOR";
      default -> rol.trim().toUpperCase(java.util.Locale.ROOT);
    };
  }

  public static Actor desde(HttpServletRequest request) {
    Object actor = request.getAttribute("actor");
    if (!(actor instanceof Actor a))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicia sesión");
    return a;
  }

  public boolean es(String... roles) {
    return java.util.Arrays.asList(roles).contains(rol);
  }

  public void exigir(String... roles) {
    if (!es(roles))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para esta acción");
  }
}
