package pe.edu.upeu.soldeshiran.reservas_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "CATALOGO-SERVICE",
        path = "/api/v1/cabanas"
)
public interface CatalogoClient {

    @GetMapping("/{idCabana}")
    CabanaDetalleClientResponse obtenerDetalle(
            @PathVariable("idCabana") Long idCabana
    );
}