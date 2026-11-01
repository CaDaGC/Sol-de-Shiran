package pe.upeu.edu.seguridad.dto.request;

import jakarta.validation.constraints.*;

public record CambiarPasswordRequest(
        @NotBlank @Size(max = 100) String actual,
        @NotBlank @Size(min = 12, max = 72) String nueva) {}
