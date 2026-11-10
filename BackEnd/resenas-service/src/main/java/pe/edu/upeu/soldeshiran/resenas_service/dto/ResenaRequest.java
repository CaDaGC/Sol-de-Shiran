package pe.edu.upeu.soldeshiran.resenas_service.dto;

import jakarta.validation.constraints.*;

public record ResenaRequest(@NotNull @Positive Long idReserva,
        @NotNull @Min(1) @Max(5) Integer puntuacion,
        @NotBlank @Size(max = 2000) String comentario) {}
