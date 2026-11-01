package pe.upeu.edu.seguridad.dto.request;

import jakarta.validation.constraints.*;

import pe.upeu.edu.seguridad.enums.Rol;

public record EditarUsuarioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 254) String email,
        @NotNull Rol rol,
        @NotNull Boolean activo) {}
