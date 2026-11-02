package pe.edu.upeu.soldeshiran.catalogo_service.service.impl;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaCatalogoResponse;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaDetalleResponse;
import pe.edu.upeu.soldeshiran.catalogo_service.mapper.CabanaMapper;
import pe.edu.upeu.soldeshiran.catalogo_service.model.EstadoCabana;
import pe.edu.upeu.soldeshiran.catalogo_service.repository.CabanaRepository;
import pe.edu.upeu.soldeshiran.catalogo_service.service.CabanaService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CabanaServiceImpl implements CabanaService {

    private final CabanaRepository cabanaRepository;
    private final CabanaMapper cabanaMapper;

    @Override
    public List<CabanaCatalogoResponse> listarCabanasActivas() {
        return cabanaRepository
                .findByEstadoOrderByNombreAsc(EstadoCabana.ACTIVA)
                .stream()
                .map(cabanaMapper::toCatalogoResponse)
                .toList();
    }

    @Override
    public CabanaDetalleResponse obtenerDetalleCabanaActiva(Long idCabana) {
        return cabanaRepository
                .buscarDetallePorIdYEstado(idCabana, EstadoCabana.ACTIVA)
                .map(cabanaMapper::toDetalleResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró una cabaña activa con el ID " + idCabana
                ));
    }
}