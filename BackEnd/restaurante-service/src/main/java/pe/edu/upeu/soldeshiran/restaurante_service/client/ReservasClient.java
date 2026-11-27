package pe.edu.upeu.soldeshiran.restaurante_service.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.EstadiaResponse;

@FeignClient(name = "MS-RESERVAS")
public interface ReservasClient {

    @GetMapping("/internal/reservas/restaurante/usuarios/{idUsuario}/estadias")
    List<EstadiaResponse> estadias(
            @PathVariable("idUsuario") Long idUsuario,
            @RequestHeader("X-Internal-Key") String clave
    );
}