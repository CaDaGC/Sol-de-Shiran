package pe.edu.upeu.soldeshiran.restaurante_service.service;

import java.util.List;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.CategoriaMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.ProductoMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.model.TipoCategoria;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;

public interface CartaMenuService {

    List<CategoriaMenuResponse> consultarCategorias(
            Long idRestaurante,
            Long idReserva,
            Actor actor
    );

    List<ProductoMenuResponse> consultarProductos(
            Long idRestaurante,
            Long idReserva,
            Long idCategoria,
            TipoCategoria tipo,
            Actor actor
    );
}