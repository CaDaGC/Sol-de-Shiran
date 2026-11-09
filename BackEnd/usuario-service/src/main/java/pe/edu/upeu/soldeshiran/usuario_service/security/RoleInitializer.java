package pe.edu.upeu.soldeshiran.usuario_service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.soldeshiran.usuario_service.model.Rol;
import pe.edu.upeu.soldeshiran.usuario_service.repository.RolRepository;

@Component
@RequiredArgsConstructor
public class RoleInitializer implements CommandLineRunner {
  private final RolRepository roles;

  @Override
  @Transactional
  public void run(String... args) {
    for (String name : java.util.List.of("ADMIN", "DUENO", "TRABAJADOR", "USUARIO", "CLIENTE")) {
      if (roles.findByNombre(name).isEmpty()) {
        Rol r = new Rol();
        r.setNombre(name);
        roles.save(r);
      }
    }
  }
}
