package pe.edu.upeu.soldeshiran.pagos_service;

import org.junit.jupiter.api.Test;
import pe.edu.upeu.soldeshiran.pagos_service.model.EstadoTransaccion;
import pe.edu.upeu.soldeshiran.pagos_service.model.TransaccionPago;
import pe.edu.upeu.soldeshiran.pagos_service.repository.TransaccionPagoRepository;
import pe.edu.upeu.soldeshiran.pagos_service.service.PagoReconciliationScheduler;
import pe.edu.upeu.soldeshiran.pagos_service.service.PagoService;

import java.util.List;

import static org.mockito.Mockito.*;

class PagoReconciliationSchedulerTests {
    @Test
    void unErrorDeUnaOrdenNoDetieneLaConciliacionDeLasDemas() {
        var repository = mock(TransaccionPagoRepository.class);
        var pagos = mock(PagoService.class);
        var primera = new TransaccionPago();
        primera.setIdTransaccion(1L);
        primera.setIdExternoPasarela("ORDTST1");
        var segunda = new TransaccionPago();
        segunda.setIdTransaccion(2L);
        segunda.setIdExternoPasarela("ORDTST2");
        when(repository.findTop50ByEstadoTransaccionAndMetodoPagoAndIdExternoPasarelaStartingWithOrderByIdTransaccionDesc(
                EstadoTransaccion.PENDIENTE, "MERCADO_PAGO_ORDERS", "ORD"))
                .thenReturn(List.of(primera, segunda));
        doThrow(new IllegalStateException("error de prueba")).when(pagos).procesarNotificacion("ORDTST1");

        new PagoReconciliationScheduler(repository, pagos).reconciliarPendientes();

        verify(pagos).procesarNotificacion("ORDTST1");
        verify(pagos).procesarNotificacion("ORDTST2");
    }
}
