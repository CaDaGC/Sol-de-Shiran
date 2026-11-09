package pe.edu.upeu.soldeshiran.reservas_service.repository;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.soldeshiran.reservas_service.model.PagoPresencial;

public interface PagoPresencialRepository extends JpaRepository<PagoPresencial, Long> {
  Optional<PagoPresencial> findByClaveOperacion(String clave);

  List<PagoPresencial> findByReservaId(Long id);
}
