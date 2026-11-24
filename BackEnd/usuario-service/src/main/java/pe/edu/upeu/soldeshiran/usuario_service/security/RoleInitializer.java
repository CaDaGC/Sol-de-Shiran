package pe.edu.upeu.soldeshiran.usuario_service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.soldeshiran.usuario_service.model.Rol;
import pe.edu.upeu.soldeshiran.usuario_service.repository.RolRepository;
import pe.edu.upeu.soldeshiran.usuario_service.repository.UsuarioRepository;

@Component
@RequiredArgsConstructor
public class RoleInitializer implements CommandLineRunner {
  private final RolRepository roles;
  private final UsuarioRepository usuarios;

  @Override
  @Transactional
  public void run(String... args) {
    for (String name : java.util.List.of("ADMIN", "DUENO", "TRABAJADOR", "CLIENTE")) {
      if (roles.findByNombre(name).isEmpty()) {
        Rol r = new Rol();
        r.setNombre(name);
        roles.save(r);
      }
    }
    roles.findAll().forEach(anterior -> {
      String canonico = Actor.normalizar(anterior.getNombre());
      if (!canonico.equals(anterior.getNombre())) {
        roles.findByNombre(canonico).ifPresent(destino -> {
          if (anterior.getId().equals(destino.getId())) {
            anterior.setNombre(canonico);
            return;
          }
          usuarios.findAllByRol(anterior).forEach(usuario -> usuario.setRol(destino));
          usuarios.flush();
          roles.delete(anterior);
        });
      }
    });
  }
}
