package pe.edu.upeu.soldeshiran.restaurante_service.dto;

public record SeleccionRestauranteResponse(
        EstadiaResponse entrega,
        RestauranteResponse restaurante
) {
}