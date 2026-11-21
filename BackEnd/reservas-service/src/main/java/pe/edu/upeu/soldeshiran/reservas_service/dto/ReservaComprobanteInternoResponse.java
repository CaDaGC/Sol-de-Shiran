package pe.edu.upeu.soldeshiran.reservas_service.dto;
public record ReservaComprobanteInternoResponse(Long idReserva, Long idUsuario, String codigo, String nombreCabana,
    java.time.LocalDate fechaIngreso, java.time.LocalDate fechaSalida, java.math.BigDecimal montoTotal, String estadoReserva) {}
