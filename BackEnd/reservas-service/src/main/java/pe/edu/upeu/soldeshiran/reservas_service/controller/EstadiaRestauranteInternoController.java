package pe.edu.upeu.soldeshiran.reservas_service.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.soldeshiran.reservas_service.dto.EstadiaRestauranteResponse;
import pe.edu.upeu.soldeshiran.reservas_service.service.EstadiaRestauranteService;

@RestController
@RequestMapping("/internal/reservas/restaurante")
@RequiredArgsConstructor
public class EstadiaRestauranteInternoController {

    private final EstadiaRestauranteService servicio;

    @GetMapping("/usuarios/{idUsuario}/estadias")
    public List<EstadiaRestauranteResponse> consultar(
            @PathVariable Long idUsuario) {
        return servicio.consultar(idUsuario);
    }
}