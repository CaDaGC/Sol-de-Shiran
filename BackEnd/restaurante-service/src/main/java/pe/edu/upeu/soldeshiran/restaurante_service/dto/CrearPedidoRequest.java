package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import pe.edu.upeu.soldeshiran.restaurante_service.model.MetodoPagoPresencial;

public record CrearPedidoRequest(
        @NotNull(message = "Falta la clave de la solicitud.")
        UUID claveSolicitud,

        @NotNull(message = "Selecciona el restaurante.")
        @Positive(message = "El restaurante debe ser válido.")
        Long idRestaurante,

        @NotNull(message = "Selecciona la reserva de entrega.")
        @Positive(message = "La reserva debe ser válida.")
        Long idReserva,

        @NotNull(message = "Selecciona el método de pago presencial.")
        MetodoPagoPresencial metodoPagoPresencial,

        @NotEmpty(message = "Agrega al menos un producto.")
        @Size(
                max = 100,
                message = "El pedido admite hasta 100 líneas."
        )
        List<@NotNull @Valid DetallePedidoRequest> detalles
) {
}