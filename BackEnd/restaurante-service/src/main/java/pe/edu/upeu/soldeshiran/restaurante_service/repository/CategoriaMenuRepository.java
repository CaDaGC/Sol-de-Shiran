package pe.edu.upeu.soldeshiran.restaurante_service.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.model.CategoriaMenu;

public interface CategoriaMenuRepository
        extends JpaRepository<CategoriaMenu, Long> {

    List<CategoriaMenu>
    findByRestauranteIdRestauranteOrderByTipoAscNombreAscIdCategoriaAsc(
            Long idRestaurante
    );

    Optional<CategoriaMenu> findByIdCategoriaAndRestauranteIdRestaurante(
            Long idCategoria,
            Long idRestaurante
    );
}