package pe.upeu.edu.seguridad.dto.request;

import jakarta.validation.constraints.*;

public record RecuperacionRequest(@NotBlank @Email @Size(max = 254) String email) {}
