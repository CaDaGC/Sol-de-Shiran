package pe.edu.upeu.soldeshiran.pagos_service.client;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name="MS-RESERVAS", contextId="reservasReportesClient")
public interface ReservasReportesClient {
  @GetMapping("/internal/reservas/reportes/duenos/{id}/reservas")
  List<Long> propias(@PathVariable("id") Long id,@RequestHeader("X-Internal-Key") String key);
}
