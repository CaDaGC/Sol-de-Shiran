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
import pe.edu.upeu.soldeshiran.restaurante_service.dto.InicioRestaurantesResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.RestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.SeleccionRestauranteResponse;
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
        var estadias = consultarEstadias(actor.id(), actor);

        if (!estadias.activa()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Necesitas una estadía activa para acceder a los restaurantes."
            );
        }

        return consultarLocalesActivos();
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

        return mapper.toResponse(restaurante);
    }

    @Override
    public InicioRestaurantesResponse consultarInicio(
            Long idReserva,
            Actor actor) {

        var estadias = consultarEstadias(actor.id(), actor);

        var entrega = resolverEstadia(
                estadias.estadias(),
                idReserva
        );

        List<RestauranteResponse> locales = entrega == null
                ? List.of()
                : consultarLocalesActivos();

        return new InicioRestaurantesResponse(
                estadias,
                entrega,
                locales
        );
    }

    @Override
    public SeleccionRestauranteResponse seleccionarRestaurante(
            Long idRestaurante,
            Long idReserva,
            Actor actor) {

        var estadias = consultarEstadias(actor.id(), actor);

        if (!estadias.activa()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Necesitas una estadía activa para seleccionar un restaurante."
            );
        }

        var entrega = resolverEstadia(
                estadias.estadias(),
                idReserva
        );

        if (entrega == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Selecciona primero la cabaña donde recibirás tu pedido."
            );
        }

        if (idRestaurante == null || idRestaurante <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El restaurante debe ser válido."
            );
        }

        var restaurante = restaurantes.findById(idRestaurante)
                .filter(local ->
                        local.getEstado() == EstadoRestaurante.ACTIVO
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El restaurante no está disponible."
                ));

        return new SeleccionRestauranteResponse(
                entrega,
                mapper.toResponse(restaurante)
        );
    }

    private List<RestauranteResponse> consultarLocalesActivos() {
        return restaurantes
                .findByEstadoOrderByNombreAsc(EstadoRestaurante.ACTIVO)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    private EstadiaResponse resolverEstadia(
            List<EstadiaResponse> estadias,
            Long idReserva) {

        if (idReserva != null) {
            if (idReserva <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La reserva debe ser válida."
                );
            }

            return estadias.stream()
                    .filter(estadia ->
                            idReserva.equals(estadia.idReserva())
                    )
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "La reserva seleccionada no corresponde a una estadía activa de tu cuenta."
                    ));
        }

        if (estadias.size() == 1) {
            return estadias.getFirst();
        }

        return null;
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