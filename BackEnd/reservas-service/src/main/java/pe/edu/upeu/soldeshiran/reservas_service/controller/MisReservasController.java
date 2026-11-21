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
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(defaultValue = "TODAS") GrupoReservasCliente grupo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String cabana,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta) {
        var actor = pe.edu.upeu.soldeshiran.reservas_service.security.Actor.desde(request);
        actor.exigir("CLIENTE");
        return service.consultar(actor.id(), grupo, page, size, codigo, cabana, desde, hasta);
    }
}
