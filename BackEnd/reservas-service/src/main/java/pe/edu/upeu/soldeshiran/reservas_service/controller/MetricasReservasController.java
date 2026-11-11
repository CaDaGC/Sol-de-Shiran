package pe.edu.upeu.soldeshiran.reservas_service.controller;

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
import pe.edu.upeu.soldeshiran.reservas_service.security.Actor;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reservas/metricas")
@RequiredArgsConstructor
public class MetricasReservasController {
    private final JdbcTemplate jdbc;

    public record FrecuenciaCabana(long idCabana, long reservas) {}
    public record ResumenReservas(LocalDate desde, LocalDate hasta, long totalReservas,
                                  List<FrecuenciaCabana> ranking) {}

    @GetMapping("/frecuencia")
    public ResumenReservas frecuencia(HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        Actor.desde(request).exigir("ADMIN");
        if (hasta.isBefore(desde) || desde.plusDays(366).isBefore(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rango debe tener entre 1 y 367 días.");
        }
        // Se cuenta la fecha de ingreso; reservas canceladas o sin pago no son ventas.
        List<FrecuenciaCabana> ranking = jdbc.query("""
                SELECT id_cabana, COUNT(*) AS reservas
                FROM reserva
                WHERE estado_reserva IN ('CONFIRMADA', 'OCUPADA', 'FINALIZADA')
                  AND fecha_ingreso >= ? AND fecha_ingreso <= ?
                GROUP BY id_cabana
                ORDER BY reservas DESC, id_cabana
                """, (rs, row) -> new FrecuenciaCabana(rs.getLong("id_cabana"),
                        rs.getLong("reservas")), desde, hasta);
        long total = ranking.stream().mapToLong(FrecuenciaCabana::reservas).sum();
        return new ResumenReservas(desde, hasta, total, ranking);
    }
}
