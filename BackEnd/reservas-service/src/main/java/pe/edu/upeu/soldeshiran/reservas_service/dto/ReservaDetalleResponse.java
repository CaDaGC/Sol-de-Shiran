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
        RegistroCheckout checkout
) {
}
