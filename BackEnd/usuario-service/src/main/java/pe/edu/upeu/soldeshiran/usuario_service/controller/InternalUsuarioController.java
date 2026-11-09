package pe.edu.upeu.soldeshiran.usuario_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.usuario_service.model.Usuario;
import pe.edu.upeu.soldeshiran.usuario_service.repository.UsuarioRepository;
import pe.edu.upeu.soldeshiran.usuario_service.security.Actor;

@RestController
@RequestMapping("/internal/usuarios")
@RequiredArgsConstructor
public class InternalUsuarioController {
  private final UsuarioRepository usuarios;

  @GetMapping("/{id}")
  public Identity identity(@PathVariable Long id) {
    return view(find(id));
  }

  @GetMapping("/autenticacion")
  public Auth auth(@RequestParam String email) {
    Usuario u =
        usuarios
            .findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return new Auth(
        u.getId(),
        u.getNombre(),
        u.getEmail(),
        u.getPassword(),
        Actor.normalizar(u.getRol().getNombre()),
        !Boolean.FALSE.equals(u.getActivo()));
  }

  @GetMapping("/{id}/perfil")
  public Perfil perfil(@PathVariable Long id) {
    Usuario u = find(id);
    return new Perfil(u.getId(), u.getNombre(), u.getEmail());
  }

  public record Perfil(Long id, String nombre, String email) {}

  private Usuario find(Long id) {
    return usuarios
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  private Identity view(Usuario u) {
    return new Identity(
        u.getId(),
        Actor.normalizar(u.getRol().getNombre()),
        !Boolean.FALSE.equals(u.getActivo()),
        u.getDuenoId());
  }

  public record Identity(Long id, String rol, boolean activo, Long duenoId) {}

  public record Auth(
      Long id, String nombre, String email, String password, String rol, boolean activo) {}
}
