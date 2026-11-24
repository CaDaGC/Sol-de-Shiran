package pe.edu.upeu.soldeshiran.resenas_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.soldeshiran.resenas_service.service.ResenaService;
import pe.edu.upeu.soldeshiran.resenas_service.dto.ElegibilidadResenaResponse;
import pe.edu.upeu.soldeshiran.resenas_service.dto.ResenaRequest;
import pe.edu.upeu.soldeshiran.resenas_service.dto.ResenasPaginaResponse;
import pe.edu.upeu.soldeshiran.resenas_service.security.IdentityFilter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/resenas/cabanas/{idCabana}")
public class ResenaController {
    private final ResenaService resenas;

    @GetMapping
    public ResenasPaginaResponse listar(@PathVariable("idCabana") @Positive Long idCabana,
            @RequestParam(defaultValue = "0") @Min(0) @Max(100000000) int pagina) {
        return resenas.listar(idCabana, pagina);
    }

    @GetMapping("/elegibilidad")
    public ElegibilidadResenaResponse elegibilidad(@PathVariable("idCabana") @Positive Long idCabana,
            @RequestAttribute(IdentityFilter.CLIENTE_ID) Long usuario) {
        return resenas.elegibilidad(idCabana, usuario);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void publicar(@PathVariable("idCabana") @Positive Long idCabana,
            @RequestAttribute(IdentityFilter.CLIENTE_ID) Long usuario, @Valid @RequestBody ResenaRequest solicitud) {
        resenas.publicar(idCabana, usuario, solicitud);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<org.springframework.http.ProblemDetail> error(
            org.springframework.web.server.ResponseStatusException e) {
        return org.springframework.http.ResponseEntity.status(e.getStatusCode())
                .body(org.springframework.http.ProblemDetail.forStatusAndDetail(e.getStatusCode(), e.getReason()));
    }
}
