package pe.edu.upeu.soldeshiran.reservas_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReservaSchemaInitializer implements CommandLineRunner {
  private final JdbcTemplate jdbc;
  private final CodigoReservaService codigos;

  @Override
  @Transactional
  public void run(String... args) {
    jdbc.update("INSERT INTO cerrojo_reservas (id) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM cerrojo_reservas WHERE id=1)");
    jdbc.queryForObject("SELECT id FROM cerrojo_reservas WHERE id=1 FOR UPDATE", Long.class);
    jdbc.update("UPDATE cerrojo_reservas SET ultimo_codigo=0 WHERE ultimo_codigo IS NULL");

    for (Long id : jdbc.queryForList(
        "SELECT id_reserva FROM reserva WHERE codigo IS NULL ORDER BY id_reserva", Long.class)) {
      jdbc.update("UPDATE reserva SET codigo=? WHERE id_reserva=?", codigos.siguiente(), id);
    }
    jdbc.update("UPDATE reserva SET hora_ingreso='00:00:00' WHERE hora_ingreso IS NULL");
    jdbc.update("UPDATE reserva SET hora_salida='00:00:00' WHERE hora_salida IS NULL");
    jdbc.update("UPDATE reserva SET version=0 WHERE version IS NULL");
    jdbc.update("UPDATE reserva SET total_pagado=0 WHERE total_pagado IS NULL");
    jdbc.update(
        "UPDATE reserva SET total_pagado=monto_total-saldo_pendiente WHERE estado_reserva IN"
            + " ('CONFIRMADA','OCUPADA','FINALIZADA') AND total_pagado <"
            + " monto_total-saldo_pendiente");
  }
}
