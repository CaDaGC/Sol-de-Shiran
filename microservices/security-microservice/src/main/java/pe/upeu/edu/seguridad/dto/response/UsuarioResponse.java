package pe.upeu.edu.seguridad.dto.response;

import pe.upeu.edu.seguridad.enums.Rol;

public record UsuarioResponse(long id, String nombre, String email, Rol rol, boolean activo) {}
