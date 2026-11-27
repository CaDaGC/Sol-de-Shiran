package pe.edu.upeu.soldeshiran.restaurante_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.CategoriaMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.ProductoMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.model.TipoCategoria;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.CartaMenuService;

@RestController
@RequestMapping("/api/restaurante/locales/{idRestaurante}")
@RequiredArgsConstructor
public class CartaMenuController {

    private final CartaMenuService servicio;

    @GetMapping("/categorias")
    public List<CategoriaMenuResponse> categorias(
            @PathVariable Long idRestaurante,
            @RequestParam(required = false) Long idReserva,
            HttpServletRequest request) {

        return servicio.consultarCategorias(
                idRestaurante,
                idReserva,
                Actor.desde(request)
        );
    }

    @GetMapping("/productos")
    public List<ProductoMenuResponse> productos(
            @PathVariable Long idRestaurante,
            @RequestParam(required = false) Long idReserva,
            @RequestParam(required = false) Long idCategoria,
            @RequestParam(required = false) TipoCategoria tipo,
            HttpServletRequest request) {

        return servicio.consultarProductos(
                idRestaurante,
                idReserva,
                idCategoria,
                tipo,
                Actor.desde(request)
        );
    }
}