package pe.edu.upeu.soldeshiran.pagos_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.pagos_service.security.Actor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pagos/metricas")
@RequiredArgsConstructor
public class MetricasIngresosController {
    private final JdbcTemplate jdbc;
    private final pe.edu.upeu.soldeshiran.pagos_service.client.ReservasReportesClient reservas;
    @org.springframework.beans.factory.annotation.Value("${seguridad.internal-key}")
    private String key;

    public record IngresoDia(LocalDate fecha, BigDecimal total, long operaciones) {}
    public record ResumenIngresos(LocalDate desde, LocalDate hasta, BigDecimal totalIngresos,
                                  long numeroPagos, List<IngresoDia> ingresosPorDia) {}

    @GetMapping("/ingresos")
    public ResumenIngresos ingresos(HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        var actor=Actor.desde(request);
        actor.exigir("ADMIN","DUENO");
        if (hasta.isBefore(desde) || desde.plusDays(366).isBefore(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango debe tener entre 1 y 367 días.");
        }

        var parametros = new java.util.ArrayList<Object>();
        parametros.add(desde.atStartOfDay()); parametros.add(hasta.plusDays(1).atStartOfDay());
        String filtro="";
        if (actor.es("DUENO")) {
            List<Long> propias;
            try { propias=reservas.propias(actor.id(),key); }
            catch(feign.FeignException e) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo verificar la propiedad de las reservas.",e);
            }
            if (propias.isEmpty()) return new ResumenIngresos(desde,hasta,BigDecimal.ZERO,0,List.of());
            filtro=" AND id_reserva IN ("+String.join(",",java.util.Collections.nCopies(propias.size(),"?"))+")";
            parametros.addAll(propias);
        }
        List<IngresoDia> dias = jdbc.query("""

                SELECT DATE(fecha_transaccion) AS dia, SUM(monto) AS total, COUNT(*) AS operaciones
                FROM transaccion_pago
                WHERE estado_transaccion = 'APROBADA'
                  AND fecha_transaccion >= ? AND fecha_transaccion < ?
                """ + filtro + """
                GROUP BY DATE(fecha_transaccion)
                ORDER BY dia
                """, (rs, row) -> new IngresoDia(rs.getDate("dia").toLocalDate(),
                        rs.getBigDecimal("total"), rs.getLong("operaciones")),
                parametros.toArray());
        BigDecimal total = dias.stream().map(IngresoDia::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        long pagos = dias.stream().mapToLong(IngresoDia::operaciones).sum();
        return new ResumenIngresos(desde, hasta, total, pagos, dias);
    }
}
