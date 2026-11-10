package pe.edu.upeu.soldeshiran.pagos_service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.upeu.soldeshiran.pagos_service.service.MigracionPagoRestante;

@SpringBootTest
@ActiveProfiles("test")
class PagosServiceApplicationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired MigracionPagoRestante migracion;
    @Autowired pe.edu.upeu.soldeshiran.pagos_service.service.PagoDanioService danos;
    @Autowired pe.edu.upeu.soldeshiran.pagos_service.repository.TransaccionPagoRepository pagos;

    @Test
    void registraUnaSolaTransaccionPresencialPorDanios() {
        var fecha = java.time.LocalDateTime.of(2026, 11, 9, 23, 37);
        var cobro = new pe.edu.upeu.soldeshiran.pagos_service.service.PagoDanioService.Cobro(
                2L, "Daño de prueba", new java.math.BigDecimal("50.00"), "EFECTIVO", fecha);
        try {
            danos.registrar(29L, cobro);
            danos.registrar(29L, cobro);
            var pago = pagos.findByReservaPagoDanos(29L).orElseThrow();
            assertEquals("DANOS", pago.getTipoPago());
            assertEquals("EFECTIVO", pago.getMetodoPago());
            assertEquals("APROBADA", pago.getEstadoTransaccion().name());
            assertEquals(fecha, pago.getFechaTransaccion());
            assertNull(pago.getIdExternoPasarela());
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM transaccion_pago WHERE reserva_pago_danos = 29", Integer.class));
        } finally {
            jdbc.update("DELETE FROM transaccion_pago WHERE reserva_pago_danos = 29");
        }
    }

    @Test
    void copiaYVerificaElPagoHistoricoAntesDeRetirarLaTabla() {
        jdbc.execute("""
                CREATE TABLE pago_restante (
                    id BIGINT PRIMARY KEY, id_reserva BIGINT, id_trabajador BIGINT,
                    clave_idempotencia VARCHAR(36), numero_operacion VARCHAR(100),
                    monto DECIMAL(10,2), metodo_pago VARCHAR(30), completado BOOLEAN,
                    fecha_registro TIMESTAMP
                )
                """);
        try {
            jdbc.update("""
                    INSERT INTO pago_restante VALUES
                    (7, 3, 2, 'historia-7', NULL, 125.00, 'EFECTIVO', TRUE, TIMESTAMP '2026-11-09 10:00:00')
                    """);
            migracion.run(null);
            migracion.run(null);
            assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM transaccion_pago WHERE tipo_pago = 'RESTANTE'", Integer.class));
            assertEquals("RESTANTE-7", jdbc.queryForObject("SELECT referencia_check_in FROM transaccion_pago WHERE reserva_pago_restante = 3", String.class));
            assertEquals("APROBADA", jdbc.queryForObject("SELECT estado_transaccion FROM transaccion_pago WHERE reserva_pago_restante = 3", String.class));
            ReflectionTestUtils.setField(migracion, "retirarTabla", true);
            migracion.run(null);
            assertThrows(org.springframework.dao.DataAccessException.class,
                    () -> jdbc.queryForObject("SELECT COUNT(*) FROM pago_restante", Integer.class));
        } finally {
            ReflectionTestUtils.setField(migracion, "retirarTabla", false);
            jdbc.execute("DROP TABLE IF EXISTS pago_restante");
            jdbc.update("DELETE FROM transaccion_pago WHERE reserva_pago_restante = 3");
        }
    }
}
