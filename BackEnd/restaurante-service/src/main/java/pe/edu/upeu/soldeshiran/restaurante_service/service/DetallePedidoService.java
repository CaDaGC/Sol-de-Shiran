package pe.edu.upeu.soldeshiran.restaurante_service.service;

import pe.edu.upeu.soldeshiran.restaurante_service.dto.PedidoResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;

public interface DetallePedidoService {

    PedidoResponse consultar(Long idPedido, Actor actor);
}