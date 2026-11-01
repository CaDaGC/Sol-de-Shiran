package pe.upeu.edu.seguridad.service;

import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.upeu.edu.seguridad.dto.response.LoginResponse;
import pe.upeu.edu.seguridad.dto.response.UsuarioResponse;
import pe.upeu.edu.seguridad.entity.Usuario;
import pe.upeu.edu.seguridad.enums.Rol;
import pe.upeu.edu.seguridad.exception.ApiException;
import pe.upeu.edu.seguridad.mapper.UsuarioMapper;
import pe.upeu.edu.seguridad.repository.SeguridadRepository;
import pe.upeu.edu.seguridad.util.SeguridadUtils;

import java.time.Instant;
import java.util.Optional;

@Service
public class AuthService {
    private final SeguridadRepository store;
    private final PasswordEncoder encoder;
    private final CorreoService correo;
    private final String dummy;

    public AuthService(SeguridadRepository store, PasswordEncoder encoder, CorreoService correo) {
        this.store = store;
        this.encoder = encoder;
        this.correo = correo;
        this.dummy = encoder.encode("Una-clave-ficticia-no-utilizable-8237");
    }

    // Los intentos fallidos deben persistir aunque la respuesta sea 401.
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse login(String email, String password) {
        var now = Instant.now();
        var found = store.porCorreo(SeguridadUtils.email(email), true);
        var u = found.orElse(null);
        boolean lengthOk = password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 72;
        boolean matches =
                encoder.matches(
                                lengthOk ? password : "Password-demasiado-largo",
                                u == null ? dummy : u.passwordHash())
                        && lengthOk;
        if (u == null) throw ApiException.acceso();
        if (!u.activo() || u.bloqueadoHasta() != null && u.bloqueadoHasta().isAfter(now))
            throw ApiException.acceso();
        if (!matches) {
            store.fallo(u, now);
            store.auditar(u.id(), "LOGIN_FALLIDO", u.id());
            throw ApiException.acceso();
        }
        store.limpiarIntentos(u.id());
        String token = SeguridadUtils.token();
        var vence = now.plusSeconds(8 * 3600);
        store.sesion(SeguridadUtils.hash(token), u.id(), vence);
        store.auditar(u.id(), "LOGIN", u.id());
        return new LoginResponse(token, vence, UsuarioMapper.toResponse(u));
    }

    @Transactional
    public UsuarioResponse registrar(String nombre, String email, String password) {
        return crear(nombre, email, password, Rol.CLIENTE, null);
    }

    UsuarioResponse crear(String nombre, String email, String password, Rol rol, Long actor) {
        SeguridadUtils.password(password);
        email = SeguridadUtils.email(email);
        if (store.porCorreo(email, false).isPresent())
            throw new ApiException(
                    HttpStatus.CONFLICT, "No se pudo registrar la cuenta con estos datos.");
        try {
            long id = store.crear(nombre.trim(), email, encoder.encode(password), rol);
            store.auditar(actor, "USUARIO_CREADO", id);
            return new UsuarioResponse(id, nombre.trim(), email, rol, true);
        } catch (DuplicateKeyException e) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "No se pudo registrar la cuenta con estos datos.");
        }
    }

    public Optional<Usuario> autenticar(String token) {
        if (token == null || token.length() > 128) return Optional.empty();
        return store.autenticar(SeguridadUtils.hash(token));
    }

    @Transactional
    public void logout(String token, Usuario u) {
        store.cerrar(SeguridadUtils.hash(token));
        store.auditar(u.id(), "LOGOUT", u.id());
    }

    @Transactional
    public void cambiarPassword(Usuario actor, String actual, String nueva) {
        var u = store.porCorreo(actor.email(), true).orElseThrow(ApiException::acceso);
        if (!u.activo() || !encoder.matches(actual, u.passwordHash())) throw ApiException.acceso();
        SeguridadUtils.password(nueva);
        store.password(u.id(), encoder.encode(nueva));
        store.auditar(u.id(), "PASSWORD_CAMBIADO", u.id());
    }

    @Transactional
    public void solicitarRecuperacion(String email) {
        String token = SeguridadUtils.codigoRecuperacion();
        var u = store.porCorreo(SeguridadUtils.email(email), true).orElse(null);
        if (u == null || !u.activo()) return;
        store.recuperacion(SeguridadUtils.hash(token), u.id(), Instant.now().plusSeconds(900));
        try {
            correo.recuperar(u.email(), token);
        } catch (org.springframework.mail.MailException e) {
            LoggerFactory.getLogger(AuthService.class)
                    .warn("No fue posible entregar un correo de recuperación; revisar SMTP.");
        }
    }

    @Transactional
    public void restablecer(String token, String password) {
        SeguridadUtils.password(password);
        var id =
                store.consumirRecuperacion(SeguridadUtils.hash(token))
                        .orElseThrow(
                                () ->
                                        ApiException.invalido(
                                                "Enlace inválido o vencido. Solicita uno nuevo."));
        var u = store.porId(id).orElseThrow(ApiException::acceso);
        if (!u.activo()) throw ApiException.acceso();
        store.password(id, encoder.encode(password));
        store.auditar(id, "PASSWORD_RESTABLECIDO", id);
    }
}
