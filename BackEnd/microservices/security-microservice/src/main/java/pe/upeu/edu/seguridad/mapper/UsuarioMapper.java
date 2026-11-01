package pe.upeu.edu.seguridad.mapper;

import pe.upeu.edu.seguridad.dto.response.UsuarioResponse;
import pe.upeu.edu.seguridad.entity.Usuario;

public final class UsuarioMapper {
    private UsuarioMapper() {}

    public static UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.id(), usuario.nombre(), usuario.email(), usuario.rol(), usuario.activo());
    }
}
