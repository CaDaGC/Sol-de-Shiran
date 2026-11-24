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

    @org.springframework.beans.factory.annotation.Value("${seguridad.internal-key}")
    private String internalKey;

    public MisReservasPaginaResponse consultar(Long idUsuario, GrupoReservasCliente grupo, int page, int size) {
        return consultar(idUsuario, grupo, page, size, null, null, null, null);
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public MisReservasPaginaResponse consultar(Long idUsuario, GrupoReservasCliente grupo, int page, int size,
            String codigo, String cabana, java.time.LocalDate desde, java.time.LocalDate hasta) {
        if (idUsuario == null || idUsuario <= 0 || page < 0 || size < 1 || size > 100
                || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parámetros de consulta inválidos.");
        }
        var ahora = LocalDateTime.now(ZoneId.of("America/Lima"));
        if (desde != null && hasta != null && desde.isAfter(hasta))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha desde debe ser anterior o igual a hasta.");
        Specification<Reserva> propietario = (root, query, cb) -> cb.equal(root.get("idUsuario"), idUsuario);
        Specification<Reserva> pendientes = (root, query, cb) -> cb.and(
                cb.equal(root.get("estadoReserva"), EstadoReserva.PENDIENTE_PAGO),
                cb.greaterThan(root.get("expiraEn"), ahora));
        Specification<Reserva> filtro = propietario;
        if (grupo == GrupoReservasCliente.PENDIENTES) filtro = filtro.and(pendientes);
        else if (grupo != GrupoReservasCliente.TODAS) {
            var estado = switch (grupo) {
                case CONFIRMADAS -> EstadoReserva.CONFIRMADA;
                case EN_ESTANCIA -> EstadoReserva.OCUPADA;
                case FINALIZADAS -> EstadoReserva.FINALIZADA;
                default -> throw new IllegalArgumentException("Grupo inválido");
            };
            filtro = filtro.and((root, query, cb) -> cb.equal(root.get("estadoReserva"), estado));
        }
        if (codigo != null && !codigo.isBlank()) {
            var patron = "%" + escapar(codigo.strip().toUpperCase(java.util.Locale.ROOT)) + "%";
            filtro = filtro.and((root, query, cb) -> cb.like(root.get("codigo"), patron, '\\'));
        }
        if (desde != null) filtro = filtro.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("fechaSalida"), desde));
        if (hasta != null) filtro = filtro.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("fechaIngreso"), hasta));
        if (cabana != null && !cabana.isBlank()) {
            try {
                var idsPropios = repository.listarCabanasDeUsuario(idUsuario);
                var texto = cabana.strip().toLowerCase(java.util.Locale.ROOT);
                var ids = idsPropios.isEmpty() ? List.<Long>of() : catalogo.consultarResumen(idsPropios, internalKey).stream()
                    .filter(c -> c.nombre() != null && c.nombre().toLowerCase(java.util.Locale.ROOT).contains(texto))
                    .map(CabanaResumenCliente::idCabana).toList();
                filtro = filtro.and((root, query, cb) -> root.get("idCabana").in(ids));
            } catch (FeignException e) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo filtrar por cabaña.", e);
            }
        }
        var pagina = repository.findAll(filtro, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "idReserva")));
        if (pagina.isEmpty()) {
            return new MisReservasPaginaResponse(List.of(), page, size, pagina.getTotalElements(), pagina.getTotalPages());
        }
        try {
            var ids = pagina.stream().map(Reserva::getIdCabana).distinct().toList();
            var cabanas = catalogo.consultarResumen(ids, internalKey).stream()
                    .collect(Collectors.toMap(CabanaResumenCliente::idCabana, Function.identity()));
            var contenido = pagina.stream().map(r -> {
                var estado = r.getEstadoReserva();
                boolean vencida = estado == EstadoReserva.PENDIENTE_PAGO && !r.getExpiraEn().isAfter(ahora);
                if (vencida) estado = EstadoReserva.EXPIRADA;
                boolean pendiente = estado == EstadoReserva.PENDIENTE_PAGO && r.finEstancia().isAfter(ahora);
                var categoria = switch (estado) {
                    case PENDIENTE_PAGO -> GrupoReservasCliente.PENDIENTES;
                    case CONFIRMADA -> GrupoReservasCliente.CONFIRMADAS;
                    case OCUPADA -> GrupoReservasCliente.EN_ESTANCIA;
                    case FINALIZADA -> GrupoReservasCliente.FINALIZADAS;
                    default -> GrupoReservasCliente.TODAS;
                };
                var resumenCabana = cabanas.get(r.getIdCabana());
                return new MiReservaResponse(r.getIdReserva(), r.getIdCabana(), r.getIdUsuario(),
                        resumenCabana == null ? null : resumenCabana.nombre(), resumenCabana == null ? null : resumenCabana.imagenPrincipal(),
                        r.getFechaIngreso(), r.getFechaSalida(), r.getCantidadHuespedes(), r.getTarifaNocheAplicada(),
                        r.getMontoTotal(), r.getMontoAdelanto(), r.getSaldoPendiente(), estado, r.getExpiraEn(),
                        categoria, pendiente && !r.inicioEstancia().isBefore(ahora), r.getHoraIngreso(), r.getHoraSalida(), r.getCodigo());
            }).toList();
            return new MisReservasPaginaResponse(contenido, page, size, pagina.getTotalElements(), pagina.getTotalPages());
        } catch (FeignException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudieron cargar los datos de tus reservas. Intenta nuevamente.", exception);
        }
    }
}
