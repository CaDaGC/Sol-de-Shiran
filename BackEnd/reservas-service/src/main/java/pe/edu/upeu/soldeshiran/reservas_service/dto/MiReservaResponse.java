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
        GrupoReservasCliente grupo, boolean pagoDisponible) {
}
