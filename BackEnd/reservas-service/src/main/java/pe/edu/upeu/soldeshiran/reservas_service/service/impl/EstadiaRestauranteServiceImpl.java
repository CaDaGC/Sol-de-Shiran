package pe.edu.upeu.soldeshiran.reservas_service.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.reservas_service.client.PanelReservasClient;
import pe.edu.upeu.soldeshiran.reservas_service.dto.EstadiaRestauranteResponse;
import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;
import pe.edu.upeu.soldeshiran.reservas_service.service.EstadiaRestauranteService;

@Service
@RequiredArgsConstructor
public class EstadiaRestauranteServiceImpl
        implements EstadiaRestauranteService {

    private final ReservaRepository reservas;
    private final PanelReservasClient datos;

    @Value("${seguridad.internal-key}")
    private String claveInterna;

    @Override
    public List<EstadiaRestauranteResponse> consultar(Long idUsuario) {
        if (idUsuario == null || idUsuario <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El usuario debe ser válido."
            );
        }

        LocalDateTime ahora = LocalDateTime.now(
                ZoneId.of("America/Lima")
        );

        var activas = reservas
                .findByIdUsuarioAndEstadoReservaOrderByFechaIngresoAscIdReservaAsc(
                        idUsuario,
                        EstadoReserva.OCUPADA
                )
                .stream()
                .filter(reserva ->
                        !ahora.isBefore(reserva.inicioEstancia())
                                && ahora.isBefore(reserva.finEstancia())
                )
                .toList();

        if (activas.isEmpty()) {
            return List.of();
        }

        try {
            var usuario = datos.consultarUsuarios(
                            claveInterna,
                            null,
                            List.of(idUsuario)
                    )
                    .stream()
                    .filter(item -> idUsuario.equals(item.id()))
                    .findFirst()
                    .orElseThrow(() -> noDisponible(
                            "No se encontraron los datos del huésped."
                    ));

            String nombreHuesped = usuario.nombre();

            if (usuario.persona() != null) {
                String nombres = usuario.persona().nombres();
                String apellidos = usuario.persona().apellidos();

                String nombreCompleto = (
                        (nombres == null ? "" : nombres)
                                + " "
                                + (apellidos == null ? "" : apellidos)
                ).trim();

                if (!nombreCompleto.isBlank()) {
                    nombreHuesped = nombreCompleto;
                }
            }

            if (nombreHuesped == null || nombreHuesped.isBlank()) {
                throw noDisponible(
                        "El huésped no tiene un nombre disponible."
                );
            }

            var idsCabanas = activas.stream()
                    .map(reserva -> reserva.getIdCabana())
                    .distinct()
                    .toList();

            var cabanas = datos.consultarCabanas(
                            claveInterna,
                            idsCabanas
                    )
                    .stream()
                    .collect(Collectors.toMap(
                            PanelReservasClient.CabanaResumen::idCabana,
                            Function.identity()
                    ));

            String nombreFinal = nombreHuesped;

            return activas.stream().map(reserva -> {
                var cabana = cabanas.get(reserva.getIdCabana());

                if (cabana == null
                        || cabana.nombre() == null
                        || cabana.nombre().isBlank()) {
                    throw noDisponible(
                            "No se pudieron obtener los datos de la cabaña."
                    );
                }

                return new EstadiaRestauranteResponse(
                        reserva.getIdReserva(),
                        reserva.getCodigo(),
                        reserva.getIdCabana(),
                        cabana.nombre(),
                        nombreFinal
                );
            }).toList();

        } catch (RestClientException exception) {
            throw noDisponible(
                    "No se pudo consultar la información de la estadía."
            );
        }
    }

    private ResponseStatusException noDisponible(String mensaje) {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                mensaje
        );
    }
}