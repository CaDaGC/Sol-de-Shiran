package pe.edu.upeu.soldeshiran.reservas_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(
        name = "CATALOGO-SERVICE",
        contextId = "catalogoReservasClient"
)
public interface CatalogoClient {
    @org.springframework.web.bind.annotation.PostMapping("/api/v1/cabanas/interno/{idCabana}/ocupacion/{idReserva}")
    void ocupar(@PathVariable Long idCabana, @PathVariable Long idReserva,
        @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Service-Token") String token);


    @org.springframework.web.bind.annotation.DeleteMapping("/api/v1/cabanas/interno/{idCabana}/ocupacion/{idReserva}")
    void liberar(@PathVariable Long idCabana, @PathVariable Long idReserva,
        @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Service-Token") String token);

    @GetMapping("/internal/cabanas/resumen")
    List<CabanaResumenCliente> consultarResumen(@RequestParam("ids") List<Long> ids,
        @org.springframework.web.bind.annotation.RequestHeader("X-Internal-Key") String key);

    record CabanaResumenCliente(Long idCabana, String nombre, String imagenPrincipal) { }

    @GetMapping("/api/v1/cabanas/{idCabana}")
    CabanaDetalleClientResponse obtenerDetalle(
            @PathVariable("idCabana") Long idCabana
    );
}
