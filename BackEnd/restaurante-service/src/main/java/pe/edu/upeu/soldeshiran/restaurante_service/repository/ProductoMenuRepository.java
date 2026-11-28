package pe.edu.upeu.soldeshiran.restaurante_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.soldeshiran.restaurante_service.model.ProductoMenu;
import pe.edu.upeu.soldeshiran.restaurante_service.model.TipoCategoria;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface ProductoMenuRepository
        extends JpaRepository<ProductoMenu, Long> {

    @Query("""
        select producto
        from ProductoMenu producto
        join fetch producto.categoria categoria
        where producto.restaurante.idRestaurante = :idRestaurante
          and categoria.restaurante.idRestaurante = :idRestaurante
          and (
              :idCategoria is null
              or categoria.idCategoria = :idCategoria
          )
          and (
              :tipo is null
              or categoria.tipo = :tipo
          )
        order by categoria.nombre, producto.nombre, producto.idProducto
        """)
    List<ProductoMenu> consultarCarta(
            @Param("idRestaurante") Long idRestaurante,
            @Param("idCategoria") Long idCategoria,
            @Param("tipo") TipoCategoria tipo
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select producto
    from ProductoMenu producto
    join fetch producto.categoria categoria
    where producto.idProducto in :ids
      and producto.restaurante.idRestaurante = :idRestaurante
      and categoria.restaurante.idRestaurante = :idRestaurante
    order by producto.idProducto
    """)
    List<ProductoMenu> buscarParaPedido(
            @Param("ids") List<Long> ids,
            @Param("idRestaurante") Long idRestaurante
    );

    boolean existsByCategoriaIdCategoria(Long idCategoria);
}