package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoPedido;

public record EstadoPedidoRequest(
        @NotNull(message = "Selecciona el nuevo estado")
        EstadoPedido estado,

        @Size(
                max = 255,
                message = "El motivo debe tener como máximo 255 caracteres"
        )
        String motivo
) {
}