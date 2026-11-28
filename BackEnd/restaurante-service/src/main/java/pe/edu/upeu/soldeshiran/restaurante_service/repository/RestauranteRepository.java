package pe.edu.upeu.soldeshiran.restaurante_service.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoRestaurante;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Restaurante;

public interface RestauranteRepository
        extends JpaRepository<Restaurante, Long> {

    List<Restaurante> findByEstadoOrderByNombreAsc(
            EstadoRestaurante estado
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select restaurante
        from Restaurante restaurante
        where restaurante.idRestaurante = :id
        """)
    Optional<Restaurante> bloquearPorId(@Param("id") Long id);
}