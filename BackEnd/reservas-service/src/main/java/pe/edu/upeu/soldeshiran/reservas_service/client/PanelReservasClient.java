package pe.edu.upeu.soldeshiran.reservas_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "api-gateway", contextId = "panelReservasClient")
public interface PanelReservasClient {
    @GetMapping("/api/v1/usuarios/resumen")
    List<UsuarioResumen> consultarUsuarios(
            @RequestHeader("Authorization") String authorization,
            @RequestParam(value = "buscar", required = false) String buscar,
            @RequestParam(value = "ids", required = false) List<Long> ids);

    @GetMapping("/api/v1/cabanas/resumen")
    List<CabanaResumen> consultarCabanas(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("ids") List<Long> ids);

    record UsuarioResumen(Long id, String nombre, String email) { }
    record CabanaResumen(Long idCabana, String nombre) { }
}
