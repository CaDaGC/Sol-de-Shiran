package pe.edu.upeu.soldeshiran.pagos_service.dto;
import java.math.BigDecimal;
public record SaldoCheckInResponse(Long idReserva, BigDecimal saldoPendiente, String estadoReserva) {}
