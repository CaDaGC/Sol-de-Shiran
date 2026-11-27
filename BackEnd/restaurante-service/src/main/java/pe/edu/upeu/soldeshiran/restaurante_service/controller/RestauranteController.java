package pe.edu.upeu.soldeshiran.restaurante_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.EstadiaActivaResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.InicioRestaurantesResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.RestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.SeleccionRestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.RestauranteConsultaService;

@RestController
@RequestMapping("/api/restaurante")
@RequiredArgsConstructor
public class RestauranteController {

    private final RestauranteConsultaService servicio;

    @GetMapping("/estadia-activa/{idUsuario}")
    public EstadiaActivaResponse estadia(
            @PathVariable Long idUsuario,
            HttpServletRequest request) {

        return servicio.consultarEstadias(
                idUsuario,
                Actor.desde(request)
        );
    }

    @GetMapping("/locales")
    public List<RestauranteResponse> locales(
            HttpServletRequest request) {

        return servicio.listarLocales(
                Actor.desde(request)
        );
    }

    @GetMapping("/mi-local/{idUsuario}")
    public RestauranteResponse miLocal(
            @PathVariable Long idUsuario,
            HttpServletRequest request) {

        return servicio.consultarMiLocal(
                idUsuario,
                Actor.desde(request)
        );
    }

    @GetMapping("/inicio")
    public InicioRestaurantesResponse inicio(
            @RequestParam(required = false) Long idReserva,
            HttpServletRequest request) {

        return servicio.consultarInicio(
                idReserva,
                Actor.desde(request)
        );
    }

    @GetMapping("/locales/{idRestaurante}")
    public SeleccionRestauranteResponse seleccionar(
            @PathVariable Long idRestaurante,
            @RequestParam(required = false) Long idReserva,
            HttpServletRequest request) {

        return servicio.seleccionarRestaurante(
                idRestaurante,
                idReserva,
                Actor.desde(request)
        );
    }
}