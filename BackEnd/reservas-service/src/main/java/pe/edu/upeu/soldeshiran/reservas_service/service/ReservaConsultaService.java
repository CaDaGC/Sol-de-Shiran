package pe.edu.upeu.soldeshiran.reservas_service.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.reservas_service.client.PanelReservasClient;
import pe.edu.upeu.soldeshiran.reservas_service.client.PanelReservasClient.CabanaResumen;
import pe.edu.upeu.soldeshiran.reservas_service.client.PanelReservasClient.UsuarioResumen;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservaPanelResponse;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservaDetalleResponse;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservasPaginaResponse;
import pe.edu.upeu.soldeshiran.reservas_service.model.Reserva;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservaConsultaService {
    private final ReservaRepository reservaRepository;
    private final PanelReservasClient panelClient;

    public ReservaDetalleResponse consultarDetalle(String authorization, Long idReserva) {
        if (idReserva == null || idReserva <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de reserva debe ser positivo.");
        }
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada."));
        try {
            var usuario = panelClient.consultarUsuarios(authorization, null, List.of(reserva.getIdUsuario()))
                    .stream().filter(u -> u.id().equals(reserva.getIdUsuario())).findFirst().orElse(null);
            var cabana = panelClient.consultarCabanas(authorization, List.of(reserva.getIdCabana()))
                    .stream().filter(c -> c.idCabana().equals(reserva.getIdCabana())).findFirst().orElse(null);
            return new ReservaDetalleResponse(
                    reserva.getIdReserva(), reserva.getIdUsuario(),
                    usuario == null ? null : usuario.nombre(), usuario == null ? null : usuario.email(),
                    reserva.getIdCabana(), cabana == null ? null : cabana.nombre(),
                    reserva.getFechaIngreso(), reserva.getFechaSalida(), reserva.getCantidadHuespedes(),
                    reserva.getTarifaNocheAplicada(), reserva.getMontoTotal(), reserva.getMontoAdelanto(),
                    reserva.getSaldoPendiente(), reserva.getEstadoReserva(), reserva.getExpiraEn(),
                    reserva.getFechaCreacion());
        } catch (FeignException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudieron consultar los datos del detalle de reserva. Intenta nuevamente.", exception);
        }
    }

    public ReservasPaginaResponse consultar(String authorization, String buscar, Long idCabana,
                                           int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "idReserva"));
        Specification<Reserva> filtro = (root, query, cb) -> cb.conjunction();
        if (idCabana != null) {
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("idCabana"), idCabana));
        }
        try {
            Map<Long, UsuarioResumen> usuarios = null;
            if (buscar != null && !buscar.isBlank()) {
                usuarios = panelClient.consultarUsuarios(authorization, buscar.strip(), null).stream()
                        .collect(Collectors.toMap(UsuarioResumen::id, Function.identity()));
                if (usuarios.isEmpty()) {
                    return ReservasPaginaResponse.from(Page.empty(pageable));
                }
                var idsUsuarios = List.copyOf(usuarios.keySet());
                filtro = filtro.and((root, query, cb) -> root.get("idUsuario").in(idsUsuarios));
            }
            Page<Reserva> reservas = reservaRepository.findAll(filtro, pageable);
            if (reservas.isEmpty()) {
                return new ReservasPaginaResponse(List.of(), page, size,
                        reservas.getTotalElements(), reservas.getTotalPages());
            }
            if (usuarios == null) {
                var ids = reservas.stream().map(Reserva::getIdUsuario).distinct().toList();
                usuarios = panelClient.consultarUsuarios(authorization, null, ids).stream()
                        .collect(Collectors.toMap(UsuarioResumen::id, Function.identity()));
            }
            var idsCabanas = reservas.stream().map(Reserva::getIdCabana).distinct().toList();
            var cabanas = panelClient.consultarCabanas(authorization, idsCabanas).stream()
                    .collect(Collectors.toMap(CabanaResumen::idCabana, Function.identity()));
            var usuariosPagina = usuarios;
            return ReservasPaginaResponse.from(reservas.map(r -> {
                var usuario = usuariosPagina.get(r.getIdUsuario());
                var cabana = cabanas.get(r.getIdCabana());
                return new ReservaPanelResponse(r.getIdReserva(), r.getIdUsuario(),
                        usuario == null ? null : usuario.nombre(), usuario == null ? null : usuario.email(),
                        r.getIdCabana(), cabana == null ? null : cabana.nombre(),
                        r.getFechaIngreso(), r.getFechaSalida(), r.getCantidadHuespedes(),
                        r.getMontoTotal(), r.getMontoAdelanto(), r.getSaldoPendiente(), r.getEstadoReserva());
            }));
        } catch (FeignException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudieron consultar los datos del panel. Intenta nuevamente.", exception);
        }
    }
}
