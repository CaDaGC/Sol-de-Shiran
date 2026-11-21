package pe.edu.upeu.soldeshiran.pagos_service.repository;
import pe.edu.upeu.soldeshiran.pagos_service.model.ComprobantePago;
public interface ComprobantePagoRepository extends org.springframework.data.jpa.repository.JpaRepository<ComprobantePago,Long> {
  java.util.Optional<ComprobantePago> findByIdReserva(Long idReserva);
}
