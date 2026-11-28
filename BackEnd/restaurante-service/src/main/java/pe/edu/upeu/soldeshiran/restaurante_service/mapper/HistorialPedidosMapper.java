package pe.edu.upeu.soldeshiran.restaurante_service.mapper;

import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.PedidoResumenResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Pedido;

@Component
public class HistorialPedidosMapper {

    public PedidoResumenResponse toResponse(Pedido pedido) {
        return new PedidoResumenResponse(
                pedido.getIdPedido(),
                pedido.getCodigoPedido(),
                pedido.getRestaurante().getIdRestaurante(),
                pedido.getRestaurante().getNombre(),
                pedido.getIdReserva(),
                pedido.getNombreCabana(),
                pedido.getMontoTotal(),
                pedido.getEstadoPedido().name(),
                pedido.getMetodoPagoPresencial().name(),
                pedido.getEstadoPago().name(),
                pedido.getFechaPedido(),
                pedido.getFechaEntrega(),
                pedido.getMotivoRechazo()
        );
    }
}