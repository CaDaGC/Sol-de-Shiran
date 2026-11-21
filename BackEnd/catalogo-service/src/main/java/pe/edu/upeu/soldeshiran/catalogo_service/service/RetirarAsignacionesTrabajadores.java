package pe.edu.upeu.soldeshiran.catalogo_service.service;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class RetirarAsignacionesTrabajadores implements ApplicationRunner {
  private final JdbcTemplate jdbc;
  @Override public void run(ApplicationArguments args) {
    jdbc.execute("DROP TABLE IF EXISTS cabana_trabajadores");
  }
}
