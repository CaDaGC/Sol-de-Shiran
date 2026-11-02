package pe.edu.upeu.soldeshiran.reservas_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CotizacionReservaResponse(
        Long idCabana,
        String nombreCabana,
        LocalDate fechaIngreso,
        LocalDate fechaSalida,
        Integer cantidadHuespedes,
        Long noches,
        BigDecimal tarifaNoche,
        BigDecimal montoTotal,
        BigDecimal montoAdelanto,
        BigDecimal saldoPendiente,
        boolean disponible,
        String mensaje
) {
}