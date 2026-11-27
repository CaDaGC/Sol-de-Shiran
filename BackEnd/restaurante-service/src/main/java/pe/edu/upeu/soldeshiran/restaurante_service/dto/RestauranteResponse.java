package pe.edu.upeu.soldeshiran.restaurante_service.dto;

public record RestauranteResponse(
        Long idRestaurante,
        String nombre,
        String especialidad,
        String horarioAtencion,
        String imagenUrl,
        String estado
) {
}