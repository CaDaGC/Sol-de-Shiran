package pe.edu.upeu.soldeshiran.reservas_service.dto;

import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservaResponse(
Long idReserva,
        Long idCabana,
        Long idUsuario,
        LocalDate fechaIngreso,
        LocalDate fechaSalida,
        Integer cantidadHuespedes,
        BigDecimal tarifaNocheAplicada,
        BigDecimal montoTotal,
        BigDecimal montoAdelanto,
        BigDecimal saldoPendiente,
        EstadoReserva estadoReserva,
        LocalDateTime expiraEn,
        java.time.LocalTime horaIngreso,
        java.time.LocalTime horaSalida,
        String codigo
) {
  public ReservaResponse(Long idReserva,
        Long idCabana,
        Long idUsuario,
        LocalDate fechaIngreso,
        LocalDate fechaSalida,
        Integer cantidadHuespedes,
        BigDecimal tarifaNocheAplicada,
        BigDecimal montoTotal,
        BigDecimal montoAdelanto,
        BigDecimal saldoPendiente,
        EstadoReserva estadoReserva,
        LocalDateTime expiraEn,
        java.time.LocalTime horaIngreso,
        java.time.LocalTime horaSalida) {
    this(idReserva, idCabana, idUsuario, fechaIngreso, fechaSalida, cantidadHuespedes, tarifaNocheAplicada, montoTotal, montoAdelanto, saldoPendiente, estadoReserva, expiraEn, horaIngreso, horaSalida, null);
  }

  public ReservaResponse(Long idReserva,
        Long idCabana,
        Long idUsuario,
        LocalDate fechaIngreso,
        LocalDate fechaSalida,
        Integer cantidadHuespedes,
        BigDecimal tarifaNocheAplicada,
        BigDecimal montoTotal,
        BigDecimal montoAdelanto,
        BigDecimal saldoPendiente,
        EstadoReserva estadoReserva,
        LocalDateTime expiraEn) {
    this(idReserva, idCabana, idUsuario, fechaIngreso, fechaSalida, cantidadHuespedes, tarifaNocheAplicada, montoTotal, montoAdelanto, saldoPendiente, estadoReserva, expiraEn, java.time.LocalTime.MIDNIGHT, java.time.LocalTime.MIDNIGHT);
  }

}