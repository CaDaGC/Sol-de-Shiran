package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import java.util.List;

public record InicioRestaurantesResponse(
        EstadiaActivaResponse estadias,
        EstadiaResponse entrega,
        List<RestauranteResponse> restaurantes
) {
}