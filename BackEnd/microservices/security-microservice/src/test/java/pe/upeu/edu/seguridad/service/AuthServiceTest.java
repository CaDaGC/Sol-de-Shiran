package pe.upeu.edu.seguridad.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import pe.upeu.edu.seguridad.entity.Usuario;
import pe.upeu.edu.seguridad.enums.Rol;
import pe.upeu.edu.seguridad.exception.ApiException;
import pe.upeu.edu.seguridad.repository.SeguridadRepository;
import pe.upeu.edu.seguridad.util.SeguridadUtils;

import java.time.Instant;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock SeguridadRepository store;
    @Mock CorreoService correo;
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    AuthService auth;
    Usuario user;

    @BeforeEach
    void setup() {
        auth = new AuthService(store, encoder, correo);
        user =
                new Usuario(
                        4,
                        "Cliente",
                        "cliente@example.com",
                        encoder.encode("Password-seguro-123"),
                        Rol.CLIENTE,
                        true,
                        0,
                        null);
    }

    @Test
    void registroPublicoSiempreEsCliente() {
        when(store.porCorreo("nuevo@example.com", false)).thenReturn(Optional.empty());
        when(store.crear(eq("Nuevo"), eq("nuevo@example.com"), anyString(), eq(Rol.CLIENTE)))
                .thenReturn(9L);
        var u = auth.registrar("Nuevo", "NUEVO@example.com", "Password-seguro-123");
        assertThat(u.rol()).isEqualTo(Rol.CLIENTE);
        var hash = ArgumentCaptor.forClass(String.class);
        verify(store).crear(eq("Nuevo"), eq("nuevo@example.com"), hash.capture(), eq(Rol.CLIENTE));
        assertThat(encoder.matches("Password-seguro-123", hash.getValue())).isTrue();
    }

    @Test
    void rechazaCorreoDuplicado() {
        when(store.porCorreo(user.email(), false)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> auth.registrar("Otro", user.email(), "Password-seguro-123"))
                .isInstanceOf(ApiException.class);
        verify(store, never()).crear(anyString(), anyString(), anyString(), any());
    }

    @Test
    void loginGuardaSoloHashDelToken() {
        when(store.porCorreo(user.email(), true)).thenReturn(Optional.of(user));
        var a = auth.login(user.email(), "Password-seguro-123");
        verify(store).sesion(eq(SeguridadUtils.hash(a.token())), eq(user.id()), any());
        assertThat(a.venceEn()).isAfter(Instant.now());
        assertThat(a.usuario().rol()).isEqualTo(Rol.CLIENTE);
    }

    @Test
    void loginFallidoRegistraIntento() {
        when(store.porCorreo(user.email(), true)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> auth.login(user.email(), "incorrecta"))
                .isInstanceOf(ApiException.class);
        verify(store).fallo(eq(user), any());
        verify(store, never()).sesion(anyString(), anyLong(), any());
    }

    @Test
    void loginInexistenteNoCreaSesion() {
        when(store.porCorreo("no@example.com", true)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auth.login("no@example.com", "incorrecta"))
                .isInstanceOf(ApiException.class);
        verify(store, never()).sesion(anyString(), anyLong(), any());
    }

    @Test
    void cuentaDesactivadaNoIngresa() {
        var inactive =
                new Usuario(
                        4,
                        user.nombre(),
                        user.email(),
                        user.passwordHash(),
                        user.rol(),
                        false,
                        0,
                        null);
        when(store.porCorreo(user.email(), true)).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> auth.login(user.email(), "Password-seguro-123"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void cuentaBloqueadaNoIngresa() {
        var blocked =
                new Usuario(
                        4,
                        user.nombre(),
                        user.email(),
                        user.passwordHash(),
                        user.rol(),
                        true,
                        5,
                        Instant.now().plusSeconds(60));
        when(store.porCorreo(user.email(), true)).thenReturn(Optional.of(blocked));
        assertThatThrownBy(() -> auth.login(user.email(), "Password-seguro-123"))
                .isInstanceOf(ApiException.class);
        verify(store, never()).sesion(anyString(), anyLong(), any());
    }

    @Test
    void logoutRevocaToken() {
        auth.logout("token-privado", user);
        verify(store).cerrar(SeguridadUtils.hash("token-privado"));
    }

    @Test
    void recuperacionNoExponeCorreoInexistente() {
        when(store.porCorreo("no@example.com", true)).thenReturn(Optional.empty());
        auth.solicitarRecuperacion("no@example.com");
        verifyNoInteractions(correo);
    }

    @Test
    void recuperacionGuardaHashYEnviaToken() {
        when(store.porCorreo(user.email(), true)).thenReturn(Optional.of(user));
        auth.solicitarRecuperacion(user.email());
        var token = ArgumentCaptor.forClass(String.class);
        verify(correo).recuperar(eq(user.email()), token.capture());
        assertThat(token.getValue()).matches("[0-9a-f]{32}");
        verify(store).recuperacion(eq(SeguridadUtils.hash(token.getValue())), eq(user.id()), any());
    }

    @Test
    void enlaceInvalidoNoCambiaPassword() {
        when(store.consumirRecuperacion(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auth.restablecer("vencido", "Password-nuevo-456"))
                .isInstanceOf(ApiException.class);
        verify(store, never()).password(anyLong(), anyString());
    }

    @Test
    void restablecerCambiaPasswordConHash() {
        when(store.consumirRecuperacion(SeguridadUtils.hash("valido"))).thenReturn(Optional.of(4L));
        when(store.porId(4)).thenReturn(Optional.of(user));
        auth.restablecer("valido", "Password-nuevo-456");
        var hash = ArgumentCaptor.forClass(String.class);
        verify(store).password(eq(4L), hash.capture());
        assertThat(encoder.matches("Password-nuevo-456", hash.getValue())).isTrue();
    }

    @Test
    void cambiarRequierePasswordActual() {
        when(store.porCorreo(user.email(), true)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> auth.cambiarPassword(user, "incorrecta", "Password-nuevo-456"))
                .isInstanceOf(ApiException.class);
        verify(store, never()).password(anyLong(), anyString());
    }

    @Test
    void rechazaPasswordCorto() {
        assertThatThrownBy(() -> auth.registrar("Nuevo", "nuevo@example.com", "corta"))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(store);
    }

    @Test
    void rechazaMasDe72Bytes() {
        assertThatThrownBy(() -> SeguridadUtils.password("ñ".repeat(37)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void tokensSonAleatorios() {
        assertThat(SeguridadUtils.token()).isNotEqualTo(SeguridadUtils.token());
        assertThat(SeguridadUtils.hash("dato")).hasSize(64);
    }
}
