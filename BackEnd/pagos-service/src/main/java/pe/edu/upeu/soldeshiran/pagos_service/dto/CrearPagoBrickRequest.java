package pe.edu.upeu.soldeshiran.pagos_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CrearPagoBrickRequest(
        String token,
        @NotBlank String paymentMethodId,
        @Min(1) Integer installments,
        String issuerId,
        @NotBlank @Email String payerEmail
) {
}