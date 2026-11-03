package pe.edu.upeu.soldeshiran.reservas_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ConfirmarPagoInternoRequest;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservaPagoInternoResponse;
import pe.edu.upeu.soldeshiran.reservas_service.service.ReservaPagoInternoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/reservas/interno")
@RequiredArgsConstructor
public class ReservaPagoInternoController {
    private final ReservaPagoInternoService service;

    @Value("${servicios.interno.token}")
    private String internalToken;

    @GetMapping("/{idReserva}")
    public ReservaPagoInternoResponse consultar(
            @PathVariable Long idReserva,
            @RequestHeader("X-Internal-Service-Token") String token
    ) {
        validarToken(token);
        return service.consultar(idReserva);
    }

    @PostMapping("/{idReserva}/confirmar-pago")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmar(
            @PathVariable Long idReserva,
            @RequestHeader("X-Internal-Service-Token") String token,
            @Valid @RequestBody ConfirmarPagoInternoRequest request
    ) {
        validarToken(token);
        service.confirmar(idReserva, request);
    }

    private void validarToken(String recibido) {
        boolean valido = recibido != null
                && MessageDigest.isEqual(
                internalToken.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8)
        );

        if (!valido) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Credencial interna inválida."
            );
        }
    }
}