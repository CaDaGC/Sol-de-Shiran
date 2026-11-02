package pe.edu.upeu.soldeshiran.catalogo_service.service;

import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaCatalogoResponse;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaDetalleResponse;

import java.util.List;

public interface CabanaService {

    List<CabanaCatalogoResponse> listarCabanasActivas();

    CabanaDetalleResponse obtenerDetalleCabanaActiva(Long idCabana);
}