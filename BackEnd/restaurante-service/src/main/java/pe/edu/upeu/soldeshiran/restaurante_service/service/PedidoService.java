package pe.edu.upeu.soldeshiran.restaurante_service.service;

import pe.edu.upeu.soldeshiran.restaurante_service.dto.CrearPedidoRequest;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.CreacionPedidoResultado;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;

public interface PedidoService {

    CreacionPedidoResultado crear(
            CrearPedidoRequest request,
            Actor actor
    );
}