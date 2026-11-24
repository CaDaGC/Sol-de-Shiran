package pe.edu.upeu.soldeshiran.resenas_service.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "MS-RESERVAS")
public interface ReservasClient {
    @GetMapping("/internal/reservas/resenas/estancias")
    List<Long> estancias(@RequestParam("idUsuario") Long usuario, @RequestParam("idCabana") Long cabana,
            @RequestHeader("X-Internal-Key") String clave);
}
