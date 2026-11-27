package pe.edu.upeu.soldeshiran.restaurante_service.dto;

public record CategoriaMenuResponse(
        Long idCategoria,
        Long idRestaurante,
        String nombre,
        String tipo
) {
}