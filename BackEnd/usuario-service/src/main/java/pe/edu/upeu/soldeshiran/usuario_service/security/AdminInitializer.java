package pe.edu.upeu.soldeshiran.usuario_service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.soldeshiran.usuario_service.model.*;
import pe.edu.upeu.soldeshiran.usuario_service.repository.*;

@Component
@Order(10)
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {
  private final UsuarioRepository usuarios;
  private final RolRepository roles;
  private final PasswordEncoder passwords;

  @Value("${admin.email:}")
  private String email;

  @Value("${admin.password:}")
  private String password;

  @Override
  @Transactional
  public void run(String... args) {
    if (email.isBlank() || password.isBlank()) return;
    if (password.length() < 12)
      throw new IllegalArgumentException("ADMIN_PASSWORD requiere 12 caracteres");
    String correo = email.trim().toLowerCase();
    if (usuarios.existsByEmail(correo)) return;
    Rol rol =
        roles
            .findByNombre("ADMIN")
            .orElseGet(
                () -> {
                  Rol r = new Rol();
                  r.setNombre("ADMIN");
                  return roles.save(r);
                });
    Usuario u = new Usuario();
    u.setNombre("Administrador");
    u.setEmail(correo);
    u.setPassword(passwords.encode(password));
    u.setRol(rol);
    u.setActivo(true);
    usuarios.save(u);
  }
}
