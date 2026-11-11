package pe.edu.upeu.soldeshiran.reservas_service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.reservas_service.security.Actor;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MetricasReservasControllerTest {
    @Test
    void cuentaSoloReservasVigentesPorFechaDeIngresoYExigeAdministrador() {
        var db = new DriverManagerDataSource("jdbc:h2:mem:metricas_reservas;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(db);
        jdbc.execute("CREATE TABLE reserva (id_cabana BIGINT, estado_reserva VARCHAR(30), fecha_ingreso DATE)");
        jdbc.update("INSERT INTO reserva VALUES (1, 'CONFIRMADA', '2026-11-02'), (1, 'FINALIZADA', '2026-11-03'), (2, 'OCUPADA', '2026-11-03'), (2, 'CANCELADA', '2026-11-03'), (3, 'CONFIRMADA', '2026-10-31')");
        var controller = new MetricasReservasController(jdbc);
        var solicitud = new MockHttpServletRequest();
        solicitud.setAttribute("actor", new Actor(1L, "ADMIN"));

        var resultado = controller.frecuencia(solicitud, LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03"));

        assertEquals(3, resultado.totalReservas());
        assertEquals(1L, resultado.ranking().getFirst().idCabana());
        assertEquals(2, resultado.ranking().getFirst().reservas());
        solicitud.setAttribute("actor", new Actor(2L, "CLIENTE"));
        assertThrows(ResponseStatusException.class,
                () -> controller.frecuencia(solicitud, LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03")));
    }
}
