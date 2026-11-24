package pe.edu.upeu.soldeshiran.pagos_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ConfirmarPagoRequest(
        @NotBlank String idTransaccionPasarela,
        @NotNull BigDecimal montoAprobado
) {
}