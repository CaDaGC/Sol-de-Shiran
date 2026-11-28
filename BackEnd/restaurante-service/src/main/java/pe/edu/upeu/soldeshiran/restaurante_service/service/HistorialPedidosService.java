package pe.edu.upeu.soldeshiran.restaurante_service.service;

import pe.edu.upeu.soldeshiran.restaurante_service.dto.HistorialPedidosResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;

public interface HistorialPedidosService {

    HistorialPedidosResponse consultar(
            Long idUsuario,
            Long idReserva,
            int pagina,
            int tamanio,
            Actor actor
    );
}