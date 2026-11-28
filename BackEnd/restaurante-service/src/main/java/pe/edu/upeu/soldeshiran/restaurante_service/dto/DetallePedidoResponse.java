package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import java.math.BigDecimal;

public record DetallePedidoResponse(
        Long idProducto,
        String nombreProducto,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal,
        String observaciones
) {
}