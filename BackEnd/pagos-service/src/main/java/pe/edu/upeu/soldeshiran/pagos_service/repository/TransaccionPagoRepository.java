package pe.edu.upeu.soldeshiran.pagos_service.repository;

import pe.edu.upeu.soldeshiran.pagos_service.model.TransaccionPago;
import pe.edu.upeu.soldeshiran.pagos_service.model.EstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransaccionPagoRepository
        extends JpaRepository<TransaccionPago, Long> {

    Optional<TransaccionPago> findFirstByIdReservaAndTipoPagoAndEstadoTransaccionOrderByIdTransaccionDesc(
            Long idReserva, String tipoPago, pe.edu.upeu.soldeshiran.pagos_service.model.EstadoTransaccion estado);

    Optional<TransaccionPago> findByReservaPagoRestante(Long idReserva);

    Optional<TransaccionPago> findByReservaPagoDanos(Long idReserva);

    Optional<TransaccionPago> findByIdExternoPasarela(String idExternoPasarela);

    List<TransaccionPago> findTop50ByEstadoTransaccionAndMetodoPagoAndIdExternoPasarelaStartingWithOrderByIdTransaccionDesc(
            EstadoTransaccion estadoTransaccion, String metodoPago, String prefijoIdExterno);
}
