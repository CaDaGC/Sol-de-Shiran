package pe.edu.upeu.soldeshiran.reservas_service.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.reservas_service.client.PagoDanioClient;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class PagoDanioScheduler {
    private final ReservaRepository reservas;
    private final PagoDanioClient pagos;

    @Value("${servicios.interno.token}")
    private String token;

    @Scheduled(fixedDelayString = "${reservas.exportar-danos-cada-ms:10000}",
            initialDelayString = "${reservas.exportar-danos-cada-ms:10000}")
    public void exportar() {
        // La consulta solo ve check-outs confirmados. Los históricos también se exportan.
        for (var cobro : reservas.listarCobrosDaniosPendientes()) {
            try {
                pagos.registrar(cobro.getIdReserva(), token, new PagoDanioClient.Cobro(
                        cobro.getRegistradoPor(), cobro.getDescripcion(), cobro.getMonto(),
                        cobro.getMedioPago(), cobro.getFecha()));
                reservas.marcarCobroDanioExportado(cobro.getIdReserva());
            } catch (FeignException e) {
                log.warn("Pago por daños de reserva {} pendiente de exportar (HTTP {})",
                        cobro.getIdReserva(), e.status());
            }
        }
    }
}
