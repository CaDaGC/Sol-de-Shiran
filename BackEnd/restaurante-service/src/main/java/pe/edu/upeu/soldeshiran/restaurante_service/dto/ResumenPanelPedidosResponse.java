package pe.edu.upeu.soldeshiran.restaurante_service.dto;

public record ResumenPanelPedidosResponse(
        long nuevos,
        long enPreparacion,
        long enEntrega,
        long finalizadosHoy
) {
}