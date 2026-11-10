package pe.edu.upeu.soldeshiran.resenas_service.service.impl;

import feign.FeignException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.resenas_service.client.ReservasClient;
import pe.edu.upeu.soldeshiran.resenas_service.client.UsuariosClient;
import pe.edu.upeu.soldeshiran.resenas_service.dto.AutorResenaResponse;
import pe.edu.upeu.soldeshiran.resenas_service.dto.ElegibilidadResenaResponse;
import pe.edu.upeu.soldeshiran.resenas_service.dto.ResenaRequest;
import pe.edu.upeu.soldeshiran.resenas_service.dto.ResenasPaginaResponse;
import pe.edu.upeu.soldeshiran.resenas_service.mapper.ResenaMapper;
import pe.edu.upeu.soldeshiran.resenas_service.service.ResenaService;
import pe.edu.upeu.soldeshiran.resenas_service.model.Resena;
import pe.edu.upeu.soldeshiran.resenas_service.repository.ResenaRepository;

@Service
@RequiredArgsConstructor
public class ResenaServiceImpl implements ResenaService {
    private final ResenaRepository resenas;
    private final UsuariosClient usuarios;
    private final ReservasClient reservas;
    private final ResenaMapper mapper;

    @Value("${seguridad.internal-key:sol-shiran-interno-desarrollo-cambiar}")
    private String clave;

    @Override
    public ResenasPaginaResponse listar(Long cabana, int pagina) {
        var filas = resenas.findByIdCabanaOrderByIdResenaDesc(cabana, PageRequest.of(pagina, 20));
        if (filas.isEmpty()) return new ResenasPaginaResponse(List.of(), false);
        try {
            var ids = filas.stream().map(Resena::getIdUsuario).distinct().toList();
            var autores = usuarios.autores(ids, clave).stream()
                    .collect(Collectors.toMap(AutorResenaResponse::id, AutorResenaResponse::nombre));
            return new ResenasPaginaResponse(filas.stream().map(r -> mapper.toResponse(r, autores.getOrDefault(r.getIdUsuario(), "Cliente"))).toList(), filas.hasNext());
        } catch (FeignException e) {
            throw indisponible(e);
        }
    }

    @Override
    public ElegibilidadResenaResponse elegibilidad(Long cabana, Long usuario) {
        var usadas = new HashSet<>(resenas.reservasResenadas(usuario, cabana));
        return new ElegibilidadResenaResponse(estancias(usuario, cabana).stream()
                .filter(id -> !usadas.contains(id)).findFirst().orElse(null));
    }

    @Override
    public void publicar(Long cabana, Long usuario, ResenaRequest solicitud) {
        if (!estancias(usuario, cabana).contains(solicitud.idReserva())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo puedes reseñar una estancia tuya que ya finalizó en esta cabaña.");
        }
        try {
            resenas.saveAndFlush(new Resena(cabana, usuario, solicitud.idReserva(),
                    solicitud.puntuacion(), solicitud.comentario().strip()));
        } catch (DataIntegrityViolationException e) {
            if (!resenas.reservasResenadas(usuario, cabana).contains(solicitud.idReserva())) throw e;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya publicaste una reseña para esta estancia.");
        }
    }

    private List<Long> estancias(Long usuario, Long cabana) {
        try {
            return reservas.estancias(usuario, cabana, clave);
        } catch (FeignException e) {
            throw indisponible(e);
        }
    }

    private ResponseStatusException indisponible(FeignException e) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo completar la consulta de reseñas. Intenta nuevamente.", e);
    }
}
