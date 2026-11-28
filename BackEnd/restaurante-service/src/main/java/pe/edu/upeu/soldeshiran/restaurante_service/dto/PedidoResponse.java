package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
        Long idPedido,
        String codigoPedido,
        Long idRestaurante,
        String nombreRestaurante,
        Long idReserva,
        String nombreCabana,
        String nombreHuesped,
        BigDecimal montoTotal,
        String estadoPedido,
        String metodoPagoPresencial,
        String estadoPago,
        LocalDateTime fechaPedido,
        LocalDateTime fechaEntrega,
        String motivoRechazo,
        List<DetallePedidoResponse> detalles
) {
}