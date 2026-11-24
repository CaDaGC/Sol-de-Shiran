package pe.edu.upeu.soldeshiran.pagos_service.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.pagos_service.service.PagoDanioService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/pagos/danos")
public class PagoDanioInternoController {
    private final PagoDanioService pagos;

    @Value("${servicios.interno.token}")
    private String token;

    @PutMapping("/{idReserva}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registrar(@PathVariable @Positive Long idReserva,
            @RequestHeader("X-Internal-Service-Token") String recibido,
            @Valid @RequestBody PagoDanioService.Cobro cobro) {
        if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                recibido.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credencial interna inválida.");
        }
        pagos.registrar(idReserva, cobro);
    }
}
