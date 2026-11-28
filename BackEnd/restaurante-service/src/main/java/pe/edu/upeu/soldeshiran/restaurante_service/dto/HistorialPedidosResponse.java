package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import java.util.List;

public record HistorialPedidosResponse(
        List<PedidoResumenResponse> pedidos,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        boolean primera,
        boolean ultima
) {
}