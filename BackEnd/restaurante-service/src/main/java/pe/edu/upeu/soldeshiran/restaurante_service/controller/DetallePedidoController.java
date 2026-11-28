package pe.edu.upeu.soldeshiran.restaurante_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.PedidoResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.DetallePedidoService;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class DetallePedidoController {

    private final DetallePedidoService servicio;

    @GetMapping("/{idPedido}")
    public ResponseEntity<PedidoResponse> consultar(
            @PathVariable("idPedido") Long idPedido,
            HttpServletRequest request) {

        var pedido = servicio.consultar(
                idPedido,
                Actor.desde(request)
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(pedido);
    }
}