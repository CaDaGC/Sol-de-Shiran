package pe.edu.upeu.soldeshiran.catalogo_service.dto;

import java.math.BigDecimal;

public record CabanaCatalogoResponse(
        Long idCabana,
        String nombre,
        String descripcion,
        Integer aforoMaximo,
        Integer numHabitaciones,
        Integer numBanos,
        BigDecimal tarifaNoche,
        String imagenPrincipal
) {
}
