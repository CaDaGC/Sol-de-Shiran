package pe.edu.upeu.soldeshiran.restaurante_service.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoRestaurante;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Restaurante;

public interface RestauranteRepository
        extends JpaRepository<Restaurante, Long> {

    List<Restaurante> findByEstadoOrderByNombreAsc(
            EstadoRestaurante estado
    );
}