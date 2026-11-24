package pe.edu.upeu.soldeshiran.pagos_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import pe.edu.upeu.soldeshiran.pagos_service.model.EstadoTransaccion;
import pe.edu.upeu.soldeshiran.pagos_service.model.TransaccionPago;

public record PagoRestanteResponse(Long id, Long idReserva, Long idTrabajador,
        String claveIdempotencia, BigDecimal monto, String metodoPago,
        String numeroOperacion, boolean completado, LocalDateTime fechaRegistro) {
    public static PagoRestanteResponse desde(TransaccionPago pago) {
        return new PagoRestanteResponse(pago.getIdTransaccion(), pago.getIdReserva(),
                pago.getIdTrabajador(), pago.getClaveIdempotenciaRestante(), pago.getMonto(),
                pago.getMetodoPago(), pago.getNumeroOperacionFisica(),
                pago.getEstadoTransaccion() == EstadoTransaccion.APROBADA, pago.getFechaTransaccion());
    }
}
