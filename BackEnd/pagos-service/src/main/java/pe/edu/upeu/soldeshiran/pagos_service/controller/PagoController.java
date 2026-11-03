package pe.edu.upeu.soldeshiran.pagos_service.controller;

import jakarta.validation.Valid;
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
            @RequestHeader("X-User-Id") @Positive Long idUsuario,
            @PathVariable @Positive Long idReserva,
            @Valid @RequestBody CrearPagoBrickRequest request
    ) {
        return pagoService.procesarPagoBrick(idUsuario, idReserva, request);
    }

    @PostMapping("/reservas/{idReserva}/preferencia")
    @ResponseStatus(HttpStatus.CREATED)
    public PreferenciaResponse crearPreferencia(
            @RequestHeader("X-User-Id") @Positive Long idUsuario,
            @PathVariable @Positive Long idReserva,
            @Valid @RequestBody CrearPreferenciaRequest request
    ) {
        return pagoService.crearPreferencia(idUsuario, idReserva, request);
    }
}