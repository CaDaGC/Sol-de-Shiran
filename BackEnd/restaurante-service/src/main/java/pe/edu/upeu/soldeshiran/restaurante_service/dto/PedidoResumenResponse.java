package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PedidoResumenResponse(
        Long idPedido,
        String codigoPedido,
        Long idRestaurante,
        String nombreRestaurante,
        Long idReserva,
        String nombreCabana,
        BigDecimal montoTotal,
        String estadoPedido,
        String metodoPagoPresencial,
        String estadoPago,
        LocalDateTime fechaPedido,
        LocalDateTime fechaEntrega,
        String motivoRechazo
) {
}