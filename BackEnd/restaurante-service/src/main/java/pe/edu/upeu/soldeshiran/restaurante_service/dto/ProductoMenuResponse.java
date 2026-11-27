package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import java.math.BigDecimal;

public record ProductoMenuResponse(
        Long idProducto,
        Long idRestaurante,
        Long idCategoria,
        String nombreCategoria,
        String tipo,
        String nombre,
        String descripcion,
        BigDecimal precio,
        Integer tiempoPreparacionMin,
        Boolean disponible,
        String imagenUrl
) {
}