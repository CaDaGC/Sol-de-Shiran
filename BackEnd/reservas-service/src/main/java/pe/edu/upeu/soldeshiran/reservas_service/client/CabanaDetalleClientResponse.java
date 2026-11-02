package pe.edu.upeu.soldeshiran.reservas_service.client;

import java.math.BigDecimal;

public record CabanaDetalleClientResponse(
        Long idCabana,
        String nombre,
        Integer aforoMaximo,
        BigDecimal tarifaNoche
) {
}