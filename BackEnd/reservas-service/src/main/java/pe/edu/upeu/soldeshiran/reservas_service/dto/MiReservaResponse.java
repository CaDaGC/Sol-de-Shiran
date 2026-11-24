package pe.edu.upeu.soldeshiran.reservas_service.dto;

import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record MiReservaResponse(
Long idReserva, Long idCabana, Long idUsuario, String nombreCabana, String imagenPrincipal,
        LocalDate fechaIngreso, LocalDate fechaSalida, Integer cantidadHuespedes,
        BigDecimal tarifaNocheAplicada, BigDecimal montoTotal, BigDecimal montoAdelanto,
        BigDecimal saldoPendiente, EstadoReserva estadoReserva, LocalDateTime expiraEn,
        GrupoReservasCliente grupo, boolean pagoDisponible,
        java.time.LocalTime horaIngreso,
        java.time.LocalTime horaSalida,
        String codigo
) {
  public MiReservaResponse(Long idReserva, Long idCabana, Long idUsuario, String nombreCabana, String imagenPrincipal,
        LocalDate fechaIngreso, LocalDate fechaSalida, Integer cantidadHuespedes,
        BigDecimal tarifaNocheAplicada, BigDecimal montoTotal, BigDecimal montoAdelanto,
        BigDecimal saldoPendiente, EstadoReserva estadoReserva, LocalDateTime expiraEn,
        GrupoReservasCliente grupo, boolean pagoDisponible,
        java.time.LocalTime horaIngreso,
        java.time.LocalTime horaSalida) {
    this(idReserva, idCabana, idUsuario, nombreCabana, imagenPrincipal, fechaIngreso, fechaSalida, cantidadHuespedes, tarifaNocheAplicada, montoTotal, montoAdelanto, saldoPendiente, estadoReserva, expiraEn, grupo, pagoDisponible, horaIngreso, horaSalida, null);
  }

  public MiReservaResponse(Long idReserva, Long idCabana, Long idUsuario, String nombreCabana, String imagenPrincipal,
        LocalDate fechaIngreso, LocalDate fechaSalida, Integer cantidadHuespedes,
        BigDecimal tarifaNocheAplicada, BigDecimal montoTotal, BigDecimal montoAdelanto,
        BigDecimal saldoPendiente, EstadoReserva estadoReserva, LocalDateTime expiraEn,
        GrupoReservasCliente grupo, boolean pagoDisponible) {
    this(idReserva, idCabana, idUsuario, nombreCabana, imagenPrincipal, fechaIngreso, fechaSalida, cantidadHuespedes, tarifaNocheAplicada, montoTotal, montoAdelanto, saldoPendiente, estadoReserva, expiraEn, grupo, pagoDisponible, java.time.LocalTime.MIDNIGHT, java.time.LocalTime.MIDNIGHT);
  }

}
