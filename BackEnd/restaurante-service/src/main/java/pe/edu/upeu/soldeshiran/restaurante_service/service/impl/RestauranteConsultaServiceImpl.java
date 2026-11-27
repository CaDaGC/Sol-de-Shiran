package pe.edu.upeu.soldeshiran.restaurante_service.service.impl;

import feign.FeignException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.restaurante_service.client.ReservasClient;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.EstadiaActivaResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.EstadiaResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.RestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.mapper.RestauranteMapper;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoRestaurante;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.RestauranteRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.RestauranteTrabajadorRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.RestauranteConsultaService;

@Service
@RequiredArgsConstructor
public class RestauranteConsultaServiceImpl
        implements RestauranteConsultaService {

    private final ReservasClient reservas;
    private final RestauranteRepository restaurantes;
    private final RestauranteTrabajadorRepository asignaciones;
    private final RestauranteMapper mapper;

    @Value("${seguridad.internal-key}")
    private String claveInterna;

    @Override
    public EstadiaActivaResponse consultarEstadias(
            Long idUsuario,
            Actor actor) {

        actor.exigir("CLIENTE");
        exigirUsuarioPropio(idUsuario, actor);

        List<EstadiaResponse> estadias;

        try {
            estadias = reservas.estadias(
                    actor.id(),
                    claveInterna
            );
        } catch (FeignException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo comprobar tu estadía. Intenta nuevamente."
            );
        }

        if (estadias == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Reservas no devolvió una respuesta válida."
            );
        }

        return new EstadiaActivaResponse(
                !estadias.isEmpty(),
                estadias.size() > 1,
                List.copyOf(estadias)
        );
    }

    @Override
    public List<RestauranteResponse> listarLocales(Actor actor) {
        var estadia = consultarEstadias(actor.id(), actor);

        if (!estadia.activa()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Necesitas una estadía activa para acceder a los restaurantes."
            );
        }

        return restaurantes
                .findByEstadoOrderByNombreAsc(EstadoRestaurante.ACTIVO)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RestauranteResponse consultarMiLocal(
            Long idUsuario,
            Actor actor) {

        actor.exigir("RESTAURANTE");
        exigirUsuarioPropio(idUsuario, actor);

        var asignacion = asignaciones
                .findByIdUsuario(actor.id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tu cuenta todavía no tiene un restaurante asignado."
                ));

        var restaurante = asignacion.getRestaurante();

        if (restaurante.getEstado() != EstadoRestaurante.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Tu restaurante está inactivo."
            );
        }

        return mapper.toResponse(restaurante);
    }

    private void exigirUsuarioPropio(Long idUsuario, Actor actor) {
        if (idUsuario == null || idUsuario <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El usuario debe ser válido."
            );
        }

        if (!idUsuario.equals(actor.id())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo puedes consultar tu propia información."
            );
        }
    }
}