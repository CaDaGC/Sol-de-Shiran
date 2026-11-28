package pe.edu.upeu.soldeshiran.restaurante_service.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoPedido;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.PedidoRepository;

@RestController
@RequestMapping("/internal/restaurante")
@RequiredArgsConstructor
public class RestauranteInternoController {
    private final PedidoRepository pedidos;

    @GetMapping("/reservas/{idReserva}/pendientes")
    public boolean pendientes(@PathVariable("idReserva") Long idReserva) {
        if (idReserva <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserva inválida");
        return pedidos.existsByIdReservaAndEstadoPedidoIn(idReserva, List.of(
                EstadoPedido.NUEVO, EstadoPedido.EN_PREPARACION, EstadoPedido.EN_ENTREGA));
    }
}
