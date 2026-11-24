package pe.edu.upeu.soldeshiran.reservas_service.dto;

import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservaPanelResponse(
Long idReserva, Long idUsuario, String nombreUsuario, String emailUsuario,
        Long idCabana, String nombreCabana, LocalDate fechaIngreso, LocalDate fechaSalida,
        Integer cantidadHuespedes, BigDecimal montoTotal, BigDecimal montoAdelanto,
        BigDecimal saldoPendiente, EstadoReserva estadoReserva,
        java.time.LocalTime horaIngreso,
        java.time.LocalTime horaSalida,
        String codigo
) {
  public ReservaPanelResponse(Long idReserva, Long idUsuario, String nombreUsuario, String emailUsuario,
        Long idCabana, String nombreCabana, LocalDate fechaIngreso, LocalDate fechaSalida,
        Integer cantidadHuespedes, BigDecimal montoTotal, BigDecimal montoAdelanto,
        BigDecimal saldoPendiente, EstadoReserva estadoReserva,
        java.time.LocalTime horaIngreso,
        java.time.LocalTime horaSalida) {
    this(idReserva, idUsuario, nombreUsuario, emailUsuario, idCabana, nombreCabana, fechaIngreso, fechaSalida, cantidadHuespedes, montoTotal, montoAdelanto, saldoPendiente, estadoReserva, horaIngreso, horaSalida, null);
  }

  public ReservaPanelResponse(Long idReserva, Long idUsuario, String nombreUsuario, String emailUsuario,
        Long idCabana, String nombreCabana, LocalDate fechaIngreso, LocalDate fechaSalida,
        Integer cantidadHuespedes, BigDecimal montoTotal, BigDecimal montoAdelanto,
        BigDecimal saldoPendiente, EstadoReserva estadoReserva) {
    this(idReserva, idUsuario, nombreUsuario, emailUsuario, idCabana, nombreCabana, fechaIngreso, fechaSalida, cantidadHuespedes, montoTotal, montoAdelanto, saldoPendiente, estadoReserva, java.time.LocalTime.MIDNIGHT, java.time.LocalTime.MIDNIGHT);
  }

}
