package pe.edu.upeu.soldeshiran.reservas_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.soldeshiran.reservas_service.dto.GrupoReservasCliente;
import pe.edu.upeu.soldeshiran.reservas_service.dto.MisReservasPaginaResponse;
import pe.edu.upeu.soldeshiran.reservas_service.service.MisReservasService;

@RestController
@RequestMapping("/api/v1/reservas/mis-reservas")
@RequiredArgsConstructor
public class MisReservasController {
    private final MisReservasService service;

    @GetMapping
    public MisReservasPaginaResponse consultar(
            @RequestHeader("X-User-Id") Long idUsuario,
            @RequestParam(defaultValue = "TODAS") GrupoReservasCliente grupo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.consultar(idUsuario, grupo, page, size);
    }
}
