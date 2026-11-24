package pe.edu.upeu.soldeshiran.pagos_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CrearPreferenciaRequest(
        @NotBlank @Email String payerEmail
) {
}