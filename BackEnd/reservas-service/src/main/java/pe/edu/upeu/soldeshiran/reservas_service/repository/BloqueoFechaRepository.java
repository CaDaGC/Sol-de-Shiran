package pe.edu.upeu.soldeshiran.reservas_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.soldeshiran.reservas_service.model.BloqueoFecha;

public interface BloqueoFechaRepository extends JpaRepository<BloqueoFecha, Long> {
  List<BloqueoFecha> findByIdCabanaAndActivoTrue(Long idCabana);
}
