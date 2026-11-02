package pe.edu.upeu.soldeshiran.catalogo_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record CabanaDetalleResponse(
        Long idCabana,
        String nombre,
        String descripcion,
        Integer aforoMaximo,
        Integer numHabitaciones,
        Integer numBanos,
        BigDecimal tarifaNoche,
        String imagenPrincipal,
        List<String> imagenesSecundarias
) {
}
