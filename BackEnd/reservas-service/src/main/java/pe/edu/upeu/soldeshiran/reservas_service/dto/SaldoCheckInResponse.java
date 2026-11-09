package pe.edu.upeu.soldeshiran.reservas_service.dto;
import java.math.BigDecimal;
public record SaldoCheckInResponse(Long idReserva, BigDecimal saldoPendiente, String estadoReserva) {}
