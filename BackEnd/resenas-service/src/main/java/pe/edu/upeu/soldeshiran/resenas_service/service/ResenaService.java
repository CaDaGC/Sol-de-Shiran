package pe.edu.upeu.soldeshiran.resenas_service.service;

import pe.edu.upeu.soldeshiran.resenas_service.dto.ElegibilidadResenaResponse;
import pe.edu.upeu.soldeshiran.resenas_service.dto.ResenaRequest;
import pe.edu.upeu.soldeshiran.resenas_service.dto.ResenasPaginaResponse;

public interface ResenaService {
    ResenasPaginaResponse listar(Long cabana, int pagina);
    ElegibilidadResenaResponse elegibilidad(Long cabana, Long usuario);
    void publicar(Long cabana, Long usuario, ResenaRequest solicitud);
}
