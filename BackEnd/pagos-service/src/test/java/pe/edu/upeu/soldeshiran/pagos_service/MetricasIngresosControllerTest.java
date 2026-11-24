package pe.edu.upeu.soldeshiran.pagos_service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.pagos_service.controller.MetricasIngresosController;
import pe.edu.upeu.soldeshiran.pagos_service.security.Actor;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MetricasIngresosControllerTest {
    @Test
    void sumaSoloPagosAprobadosEnElRangoYExigeAdministrador() {
        var db = new DriverManagerDataSource("jdbc:h2:mem:metricas_pagos;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(db);
        jdbc.execute("CREATE TABLE transaccion_pago (monto DECIMAL(10,2), estado_transaccion VARCHAR(30), fecha_transaccion TIMESTAMP)");
        jdbc.update("INSERT INTO transaccion_pago VALUES (100.00, 'APROBADA', '2026-11-02 10:00:00'), (50.00, 'APROBADA', '2026-11-02 11:00:00'), (20.00, 'PENDIENTE', '2026-11-02 12:00:00'), (10.00, 'APROBADA', '2026-10-31 10:00:00')");
        var reservas = org.mockito.Mockito.mock(pe.edu.upeu.soldeshiran.pagos_service.client.ReservasReportesClient.class);
        var controller = new MetricasIngresosController(jdbc, reservas);
        var solicitud = new MockHttpServletRequest();
        solicitud.setAttribute("actor", new Actor(1L, "ADMIN"));

        var resultado = controller.ingresos(solicitud, LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03"));

        assertEquals(0, new BigDecimal("150.00").compareTo(resultado.totalIngresos()));
        assertEquals(2, resultado.numeroPagos());
        assertEquals(1, resultado.ingresosPorDia().size());
        jdbc.execute("ALTER TABLE transaccion_pago ADD COLUMN id_reserva BIGINT");
        jdbc.update("UPDATE transaccion_pago SET id_reserva=29 WHERE monto=100");
        jdbc.update("UPDATE transaccion_pago SET id_reserva=30 WHERE monto=50");
        solicitud.setAttribute("actor", new Actor(10L, "DUENO"));
        org.mockito.Mockito.when(reservas.propias(org.mockito.ArgumentMatchers.eq(10L),org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(java.util.List.of(30L));
        var propio=controller.ingresos(solicitud,LocalDate.parse("2026-11-01"),LocalDate.parse("2026-11-03"));
        assertEquals(0,new BigDecimal("50.00").compareTo(propio.totalIngresos()));
        assertEquals(1,propio.numeroPagos());
        org.mockito.Mockito.when(reservas.propias(org.mockito.ArgumentMatchers.eq(10L),org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(java.util.List.of());
        assertEquals(0,controller.ingresos(solicitud,LocalDate.parse("2026-11-01"),LocalDate.parse("2026-11-03")).numeroPagos());
        solicitud.setAttribute("actor", new Actor(2L, "CLIENTE"));
        assertThrows(ResponseStatusException.class,
                () -> controller.ingresos(solicitud, LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03")));
    }
}
