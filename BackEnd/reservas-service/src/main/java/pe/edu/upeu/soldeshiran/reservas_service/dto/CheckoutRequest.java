package pe.edu.upeu.soldeshiran.reservas_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CheckoutRequest(@Valid Danos danos) {
  public record Danos(
      @NotBlank @Size(max = 500) String descripcion,
      @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal monto,
      @NotBlank @Pattern(regexp = "EFECTIVO|TARJETA|YAPE|PLIN") String medioPago) {}
}
