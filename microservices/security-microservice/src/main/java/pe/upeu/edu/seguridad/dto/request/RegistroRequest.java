package pe.upeu.edu.seguridad.dto.request;

import jakarta.validation.constraints.*;

public record RegistroRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 12, max = 72) String password) {}
