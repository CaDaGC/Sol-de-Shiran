package pe.edu.upeu.soldeshiran.restaurante_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.PanelPedidosResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoPedido;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.PanelPedidosService;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PanelPedidosController {

    private final PanelPedidosService servicio;

    @GetMapping("/restaurante/{idRestaurante}")
    public ResponseEntity<PanelPedidosResponse> consultar(
            @PathVariable("idRestaurante") Long idRestaurante,
            @RequestParam(name = "estado", required = false)
            EstadoPedido estado,
            @RequestParam(name = "cabana", required = false)
            String cabana,
            @RequestParam(name = "pagina", defaultValue = "0")
            int pagina,
            @RequestParam(name = "tamanio", defaultValue = "20")
            int tamanio,
            HttpServletRequest request) {

        var respuesta = servicio.consultar(
                idRestaurante,
                estado,
                cabana,
                pagina,
                tamanio,
                Actor.desde(request)
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(respuesta);
    }
}