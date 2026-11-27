package pe.edu.upeu.soldeshiran.restaurante_service.service;

import java.util.List;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.EstadiaActivaResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.RestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;

public interface RestauranteConsultaService {

    EstadiaActivaResponse consultarEstadias(
            Long idUsuario,
            Actor actor
    );

    List<RestauranteResponse> listarLocales(Actor actor);

    RestauranteResponse consultarMiLocal(
            Long idUsuario,
            Actor actor
    );
}