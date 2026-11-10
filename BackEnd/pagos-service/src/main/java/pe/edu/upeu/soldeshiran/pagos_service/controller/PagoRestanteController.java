package pe.edu.upeu.soldeshiran.pagos_service.controller;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.pagos_service.client.ReservaInternaClient;
import pe.edu.upeu.soldeshiran.pagos_service.dto.RegistrarPagoRestanteRequest;
import pe.edu.upeu.soldeshiran.pagos_service.dto.PagoRestanteResponse;
import pe.edu.upeu.soldeshiran.pagos_service.security.Actor;
import pe.edu.upeu.soldeshiran.pagos_service.service.PagoRestanteService;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/pagos/reservas/{idReserva}/restante")
public class PagoRestanteController {
    private final PagoRestanteService service;
    private final ReservaInternaClient reservas;

    @PostMapping
    public PagoRestanteResponse registrar(HttpServletRequest solicitud, @PathVariable @Positive Long idReserva,
            @Valid @RequestBody RegistrarPagoRestanteRequest request) {
        Actor actor = Actor.desde(solicitud);
        autorizar(actor, idReserva, solicitud.getHeader("Authorization"));
        return PagoRestanteResponse.desde(service.registrar(actor.id(), idReserva, request));
    }

    @GetMapping
    public PagoRestanteResponse consultar(HttpServletRequest solicitud, @PathVariable @Positive Long idReserva) {
        autorizar(Actor.desde(solicitud), idReserva, solicitud.getHeader("Authorization"));
        var pago = service.consultar(idReserva);
        return pago == null ? null : PagoRestanteResponse.desde(pago);
    }

    private void autorizar(Actor actor, Long idReserva, String authorization) {
        actor.exigir("ADMIN", "TRABAJADOR");
        try {
            reservas.exigirAccesoOperativo(idReserva, authorization);
        } catch (FeignException exception) {
            if (exception.status() == 403 || exception.status() == 404) {
                throw new ResponseStatusException(HttpStatus.valueOf(exception.status()),
                        "No tienes acceso a esta reserva.");
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo comprobar el acceso a la reserva.", exception);
        }
    }
}
