package pe.edu.upeu.soldeshiran.restaurante_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.EstadoPedidoRequest;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.PedidoResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.AtencionPedidoService;

@RestController
@RequestMapping("/api/pedidos/restaurante/{idRestaurante}")
@RequiredArgsConstructor
public class AtencionPedidoController {

    private final AtencionPedidoService servicio;

    @GetMapping("/{idPedido}")
    public ResponseEntity<PedidoResponse> consultar(
            @PathVariable("idRestaurante") Long idRestaurante,
            @PathVariable("idPedido") Long idPedido,
            HttpServletRequest request) {

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(servicio.consultar(
                        idRestaurante,
                        idPedido,
                        Actor.desde(request)
                ));
    }

    @PatchMapping("/{idPedido}/estado")
    public PedidoResponse cambiarEstado(
            @PathVariable("idRestaurante") Long idRestaurante,
            @PathVariable("idPedido") Long idPedido,
            @Valid @RequestBody EstadoPedidoRequest datos,
            HttpServletRequest request) {

        return servicio.cambiarEstado(
                idRestaurante,
                idPedido,
                datos,
                Actor.desde(request)
        );
    }

    @GetMapping("/{idPedido}/comanda")
    public ResponseEntity<byte[]> comanda(
            @PathVariable("idRestaurante") Long idRestaurante,
            @PathVariable("idPedido") Long idPedido,
            @RequestParam(
                    name = "descargar",
                    defaultValue = "false"
            ) boolean descargar,
            HttpServletRequest request) {

        byte[] contenido = servicio.descargarComanda(
                idRestaurante,
                idPedido,
                Actor.desde(request)
        );

        var disposicion = descargar
                ? ContentDisposition.attachment()
                : ContentDisposition.inline();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposicion.filename(
                                "comanda-pedido-" + idPedido + ".pdf"
                        ).build().toString()
                )
                .body(contenido);
    }
}