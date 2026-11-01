package pe.upeu.edu.seguridad.dto.request;

import jakarta.validation.constraints.*;

import pe.upeu.edu.seguridad.enums.Rol;

public record CrearUsuarioRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotNull Rol rol) {}
