package pe.edu.upeu.soldeshiran.pagos_service.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** El importe se obtiene de reservas; la clave identifica los reintentos del mismo pago. */
public record RegistrarPagoRestanteRequest(
        @NotNull MetodoPago metodoPago,
        @Size(max = 100) String numeroOperacion,
        @NotNull UUID claveIdempotencia
) {
    public enum MetodoPago { EFECTIVO, TARJETA, YAPE_BILLETERA }

    @AssertTrue(message = "El número de operación es obligatorio para tarjeta y Yape/Billetera")
    public boolean isNumeroOperacionValido() {
        return metodoPago == null || metodoPago == MetodoPago.EFECTIVO
                || (numeroOperacion != null && !numeroOperacion.isBlank());
    }
}
