package pe.edu.upeu.soldeshiran.reservas_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(
        name = "CATALOGO-SERVICE",
        path = "/api/v1/cabanas"
)
public interface CatalogoClient {

    @GetMapping("/resumen")
    List<CabanaResumenCliente> consultarResumen(@RequestParam("ids") List<Long> ids);

    record CabanaResumenCliente(Long idCabana, String nombre, String imagenPrincipal) { }

    @GetMapping("/{idCabana}")
    CabanaDetalleClientResponse obtenerDetalle(
            @PathVariable("idCabana") Long idCabana
    );
}
