package pe.edu.upeu.soldeshiran.catalogo_service.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record CabanaRequest(
    @NotBlank @Size(max = 100) String nombre,
    @NotBlank @Size(max = 10000) String descripcion,
    @NotNull @Min(1) @Max(1000) Integer aforoMaximo,
    @NotNull @Min(1) @Max(1000) Integer numHabitaciones,
    @NotNull @Min(1) @Max(1000) Integer numBanos,
    @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal tarifaNoche,
    @NotBlank @Size(max = 255) @Pattern(regexp = "^(https?://|/)[^\\s]*$") String imagenPrincipal,
    @Size(max = 20)
        List<@NotBlank @Size(max = 255) @Pattern(regexp = "^(https?://|/)[^\\s]*$") String>
            imagenesSecundarias,
    @Positive Long duenoId) {}
