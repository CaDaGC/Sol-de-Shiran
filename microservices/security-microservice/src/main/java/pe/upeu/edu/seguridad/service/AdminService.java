package pe.upeu.edu.seguridad.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.upeu.edu.seguridad.dto.response.UsuarioResponse;
import pe.upeu.edu.seguridad.entity.Usuario;
import pe.upeu.edu.seguridad.enums.Rol;
import pe.upeu.edu.seguridad.exception.ApiException;
import pe.upeu.edu.seguridad.mapper.UsuarioMapper;
import pe.upeu.edu.seguridad.repository.SeguridadRepository;
import pe.upeu.edu.seguridad.util.SeguridadUtils;

import java.util.List;

@Service
public class AdminService {
    private final SeguridadRepository store;
    private final AuthService auth;

    public AdminService(SeguridadRepository store, AuthService auth) {
        this.store = store;
        this.auth = auth;
    }

    private void comprobar(Usuario actor) {
        var actual = store.porId(actor.id()).orElseThrow(ApiException::acceso);
        if (!actual.activo() || actual.rol() != Rol.ADMIN_GENERAL)
            throw new ApiException(HttpStatus.FORBIDDEN, "Acceso no permitido.");
    }

    public List<UsuarioResponse> listar(Usuario actor, int pagina) {
        comprobar(actor);
        if (pagina < 0 || pagina > 10000) throw ApiException.invalido("Página inválida.");
        return store.listar(pagina).stream().map(UsuarioMapper::toResponse).toList();
    }

    @Transactional
    public UsuarioResponse crear(
            Usuario actor, String nombre, String email, String password, Rol rol) {
        comprobar(actor);
        return auth.crear(nombre, email, password, rol, actor.id());
    }

    @Transactional
    public void acceso(Usuario actor, long id, Rol rol, boolean activo) {
        var admins = store.bloquearAdmins();
        comprobar(actor);
        if (actor.id() == id)
            throw ApiException.invalido(
                    "No puedes cambiar tu propio rol o estado desde este panel.");
        var u =
                store.porId(id)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "Usuario no encontrado."));
        if (u.rol() == Rol.ADMIN_GENERAL
                && u.activo()
                && (rol != Rol.ADMIN_GENERAL || !activo)
                && admins.size() < 2)
            throw ApiException.invalido("Debe permanecer un administrador activo.");
        store.cambiarAcceso(id, rol, activo);
        store.auditar(actor.id(), "ACCESO_MODIFICADO", id);
    }

    @Transactional
    public UsuarioResponse editar(
            Usuario actor, long id, String nombre, String email, Rol rol, boolean activo) {
        var admins = store.bloquearAdmins();
        comprobar(actor);
        var actual =
                store.porId(id)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "Usuario no encontrado."));
        if (actor.id() == id && (rol != actual.rol() || activo != actual.activo()))
            throw ApiException.invalido("No puedes cambiar tu propio rol o estado.");
        if (actual.rol() == Rol.ADMIN_GENERAL
                && actual.activo()
                && (rol != Rol.ADMIN_GENERAL || !activo)
                && admins.size() < 2)
            throw ApiException.invalido("Debe permanecer un administrador activo.");
        String correo = SeguridadUtils.email(email);
        var existente = store.porCorreo(correo, false);
        if (existente.isPresent() && existente.get().id() != id)
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado.");
        try {
            store.editar(id, nombre.trim(), correo, rol, activo);
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "El correo ya está registrado.");
        }
        store.auditar(actor.id(), "USUARIO_EDITADO", id);
        return new UsuarioResponse(id, nombre.trim(), correo, rol, activo);
    }

    @Transactional
    public void eliminar(Usuario actor, long id) {
        var admins = store.bloquearAdmins();
        comprobar(actor);
        if (actor.id() == id) throw ApiException.invalido("No puedes eliminar tu propia cuenta.");
        var actual =
                store.porId(id)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "Usuario no encontrado."));
        if (actual.rol() == Rol.ADMIN_GENERAL && actual.activo() && admins.size() < 2)
            throw ApiException.invalido("Debe permanecer un administrador activo.");
        store.eliminar(id);
        store.auditar(actor.id(), "USUARIO_ELIMINADO_" + id, null);
    }
}
