package pe.edu.upeu.soldeshiran.pagos_service.dto;

import java.math.BigDecimal;

public record PagoResponse(
        Long idTransaccion,
        Long idReserva,
        String estado,
        BigDecimal monto,
        String moneda,
        String idExternoPasarela,
        String mensaje
) {
}