package pe.edu.upeu.soldeshiran.reservas_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservaPagoInternoResponse(
        Long idReserva,
        Long idUsuario,
        BigDecimal montoAdelanto,
        String estadoReserva,
        LocalDateTime expiraEn
) {
}