package pe.edu.upeu.soldeshiran.pagos_service.repository;

import pe.edu.upeu.soldeshiran.pagos_service.model.TransaccionPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransaccionPagoRepository
        extends JpaRepository<TransaccionPago, Long> {

    Optional<TransaccionPago> findByIdExternoPasarela(String idExternoPasarela);
}