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

    public record IngresoDia(LocalDate fecha, BigDecimal total, long operaciones) {}
    public record ResumenIngresos(LocalDate desde, LocalDate hasta, BigDecimal totalIngresos,
                                  long numeroPagos, List<IngresoDia> ingresosPorDia) {}

    @GetMapping("/ingresos")
    public ResumenIngresos ingresos(HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        Actor.desde(request).exigir("ADMIN");
        if (hasta.isBefore(desde) || desde.plusDays(366).isBefore(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango debe tener entre 1 y 367 días.");
        }
        // Solo pagos acreditados. El día corresponde al registro de la transacción local.
        List<IngresoDia> dias = jdbc.query("""
                SELECT DATE(fecha_transaccion) AS dia, SUM(monto) AS total, COUNT(*) AS operaciones
                FROM transaccion_pago
                WHERE estado_transaccion = 'APROBADA'
                  AND fecha_transaccion >= ? AND fecha_transaccion < ?
                GROUP BY DATE(fecha_transaccion)
                ORDER BY dia
                """, (rs, row) -> new IngresoDia(rs.getDate("dia").toLocalDate(),
                        rs.getBigDecimal("total"), rs.getLong("operaciones")),
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());
        BigDecimal total = dias.stream().map(IngresoDia::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        long pagos = dias.stream().mapToLong(IngresoDia::operaciones).sum();
        return new ResumenIngresos(desde, hasta, total, pagos, dias);
    }
}
