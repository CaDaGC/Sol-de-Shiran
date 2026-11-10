package pe.edu.upeu.soldeshiran.resenas_service.mapper;

import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.resenas_service.dto.ResenaResponse;
import pe.edu.upeu.soldeshiran.resenas_service.model.Resena;

@Component
public class ResenaMapper {
    public ResenaResponse toResponse(Resena resena, String autor) {
        return new ResenaResponse(resena.getIdResena(), autor, resena.getPuntuacion(),
                resena.getComentario(), resena.getFechaCreacion());
    }
}
