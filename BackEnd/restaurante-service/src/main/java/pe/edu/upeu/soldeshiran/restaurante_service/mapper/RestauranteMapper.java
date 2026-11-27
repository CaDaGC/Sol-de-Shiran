package pe.edu.upeu.soldeshiran.restaurante_service.mapper;

import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.RestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Restaurante;

@Component
public class RestauranteMapper {

    public RestauranteResponse toResponse(Restaurante restaurante) {
        return new RestauranteResponse(
                restaurante.getIdRestaurante(),
                restaurante.getNombre(),
                restaurante.getEspecialidad(),
                restaurante.getHorarioAtencion(),
                restaurante.getImagenUrl(),
                restaurante.getEstado().name()
        );
    }
}