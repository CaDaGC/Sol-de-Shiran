package pe.edu.upeu.soldeshiran.reservas_service.service;

import java.util.List;
import pe.edu.upeu.soldeshiran.reservas_service.dto.EstadiaRestauranteResponse;

public interface EstadiaRestauranteService {

    List<EstadiaRestauranteResponse> consultar(Long idUsuario);
}