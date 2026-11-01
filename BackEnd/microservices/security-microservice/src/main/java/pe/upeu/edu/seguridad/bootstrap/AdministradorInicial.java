package pe.upeu.edu.seguridad.bootstrap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pe.upeu.edu.seguridad.enums.Rol;
import pe.upeu.edu.seguridad.repository.SeguridadRepository;
import pe.upeu.edu.seguridad.util.SeguridadUtils;

@Component
public class AdministradorInicial implements ApplicationRunner {
    private final SeguridadRepository store;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdministradorInicial(
            SeguridadRepository store,
            PasswordEncoder encoder,
            @Value("${app.admin-email:}") String email,
            @Value("${app.admin-password:}") String password) {
        this.store = store;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() && password.isBlank()) return;
        if (!email.matches("[^ @]+@[^ @]+[.][^ @]+"))
            throw new IllegalArgumentException("Configura ADMIN_EMAIL con un correo válido.");
        SeguridadUtils.password(password);
        var found = store.porCorreo(SeguridadUtils.email(email), false);
        if (found.isPresent()) {
            if (found.get().rol() != Rol.ADMIN_GENERAL)
                throw new IllegalStateException(
                        "ADMIN_EMAIL ya pertenece a una cuenta no administradora.");
            return;
        }
        long id =
                store.crear(
                        "Administrador general",
                        SeguridadUtils.email(email),
                        encoder.encode(password),
                        Rol.ADMIN_GENERAL);
        store.auditar(null, "ADMIN_INICIAL_CREADO", id);
    }
}
