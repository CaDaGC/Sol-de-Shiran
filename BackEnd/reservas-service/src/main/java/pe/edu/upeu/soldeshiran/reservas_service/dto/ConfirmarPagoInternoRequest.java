package pe.edu.upeu.soldeshiran.reservas_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConfirmarPagoInternoRequest(
        @NotBlank String idTransaccionPasarela,
        @NotNull BigDecimal montoAprobado
) {
}