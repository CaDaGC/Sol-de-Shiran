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
        String mensaje,
        java.time.LocalTime horaIngreso,
        java.time.LocalTime horaSalida
) {
  public CotizacionReservaResponse(Long idCabana,
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
        String mensaje) {
    this(idCabana, nombreCabana, fechaIngreso, fechaSalida, cantidadHuespedes, noches, tarifaNoche, montoTotal, montoAdelanto, saldoPendiente, disponible, mensaje, java.time.LocalTime.MIDNIGHT, java.time.LocalTime.MIDNIGHT);
  }

}