package pe.edu.upeu.soldeshiran.pagos_service.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.pagos_service.model.EstadoTransaccion;
import pe.edu.upeu.soldeshiran.pagos_service.repository.TransaccionPagoRepository;

@Component
@RequiredArgsConstructor
public class PagoReconciliationScheduler {
    private static final Logger log = LoggerFactory.getLogger(PagoReconciliationScheduler.class);
    private final TransaccionPagoRepository transacciones;
    private final PagoService pagos;

    // Recupera pagos acreditados cuando el comprador no vuelve al sitio o el webhook no llega.
    @Scheduled(initialDelayString = "${pagos.reconciliacion-inicio-ms:30000}",
            fixedDelayString = "${pagos.reconciliacion-cada-ms:120000}")
    public void reconciliarPendientes() {
        var pendientes = transacciones
                .findTop50ByEstadoTransaccionAndMetodoPagoAndIdExternoPasarelaStartingWithOrderByIdTransaccionDesc(
                        EstadoTransaccion.PENDIENTE, "MERCADO_PAGO_ORDERS", "ORD");
        for (var transaccion : pendientes) {
            try {
                pagos.procesarNotificacion(transaccion.getIdExternoPasarela());
            } catch (Exception e) {
                log.warn("No se pudo conciliar la transacción {} con Mercado Pago: {}",
                        transaccion.getIdTransaccion(), e.toString());
            }
        }
    }
}
