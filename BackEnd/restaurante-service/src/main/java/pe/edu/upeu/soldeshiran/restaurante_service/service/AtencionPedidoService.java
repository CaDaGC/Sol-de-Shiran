package pe.edu.upeu.soldeshiran.restaurante_service.service;

import pe.edu.upeu.soldeshiran.restaurante_service.dto.EstadoPedidoRequest;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.PedidoResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;

public interface AtencionPedidoService {

    PedidoResponse consultar(
            Long idRestaurante,
            Long idPedido,
            Actor actor
    );

    PedidoResponse cambiarEstado(
            Long idRestaurante,
            Long idPedido,
            EstadoPedidoRequest request,
            Actor actor
    );

    byte[] descargarComanda(
            Long idRestaurante,
            Long idPedido,
            Actor actor
    );
}