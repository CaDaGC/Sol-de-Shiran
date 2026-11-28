package pe.edu.upeu.soldeshiran.reservas_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "RESTAURANTE-SERVICE")
public interface RestauranteClient {
    @GetMapping("/internal/restaurante/reservas/{idReserva}/pendientes")
    Boolean tienePendientes(@PathVariable("idReserva") Long idReserva,
            @RequestHeader("X-Internal-Key") String clave);
}
