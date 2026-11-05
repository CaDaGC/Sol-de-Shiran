package pe.edu.upeu.soldeshiran.pagos_service.repository;
import pe.edu.upeu.soldeshiran.pagos_service.model.PagoRestante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface PagoRestanteRepository extends JpaRepository<PagoRestante,Long> {
 Optional<PagoRestante> findByIdReserva(Long idReserva);
}
