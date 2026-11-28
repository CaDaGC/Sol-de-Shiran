package pe.edu.upeu.soldeshiran.restaurante_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.HistorialPedidosResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.HistorialPedidosService;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class HistorialPedidosController {

    private final HistorialPedidosService servicio;

    @GetMapping("/usuario/{idUsuario}")
    public HistorialPedidosResponse consultar(
            @PathVariable("idUsuario") Long idUsuario,
            @RequestParam(name = "idReserva", required = false)
            Long idReserva,
            @RequestParam(name = "pagina", defaultValue = "0")
            int pagina,
            @RequestParam(name = "tamanio", defaultValue = "20")
            int tamanio,
            HttpServletRequest request) {

        return servicio.consultar(
                idUsuario,
                idReserva,
                pagina,
                tamanio,
                Actor.desde(request)
        );
    }
}