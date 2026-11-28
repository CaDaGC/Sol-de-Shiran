package pe.edu.upeu.soldeshiran.restaurante_service.dto;

import java.util.List;

public record PanelPedidosResponse(
        RestauranteResponse restaurante,
        ResumenPanelPedidosResponse resumen,
        List<PedidoPanelResponse> pedidos,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        boolean primera,
        boolean ultima
) {
}