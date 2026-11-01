package pe.upeu.edu.seguridad.integration;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import pe.upeu.edu.seguridad.exception.ApiException;
import pe.upeu.edu.seguridad.repository.SeguridadRepository;
import pe.upeu.edu.seguridad.service.AdminService;
import pe.upeu.edu.seguridad.service.AuthService;
import pe.upeu.edu.seguridad.util.SeguridadUtils;

import java.util.UUID;

@SpringBootTest
@ActiveProfiles("it")
@Tag("mysql")
@Transactional
class MysqlSecurityIT {
    @Autowired AuthService auth;
    @Autowired SeguridadRepository store;
    @Autowired AdminService admin;

    String email() {
        return UUID.randomUUID() + "@example.com";
    }

    @Test
    void altaLoginYLogoutPersisten() {
        var email = email();
        auth.registrar("Cliente", email, "Password-seguro-123");
        var a = auth.login(email, "Password-seguro-123");
        assertThat(auth.autenticar(a.token())).isPresent();
        auth.logout(a.token(), store.porId(a.usuario().id()).orElseThrow());
        assertThat(auth.autenticar(a.token())).isEmpty();
    }

    @Test
    void restablecerEsDeUnSoloUsoYRevocaSesiones() {
        var email = email();
        var user = auth.registrar("Cliente", email, "Password-seguro-123");
        var acceso = auth.login(email, "Password-seguro-123");
        var raw = SeguridadUtils.token();
        store.recuperacion(
                SeguridadUtils.hash(raw), user.id(), java.time.Instant.now().plusSeconds(100));
        auth.restablecer(raw, "Password-nuevo-456");
        assertThat(auth.autenticar(acceso.token())).isEmpty();
        assertThatThrownBy(() -> auth.restablecer(raw, "Password-nuevo-789"))
                .isInstanceOf(ApiException.class);
        assertThat(auth.login(email, "Password-nuevo-456").usuario().id()).isEqualTo(user.id());
    }

    @Test
    void cincoFallosBloqueanLaCuenta() {
        var email = email();
        auth.registrar("Cliente", email, "Password-seguro-123");
        for (int i = 0; i < 5; i++)
            assertThatThrownBy(() -> auth.login(email, "incorrecta"))
                    .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> auth.login(email, "Password-seguro-123"))
                .isInstanceOf(ApiException.class);
        assertThat(store.porCorreo(email, false).orElseThrow().intentos()).isEqualTo(5);
    }
}
