package pe.edu.upeu.soldeshiran.pagos_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservaPagoResponse(
        Long idReserva,
        Long idUsuario,
        BigDecimal montoAdelanto,
        String estadoReserva,
        LocalDateTime expiraEn
) {
}