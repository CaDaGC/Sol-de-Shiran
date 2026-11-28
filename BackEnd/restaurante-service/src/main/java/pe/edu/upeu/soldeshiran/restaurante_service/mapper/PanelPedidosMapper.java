package pe.edu.upeu.soldeshiran.restaurante_service.mapper;

import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.PedidoPanelResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Pedido;

@Component
public class PanelPedidosMapper {

    public PedidoPanelResponse toResponse(Pedido pedido) {
        return new PedidoPanelResponse(
                pedido.getIdPedido(),
                pedido.getCodigoPedido(),
                pedido.getNombreCabana(),
                pedido.getNombreHuesped(),
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