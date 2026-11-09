package pe.edu.upeu.soldeshiran.reservas_service.dto;

import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ReservaPanelResponse(
        Long idReserva, Long idUsuario, String nombreUsuario, String emailUsuario,
        Long idCabana, String nombreCabana, LocalDate fechaIngreso, LocalDate fechaSalida,
        Integer cantidadHuespedes, BigDecimal montoTotal, BigDecimal montoAdelanto,
        BigDecimal saldoPendiente, EstadoReserva estadoReserva) {
}
