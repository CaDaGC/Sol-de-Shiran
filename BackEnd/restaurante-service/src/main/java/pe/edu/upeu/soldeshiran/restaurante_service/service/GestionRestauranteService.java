package pe.edu.upeu.soldeshiran.restaurante_service.service;

import java.util.List;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.AsignacionRestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.CategoriaMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.GestionRestauranteRequest.*;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.ProductoMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.RestauranteResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;

public interface GestionRestauranteService {

    List<RestauranteResponse> listarRestaurantes(Actor actor);

    RestauranteResponse crearRestaurante(
            RestauranteDatos datos,
            Actor actor
    );

    RestauranteResponse editarRestaurante(
            Long idRestaurante,
            RestauranteDatos datos,
            Actor actor
    );

    RestauranteResponse cambiarEstado(
            Long idRestaurante,
            EstadoDatos datos,
            Actor actor
    );

    List<CategoriaMenuResponse> listarCategorias(
            Long idRestaurante,
            Actor actor
    );

    CategoriaMenuResponse guardarCategoria(
            Long idRestaurante,
            Long idCategoria,
            CategoriaDatos datos,
            Actor actor
    );

    void eliminarCategoria(
            Long idRestaurante,
            Long idCategoria,
            Actor actor
    );

    List<ProductoMenuResponse> listarProductos(
            Long idRestaurante,
            Actor actor
    );

    ProductoMenuResponse guardarProducto(
            Long idRestaurante,
            Long idProducto,
            ProductoDatos datos,
            Actor actor
    );

    ProductoMenuResponse cambiarDisponibilidad(
            Long idRestaurante,
            Long idProducto,
            DisponibilidadDatos datos,
            Actor actor
    );

    List<AsignacionRestauranteResponse> listarAsignaciones(
            Long idRestaurante,
            Actor actor
    );

    AsignacionRestauranteResponse asignar(
            Long idRestaurante,
            AsignacionDatos datos,
            Actor actor
    );

    void retirarAsignacion(
            Long idRestaurante,
            Long idUsuario,
            Actor actor
    );
}