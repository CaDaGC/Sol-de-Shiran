package pe.edu.upeu.soldeshiran.reservas_service.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "MS-PAGOS", contextId = "pagoDanioClient")
public interface PagoDanioClient {
    @PutMapping("/internal/pagos/danos/{idReserva}")
    void registrar(@PathVariable("idReserva") Long idReserva,
            @RequestHeader("X-Internal-Service-Token") String token, @RequestBody Cobro cobro);

    record Cobro(Long idTrabajador, String descripcion, BigDecimal monto, String metodoPago, LocalDateTime fecha) {}
}
