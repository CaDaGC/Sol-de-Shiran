package pe.edu.upeu.soldeshiran.restaurante_service.mapper;

import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.CategoriaMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.ProductoMenuResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.model.CategoriaMenu;
import pe.edu.upeu.soldeshiran.restaurante_service.model.ProductoMenu;

@Component
public class CartaMenuMapper {

    public CategoriaMenuResponse toResponse(CategoriaMenu categoria) {
        return new CategoriaMenuResponse(
                categoria.getIdCategoria(),
                categoria.getRestaurante().getIdRestaurante(),
                categoria.getNombre(),
                categoria.getTipo().name()
        );
    }

    public ProductoMenuResponse toResponse(ProductoMenu producto) {
        var categoria = producto.getCategoria();

        return new ProductoMenuResponse(
                producto.getIdProducto(),
                producto.getRestaurante().getIdRestaurante(),
                categoria.getIdCategoria(),
                categoria.getNombre(),
                categoria.getTipo().name(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getTiempoPreparacionMin(),
                producto.getDisponible(),
                producto.getImagenUrl()
        );
    }
}