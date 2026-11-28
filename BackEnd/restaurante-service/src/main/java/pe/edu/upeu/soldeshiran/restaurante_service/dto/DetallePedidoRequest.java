package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DetallePedidoRequest(
        @NotNull(message = "Selecciona un producto.")
        @Positive(message = "El producto debe ser válido.")
        Long idProducto,

        @NotNull(message = "Indica la cantidad.")
        @Positive(message = "La cantidad debe ser mayor que cero.")
        Integer cantidad,

        @Size(
                max = 255,
                message = "Las observaciones admiten hasta 255 caracteres."
        )
        String observaciones
) {
}