package pe.edu.upeu.soldeshiran.reservas_service.dto;

import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;
import pe.edu.upeu.soldeshiran.reservas_service.model.RegistroCheckout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservaDetalleResponse(
Long idReserva,
        Long idUsuario,
        String nombreUsuario,
        String emailUsuario,
        Long idCabana,
        String nombreCabana,
        LocalDate fechaIngreso,
        LocalDate fechaSalida,
        Integer cantidadHuespedes,
        BigDecimal tarifaNocheAplicada,
        BigDecimal montoTotal,
        BigDecimal montoAdelanto,
        BigDecimal saldoPendiente,
        EstadoReserva estadoReserva,
        LocalDateTime expiraEn,
        LocalDateTime fechaCreacion,
        RegistroCheckout checkout,
        java.time.LocalTime horaIngreso,
        java.time.LocalTime horaSalida,
        PersonaClienteResponse persona,
        String fotoPerfilUsuario,
        String codigo
) {
  public ReservaDetalleResponse(Long idReserva,
        Long idUsuario,
        String nombreUsuario,
        String emailUsuario,
        Long idCabana,
        String nombreCabana,
        LocalDate fechaIngreso,
        LocalDate fechaSalida,
        Integer cantidadHuespedes,
        BigDecimal tarifaNocheAplicada,
        BigDecimal montoTotal,
        BigDecimal montoAdelanto,
        BigDecimal saldoPendiente,
        EstadoReserva estadoReserva,
        LocalDateTime expiraEn,
        LocalDateTime fechaCreacion,
        RegistroCheckout checkout,
        java.time.LocalTime horaIngreso,
        java.time.LocalTime horaSalida,
        PersonaClienteResponse persona,
        String fotoPerfilUsuario) {
    this(idReserva, idUsuario, nombreUsuario, emailUsuario, idCabana, nombreCabana, fechaIngreso, fechaSalida, cantidadHuespedes, tarifaNocheAplicada, montoTotal, montoAdelanto, saldoPendiente, estadoReserva, expiraEn, fechaCreacion, checkout, horaIngreso, horaSalida, persona, fotoPerfilUsuario, null);
  }

  public ReservaDetalleResponse(Long idReserva, Long idUsuario, String nombreUsuario, String emailUsuario,
        Long idCabana, String nombreCabana, LocalDate fechaIngreso, LocalDate fechaSalida,
        Integer cantidadHuespedes, BigDecimal tarifaNocheAplicada, BigDecimal montoTotal,
        BigDecimal montoAdelanto, BigDecimal saldoPendiente, EstadoReserva estadoReserva,
        LocalDateTime expiraEn, LocalDateTime fechaCreacion, RegistroCheckout checkout,
        java.time.LocalTime horaIngreso, java.time.LocalTime horaSalida) {
    this(idReserva,idUsuario,nombreUsuario,emailUsuario,idCabana,nombreCabana,fechaIngreso,fechaSalida,
        cantidadHuespedes,tarifaNocheAplicada,montoTotal,montoAdelanto,saldoPendiente,estadoReserva,
        expiraEn,fechaCreacion,checkout,horaIngreso,horaSalida,null,null);
  }
  public ReservaDetalleResponse(Long idReserva,
        Long idUsuario,
        String nombreUsuario,
        String emailUsuario,
        Long idCabana,
        String nombreCabana,
        LocalDate fechaIngreso,
        LocalDate fechaSalida,
        Integer cantidadHuespedes,
        BigDecimal tarifaNocheAplicada,
        BigDecimal montoTotal,
        BigDecimal montoAdelanto,
        BigDecimal saldoPendiente,
        EstadoReserva estadoReserva,
        LocalDateTime expiraEn,
        LocalDateTime fechaCreacion,
        RegistroCheckout checkout) {
    this(idReserva, idUsuario, nombreUsuario, emailUsuario, idCabana, nombreCabana, fechaIngreso, fechaSalida, cantidadHuespedes, tarifaNocheAplicada, montoTotal, montoAdelanto, saldoPendiente, estadoReserva, expiraEn, fechaCreacion, checkout, java.time.LocalTime.MIDNIGHT, java.time.LocalTime.MIDNIGHT, null, null);
  }

}
