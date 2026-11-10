package pe.edu.upeu.soldeshiran.pagos_service.controller;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import pe.edu.upeu.soldeshiran.pagos_service.security.Actor;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.soldeshiran.pagos_service.dto.*;
import pe.edu.upeu.soldeshiran.pagos_service.service.PagoService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
public class PagoController {
    private final PagoService pagoService;

    @PostMapping("/reservas/{idReserva}/brick")
    public PagoResponse pagarConBrick(
            HttpServletRequest solicitud,
            @PathVariable @Positive Long idReserva,
            @Valid @RequestBody CrearPagoBrickRequest request
    ) {
        return pagoService.procesarPagoBrick(cliente(solicitud).id(), idReserva, request);
    }

    @PostMapping({"/reservas/{idReserva}/orden", "/reservas/{idReserva}/preferencia"})
    @ResponseStatus(HttpStatus.CREATED)
    public PreferenciaResponse crearPreferencia(
            HttpServletRequest solicitud,
            @PathVariable @Positive Long idReserva,
            @Valid @RequestBody CrearPreferenciaRequest request
    ) {
        return pagoService.crearPreferencia(cliente(solicitud).id(), idReserva, request);
    }
    @GetMapping("/transacciones/{idTransaccion}")
    public PagoResponse consultarEstado(HttpServletRequest solicitud,
            @PathVariable Long idTransaccion) {
        return pagoService.consultarEstado(cliente(solicitud).id(), idTransaccion);
    }

    private Actor cliente(HttpServletRequest solicitud) {
        Actor actor = Actor.desde(solicitud);
        actor.exigir("ADMIN", "CLIENTE");
        return actor;
    }
}
