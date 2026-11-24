package pe.edu.upeu.soldeshiran.pagos_service.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record CheckInRequest(@NotBlank String referenciaPago, @NotNull @DecimalMin("0.00") BigDecimal monto) {}
