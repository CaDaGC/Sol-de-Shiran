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

  @Override
  @Transactional
  public void run(String... args) {
    jdbc.update("UPDATE reserva SET version=0 WHERE version IS NULL");
    jdbc.update("UPDATE reserva SET total_pagado=0 WHERE total_pagado IS NULL");
    jdbc.update(
        "UPDATE reserva SET total_pagado=monto_total-saldo_pendiente WHERE estado_reserva IN"
            + " ('CONFIRMADA','OCUPADA','FINALIZADA') AND total_pagado <"
            + " monto_total-saldo_pendiente");
  }
}
