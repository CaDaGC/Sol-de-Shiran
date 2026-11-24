package pe.edu.upeu.soldeshiran.pagos_service.repository;

import pe.edu.upeu.soldeshiran.pagos_service.model.TransaccionPago;
import pe.edu.upeu.soldeshiran.pagos_service.model.EstadoTransaccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransaccionPagoRepository
        extends JpaRepository<TransaccionPago, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from TransaccionPago t where t.idTransaccion=:id")
    Optional<TransaccionPago> bloquearComprobante(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Query("select t.idTransaccion from TransaccionPago t where t.tipoPago='ONLINE_ADELANTO' and t.estadoTransaccion=pe.edu.upeu.soldeshiran.pagos_service.model.EstadoTransaccion.APROBADA and not exists (select c.id from ComprobantePago c where c.idTransaccion=t.idTransaccion) order by t.idTransaccion")
    List<Long> pendientesComprobante(org.springframework.data.domain.Pageable pagina);


    Optional<TransaccionPago> findFirstByIdReservaAndTipoPagoAndEstadoTransaccionOrderByIdTransaccionDesc(
            Long idReserva, String tipoPago, pe.edu.upeu.soldeshiran.pagos_service.model.EstadoTransaccion estado);

    Optional<TransaccionPago> findByReservaPagoRestante(Long idReserva);

    Optional<TransaccionPago> findByReservaPagoDanos(Long idReserva);

    Optional<TransaccionPago> findByIdExternoPasarela(String idExternoPasarela);

    List<TransaccionPago> findTop50ByEstadoTransaccionAndMetodoPagoAndIdExternoPasarelaStartingWithOrderByIdTransaccionDesc(
            EstadoTransaccion estadoTransaccion, String metodoPago, String prefijoIdExterno);
}
