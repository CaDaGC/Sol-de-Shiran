package pe.edu.upeu.soldeshiran.restaurante_service.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.model.RestauranteTrabajador;
import java.util.List;

public interface RestauranteTrabajadorRepository
        extends JpaRepository<RestauranteTrabajador, Long> {

    @EntityGraph(attributePaths = "restaurante")
    Optional<RestauranteTrabajador> findByIdUsuario(Long idUsuario);

    List<RestauranteTrabajador>
    findByRestauranteIdRestauranteOrderByIdAsignacionAsc(
            Long idRestaurante
    );
}