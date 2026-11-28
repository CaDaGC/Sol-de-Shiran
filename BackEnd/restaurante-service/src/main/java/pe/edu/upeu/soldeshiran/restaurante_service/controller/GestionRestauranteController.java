package pe.edu.upeu.soldeshiran.restaurante_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.AsignacionRestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.CategoriaMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.GestionRestauranteRequest.*;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.ProductoMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.RestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.GestionRestauranteService;

@RestController
@RequestMapping("/api/restaurante")
@RequiredArgsConstructor
public class GestionRestauranteController {

    private final GestionRestauranteService servicio;

    @GetMapping("/admin/locales")
    public List<RestauranteResponse> listarRestaurantes(
            HttpServletRequest request) {

        return servicio.listarRestaurantes(Actor.desde(request));
    }

    @PostMapping("/admin/locales")
    @ResponseStatus(HttpStatus.CREATED)
    public RestauranteResponse crearRestaurante(
            @Valid @RequestBody RestauranteDatos datos,
            HttpServletRequest request) {

        return servicio.crearRestaurante(
                datos,
                Actor.desde(request)
        );
    }

    @PutMapping("/admin/locales/{idRestaurante}")
    public RestauranteResponse editarRestaurante(
            @PathVariable("idRestaurante") Long idRestaurante,
            @Valid @RequestBody RestauranteDatos datos,
            HttpServletRequest request) {

        return servicio.editarRestaurante(
                idRestaurante,
                datos,
                Actor.desde(request)
        );
    }

    @PatchMapping("/admin/locales/{idRestaurante}/estado")
    public RestauranteResponse cambiarEstado(
            @PathVariable("idRestaurante") Long idRestaurante,
            @Valid @RequestBody EstadoDatos datos,
            HttpServletRequest request) {

        return servicio.cambiarEstado(
                idRestaurante,
                datos,
                Actor.desde(request)
        );
    }

    @GetMapping("/admin/locales/{idRestaurante}/categorias")
    public List<CategoriaMenuResponse> listarCategorias(
            @PathVariable("idRestaurante") Long idRestaurante,
            HttpServletRequest request) {

        return servicio.listarCategorias(
                idRestaurante,
                Actor.desde(request)
        );
    }

    @PostMapping("/admin/locales/{idRestaurante}/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaMenuResponse crearCategoria(
            @PathVariable("idRestaurante") Long idRestaurante,
            @Valid @RequestBody CategoriaDatos datos,
            HttpServletRequest request) {

        return servicio.guardarCategoria(
                idRestaurante,
                null,
                datos,
                Actor.desde(request)
        );
    }

    @PutMapping("/admin/locales/{idRestaurante}/categorias/{idCategoria}")
    public CategoriaMenuResponse editarCategoria(
            @PathVariable("idRestaurante") Long idRestaurante,
            @PathVariable("idCategoria") Long idCategoria,
            @Valid @RequestBody CategoriaDatos datos,
            HttpServletRequest request) {

        return servicio.guardarCategoria(
                idRestaurante,
                idCategoria,
                datos,
                Actor.desde(request)
        );
    }

    @DeleteMapping("/admin/locales/{idRestaurante}/categorias/{idCategoria}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarCategoria(
            @PathVariable("idRestaurante") Long idRestaurante,
            @PathVariable("idCategoria") Long idCategoria,
            HttpServletRequest request) {

        servicio.eliminarCategoria(
                idRestaurante,
                idCategoria,
                Actor.desde(request)
        );
    }

    @GetMapping({"/admin/locales/{idRestaurante}/productos", "/locales/{idRestaurante}/carta-operativa"})
    public List<ProductoMenuResponse> listarProductos(
            @PathVariable("idRestaurante") Long idRestaurante,
            HttpServletRequest request) {

        return servicio.listarProductos(
                idRestaurante,
                Actor.desde(request)
        );
    }

    @PostMapping("/admin/locales/{idRestaurante}/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoMenuResponse crearProducto(
            @PathVariable("idRestaurante") Long idRestaurante,
            @Valid @RequestBody ProductoDatos datos,
            HttpServletRequest request) {

        return servicio.guardarProducto(
                idRestaurante,
                null,
                datos,
                Actor.desde(request)
        );
    }

    @PutMapping("/admin/locales/{idRestaurante}/productos/{idProducto}")
    public ProductoMenuResponse editarProducto(
            @PathVariable("idRestaurante") Long idRestaurante,
            @PathVariable("idProducto") Long idProducto,
            @Valid @RequestBody ProductoDatos datos,
            HttpServletRequest request) {

        return servicio.guardarProducto(
                idRestaurante,
                idProducto,
                datos,
                Actor.desde(request)
        );
    }

    @PatchMapping("/locales/{idRestaurante}/productos/{idProducto}/disponibilidad")
    public ProductoMenuResponse cambiarDisponibilidad(
            @PathVariable("idRestaurante") Long idRestaurante,
            @PathVariable("idProducto") Long idProducto,
            @Valid @RequestBody DisponibilidadDatos datos,
            HttpServletRequest request) {

        return servicio.cambiarDisponibilidad(
                idRestaurante,
                idProducto,
                datos,
                Actor.desde(request)
        );
    }

    @GetMapping("/admin/locales/{idRestaurante}/asignaciones")
    public List<AsignacionRestauranteResponse> listarAsignaciones(
            @PathVariable("idRestaurante") Long idRestaurante,
            HttpServletRequest request) {

        return servicio.listarAsignaciones(
                idRestaurante,
                Actor.desde(request)
        );
    }

    @PutMapping("/admin/locales/{idRestaurante}/asignaciones")
    public AsignacionRestauranteResponse asignar(
            @PathVariable("idRestaurante") Long idRestaurante,
            @Valid @RequestBody AsignacionDatos datos,
            HttpServletRequest request) {

        return servicio.asignar(
                idRestaurante,
                datos,
                Actor.desde(request)
        );
    }

    @DeleteMapping("/admin/locales/{idRestaurante}/asignaciones/{idUsuario}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void retirarAsignacion(
            @PathVariable("idRestaurante") Long idRestaurante,
            @PathVariable("idUsuario") Long idUsuario,
            HttpServletRequest request) {

        servicio.retirarAsignacion(
                idRestaurante,
                idUsuario,
                Actor.desde(request)
        );
    }
}