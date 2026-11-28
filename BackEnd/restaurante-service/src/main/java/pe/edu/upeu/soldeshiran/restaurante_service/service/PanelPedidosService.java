package pe.edu.upeu.soldeshiran.restaurante_service.service;

import pe.edu.upeu.soldeshiran.restaurante_service.dto.PanelPedidosResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoPedido;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;

public interface PanelPedidosService {

    PanelPedidosResponse consultar(
            Long idRestaurante,
            EstadoPedido estado,
            String cabana,
            int pagina,
            int tamanio,
            Actor actor
    );
}