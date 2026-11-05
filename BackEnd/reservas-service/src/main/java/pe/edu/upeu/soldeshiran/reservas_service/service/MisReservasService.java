package pe.edu.upeu.soldeshiran.reservas_service.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient;
import pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient.CabanaResumenCliente;
import pe.edu.upeu.soldeshiran.reservas_service.dto.*;
import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;
import pe.edu.upeu.soldeshiran.reservas_service.model.Reserva;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MisReservasService {
    private final ReservaRepository repository;
    private final CatalogoClient catalogo;

    public MisReservasPaginaResponse consultar(Long idUsuario, GrupoReservasCliente grupo, int page, int size) {
        if (idUsuario == null || idUsuario <= 0 || page < 0 || size < 1 || size > 100
                || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parámetros de consulta inválidos.");
        }
        var ahora = LocalDateTime.now(ZoneId.of("America/Lima"));
        var hoy = ahora.toLocalDate();
        Specification<Reserva> propietario = (root, query, cb) -> cb.equal(root.get("idUsuario"), idUsuario);
        Specification<Reserva> pendientes = (root, query, cb) -> cb.and(
                cb.equal(root.get("estadoReserva"), EstadoReserva.PENDIENTE_PAGO),
                cb.greaterThan(root.get("expiraEn"), ahora), cb.greaterThan(root.get("fechaSalida"), hoy));
        Specification<Reserva> confirmadas = (root, query, cb) -> cb.and(
                root.get("estadoReserva").in(EstadoReserva.CONFIRMADA, EstadoReserva.OCUPADA),
                cb.greaterThan(root.get("fechaSalida"), hoy));
        var filtro = switch (grupo) {
            case TODAS -> propietario;
            case PENDIENTES -> propietario.and(pendientes);
            case CONFIRMADAS -> propietario.and(confirmadas);
            case PASADAS -> propietario.and(Specification.not(pendientes.or(confirmadas)));
        };
        var pagina = repository.findAll(filtro, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "idReserva")));
        if (pagina.isEmpty()) {
            return new MisReservasPaginaResponse(List.of(), page, size, pagina.getTotalElements(), pagina.getTotalPages());
        }
        try {
            var ids = pagina.stream().map(Reserva::getIdCabana).distinct().toList();
            var cabanas = catalogo.consultarResumen(ids).stream()
                    .collect(Collectors.toMap(CabanaResumenCliente::idCabana, Function.identity()));
            var contenido = pagina.stream().map(r -> {
                var estado = r.getEstadoReserva();
                boolean vencida = estado == EstadoReserva.PENDIENTE_PAGO && !r.getExpiraEn().isAfter(ahora);
                if (vencida) estado = EstadoReserva.EXPIRADA;
                boolean pendiente = estado == EstadoReserva.PENDIENTE_PAGO && r.getFechaSalida().isAfter(hoy);
                boolean confirmada = (estado == EstadoReserva.CONFIRMADA || estado == EstadoReserva.OCUPADA)
                        && r.getFechaSalida().isAfter(hoy);
                var categoria = pendiente ? GrupoReservasCliente.PENDIENTES
                        : confirmada ? GrupoReservasCliente.CONFIRMADAS : GrupoReservasCliente.PASADAS;
                var cabana = cabanas.get(r.getIdCabana());
                return new MiReservaResponse(r.getIdReserva(), r.getIdCabana(), r.getIdUsuario(),
                        cabana == null ? null : cabana.nombre(), cabana == null ? null : cabana.imagenPrincipal(),
                        r.getFechaIngreso(), r.getFechaSalida(), r.getCantidadHuespedes(), r.getTarifaNocheAplicada(),
                        r.getMontoTotal(), r.getMontoAdelanto(), r.getSaldoPendiente(), estado, r.getExpiraEn(),
                        categoria, pendiente && !r.getFechaIngreso().isBefore(hoy));
            }).toList();
            return new MisReservasPaginaResponse(contenido, page, size, pagina.getTotalElements(), pagina.getTotalPages());
        } catch (FeignException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudieron cargar los datos de tus reservas. Intenta nuevamente.", exception);
        }
    }
}
