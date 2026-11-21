package pe.edu.upeu.soldeshiran.reservas_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CodigoReservaService {
  private final JdbcTemplate jdbc;

  @Transactional
  public String siguiente() {

    Long ultimo = jdbc.queryForObject(
        "SELECT ultimo_codigo FROM cerrojo_reservas WHERE id=1 FOR UPDATE", Long.class);
    long siguiente = Math.addExact(ultimo == null ? 0L : ultimo, 1L);
    jdbc.update("UPDATE cerrojo_reservas SET ultimo_codigo=? WHERE id=1", siguiente);
    return String.format(java.util.Locale.ROOT, "RSS%05d", siguiente);
  }
}
