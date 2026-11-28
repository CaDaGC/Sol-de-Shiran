package pe.edu.upeu.soldeshiran.restaurante_service.mapper;

import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.RestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Restaurante;

@Component
@lombok.RequiredArgsConstructor
public class RestauranteMapper {
    private final pe.edu.upeu.soldeshiran.restaurante_service.service.HorarioRestauranteService horarios;

    public RestauranteResponse toResponse(Restaurante restaurante) {
        return new RestauranteResponse(
                restaurante.getIdRestaurante(),
                restaurante.getNombre(),
                restaurante.getEspecialidad(),
                horarios.describirHorario(),
                restaurante.getImagenUrl(),
                restaurante.getEstado().name()
        );
    }
}