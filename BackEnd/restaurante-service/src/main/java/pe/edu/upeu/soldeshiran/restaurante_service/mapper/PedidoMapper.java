package pe.edu.upeu.soldeshiran.restaurante_service.mapper;

import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.DetallePedidoResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.PedidoResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Pedido;

@Component
public class PedidoMapper {

    public PedidoResponse toResponse(Pedido pedido) {
        var detalles = pedido.getDetalles().stream()
                .map(detalle -> new DetallePedidoResponse(
                        detalle.getProducto().getIdProducto(),
                        detalle.getNombreProducto(),
                        detalle.getCantidad(),
                        detalle.getPrecioUnitario(),
                        detalle.getSubtotal(),
                        detalle.getObservaciones()
                ))
                .toList();

        return new PedidoResponse(
                pedido.getIdPedido(),
                pedido.getCodigoPedido(),
                pedido.getRestaurante().getIdRestaurante(),
                pedido.getRestaurante().getNombre(),
                pedido.getIdReserva(),
                pedido.getNombreCabana(),
                pedido.getNombreHuesped(),
                pedido.getMontoTotal(),
                pedido.getEstadoPedido().name(),
                pedido.getMetodoPagoPresencial().name(),
                pedido.getEstadoPago().name(),
                pedido.getFechaPedido(),
                pedido.getFechaEntrega(),
                pedido.getMotivoRechazo(),
                detalles
        );
    }
}