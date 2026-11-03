package pe.edu.upeu.soldeshiran.pagos_service.dto;

import java.math.BigDecimal;

public record PreferenciaResponse(
        Long idTransaccion,
        String preferenceId,
        String sandboxInitPoint,
        BigDecimal monto,
        String moneda
) {
}