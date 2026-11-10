package pe.edu.upeu.soldeshiran.pagos_service.service;

import java.sql.ResultSet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
public class MigracionPagoRestante implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacciones;

    @Value("${pagos.migracion.retirar-tabla-restante:false}")
    private boolean retirarTabla;

    @Override
    public void run(ApplicationArguments args) {
        if (!existeTablaAnterior()) return;

        transacciones.executeWithoutResult(status -> {
            jdbc.update("""
                    INSERT INTO transaccion_pago
                        (id_reserva, tipo_pago, reserva_pago_restante, id_trabajador,
                         clave_idempotencia_restante, referencia_check_in, numero_operacion_fisica,
                         monto, moneda, metodo_pago, estado_transaccion, fecha_transaccion)
                    SELECT p.id_reserva, 'RESTANTE', p.id_reserva, p.id_trabajador,
                           p.clave_idempotencia, CONCAT('RESTANTE-', p.id), p.numero_operacion,
                           p.monto, 'PEN', p.metodo_pago,
                           CASE WHEN p.completado THEN 'APROBADA' ELSE 'PENDIENTE' END,
                           p.fecha_registro
                    FROM pago_restante p
                    WHERE NOT EXISTS (
                        SELECT 1 FROM transaccion_pago t WHERE t.reserva_pago_restante = p.id_reserva
                    )
                    """);
            Integer faltantes = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM pago_restante p
                    WHERE NOT EXISTS (
                        SELECT 1 FROM transaccion_pago t
                        WHERE t.id_reserva = p.id_reserva AND t.reserva_pago_restante = p.id_reserva
                          AND t.tipo_pago = 'RESTANTE' AND t.id_trabajador = p.id_trabajador
                          AND t.clave_idempotencia_restante = p.clave_idempotencia
                          AND t.referencia_check_in = CONCAT('RESTANTE-', p.id)
                          AND (t.numero_operacion_fisica = p.numero_operacion
                               OR (t.numero_operacion_fisica IS NULL AND p.numero_operacion IS NULL))
                          AND t.monto = p.monto AND t.metodo_pago = p.metodo_pago
                          AND t.fecha_transaccion = p.fecha_registro
                          AND t.id_externo_pasarela IS NULL
                          AND (t.estado_transaccion = 'APROBADA'
                               OR (NOT p.completado AND t.estado_transaccion = 'PENDIENTE'))
                    )
                    """, Integer.class);
            if (faltantes != 0) {
                throw new IllegalStateException("Hay pagos restantes que no coinciden con la copia. Se conserva la tabla anterior.");
            }
        });

        // En MySQL DROP TABLE confirma implícitamente. La copia ya fue verificada y confirmada.
        if (retirarTabla) jdbc.execute("DROP TABLE pago_restante");
    }

    private boolean existeTablaAnterior() {
        return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) conexion -> {
            try (ResultSet tablas = conexion.getMetaData()
                    .getTables(conexion.getCatalog(), conexion.getSchema(), "%", null)) {
                while (tablas.next()) {
                    if ("pago_restante".equalsIgnoreCase(tablas.getString("TABLE_NAME"))) return true;
                }
                return false;
            }
        }));
    }
}

