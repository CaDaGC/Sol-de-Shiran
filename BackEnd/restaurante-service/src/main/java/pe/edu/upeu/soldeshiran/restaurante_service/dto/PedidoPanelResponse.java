package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PedidoPanelResponse(
        Long idPedido,
        String codigoPedido,
        String nombreCabana,
        String nombreHuesped,
        BigDecimal montoTotal,
        String estadoPedido,
        String metodoPagoPresencial,
        String estadoPago,
        LocalDateTime fechaPedido,
        LocalDateTime fechaEntrega,
        String motivoRechazo
) {
}