package pe.edu.upeu.soldeshiran.pagos_service.dto;
public record DatosReservaComprobante(Long idReserva, Long idUsuario, String codigo, String nombreCabana,
    java.time.LocalDate fechaIngreso, java.time.LocalDate fechaSalida, java.math.BigDecimal montoTotal, String estadoReserva) {}
