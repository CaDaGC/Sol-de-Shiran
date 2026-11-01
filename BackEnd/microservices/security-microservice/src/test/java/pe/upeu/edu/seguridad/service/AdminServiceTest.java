package pe.upeu.edu.seguridad.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.upeu.edu.seguridad.entity.Usuario;
import pe.upeu.edu.seguridad.enums.Rol;
import pe.upeu.edu.seguridad.exception.ApiException;
import pe.upeu.edu.seguridad.repository.SeguridadRepository;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {
    @Mock SeguridadRepository store;
    @Mock AuthService auth;
    AdminService admin;
    Usuario actor =
            new Usuario(1, "Admin", "admin@example.com", "hash", Rol.ADMIN_GENERAL, true, 0, null);

    @BeforeEach
    void init() {
        admin = new AdminService(store, auth);
    }

    @Test
    void administradorPuedeCrearEmpleado() {
        when(store.porId(1)).thenReturn(Optional.of(actor));
        admin.crear(actor, "Empleado", "empleado@example.com", "Password-seguro-123", Rol.EMPLEADO);
        verify(auth)
                .crear("Empleado", "empleado@example.com", "Password-seguro-123", Rol.EMPLEADO, 1L);
    }

    @Test
    void empleadoNoAdministraUsuarios() {
        var empleado =
                new Usuario(
                        3, "Empleado", "empleado@example.com", "hash", Rol.EMPLEADO, true, 0, null);
        when(store.porId(3)).thenReturn(Optional.of(empleado));
        assertThatThrownBy(() -> admin.listar(empleado, 0)).isInstanceOf(ApiException.class);
        verify(store, never()).listar(anyInt());
    }

    @Test
    void propietarioNoListaUsuarios() {
        var owner =
                new Usuario(
                        2, "Dueño", "owner@example.com", "hash", Rol.PROPIETARIO, true, 0, null);
        when(store.porId(2)).thenReturn(Optional.of(owner));
        assertThatThrownBy(() -> admin.listar(owner, 0)).isInstanceOf(ApiException.class);
        verify(store, never()).listar(anyInt());
    }

    @Test
    void editarNormalizaCorreoYConservaDatosDeRespuesta() {
        when(store.porId(1)).thenReturn(Optional.of(actor));
        when(store.porId(2))
                .thenReturn(
                        Optional.of(
                                new Usuario(
                                        2,
                                        "Antes",
                                        "antes@example.com",
                                        "hash",
                                        Rol.CLIENTE,
                                        true,
                                        0,
                                        null)));
        var result = admin.editar(actor, 2, "  Pedro  ", " PEDRO@example.com ", Rol.EMPLEADO, true);
        verify(store).editar(2, "Pedro", "pedro@example.com", Rol.EMPLEADO, true);
        assertThat(result.email()).isEqualTo("pedro@example.com");
        verify(store).auditar(1L, "USUARIO_EDITADO", 2L);
    }

    @Test
    void editarRechazaCorreoDeOtraCuenta() {
        when(store.porId(1)).thenReturn(Optional.of(actor));
        when(store.porId(2))
                .thenReturn(
                        Optional.of(
                                new Usuario(
                                        2,
                                        "Cliente",
                                        "cliente@example.com",
                                        "hash",
                                        Rol.CLIENTE,
                                        true,
                                        0,
                                        null)));
        when(store.porCorreo(actor.email(), false)).thenReturn(Optional.of(actor));
        assertThatThrownBy(
                        () -> admin.editar(actor, 2, "Cliente", actor.email(), Rol.CLIENTE, true))
                .isInstanceOf(ApiException.class);
        verify(store, never()).editar(anyLong(), anyString(), anyString(), any(), anyBoolean());
    }

    @Test
    void editarNoPermiteDesactivarSuPropiaCuenta() {
        when(store.porId(1)).thenReturn(Optional.of(actor));
        assertThatThrownBy(
                        () ->
                                admin.editar(
                                        actor,
                                        1,
                                        actor.nombre(),
                                        actor.email(),
                                        Rol.ADMIN_GENERAL,
                                        false))
                .isInstanceOf(ApiException.class);
        verify(store, never()).editar(anyLong(), anyString(), anyString(), any(), anyBoolean());
    }

    @Test
    void eliminarClienteConservaEventoSinReferenciaBorrada() {
        when(store.porId(1)).thenReturn(Optional.of(actor));
        when(store.porId(2))
                .thenReturn(
                        Optional.of(
                                new Usuario(
                                        2,
                                        "Cliente",
                                        "cliente@example.com",
                                        "hash",
                                        Rol.CLIENTE,
                                        true,
                                        0,
                                        null)));
        admin.eliminar(actor, 2);
        verify(store).eliminar(2);
        verify(store).auditar(1L, "USUARIO_ELIMINADO_2", null);
    }

    @Test
    void eliminarNoPermiteBorrarseASiMismo() {
        when(store.porId(1)).thenReturn(Optional.of(actor));
        assertThatThrownBy(() -> admin.eliminar(actor, 1)).isInstanceOf(ApiException.class);
        verify(store, never()).eliminar(anyLong());
    }

    @Test
    void empleadoNoPuedeEliminarUsuarios() {
        var empleado =
                new Usuario(
                        3, "Empleado", "empleado@example.com", "hash", Rol.EMPLEADO, true, 0, null);
        when(store.porId(3)).thenReturn(Optional.of(empleado));
        assertThatThrownBy(() -> admin.eliminar(empleado, 2)).isInstanceOf(ApiException.class);
        verify(store, never()).eliminar(anyLong());
    }

    @Test
    void adminNoModificaSuPropioAcceso() {
        when(store.porId(1)).thenReturn(Optional.of(actor));
        assertThatThrownBy(() -> admin.acceso(actor, 1, Rol.CLIENTE, false))
                .isInstanceOf(ApiException.class);
        verify(store, never()).cambiarAcceso(anyLong(), any(), anyBoolean());
    }

    @Test
    void adminDesactivadoNoGestionaUsuarios() {
        var disabled =
                new Usuario(1, "Admin", actor.email(), "hash", Rol.ADMIN_GENERAL, false, 0, null);
        when(store.porId(1)).thenReturn(Optional.of(disabled));
        assertThatThrownBy(
                        () ->
                                admin.crear(
                                        actor,
                                        "Otro",
                                        "otro@example.com",
                                        "Password-seguro-123",
                                        Rol.PROPIETARIO))
                .isInstanceOf(ApiException.class);
        verifyNoInteractions(auth);
    }

    @Test
    void cambioDeAccesoPideRevocacionEnStore() {
        when(store.porId(1)).thenReturn(Optional.of(actor));
        when(store.porId(2))
                .thenReturn(
                        Optional.of(
                                new Usuario(
                                        2,
                                        "Owner",
                                        "o@example.com",
                                        "hash",
                                        Rol.PROPIETARIO,
                                        true,
                                        0,
                                        null)));
        admin.acceso(actor, 2, Rol.CLIENTE, false);
        verify(store).cambiarAcceso(2, Rol.CLIENTE, false);
    }
}
