package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import java.util.List;

public record EstadiaActivaResponse(
        boolean activa,
        boolean requiereSeleccion,
        List<EstadiaResponse> estadias
) {
}