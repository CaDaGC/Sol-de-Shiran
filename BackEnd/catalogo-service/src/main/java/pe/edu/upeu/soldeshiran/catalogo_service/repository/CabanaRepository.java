package pe.edu.upeu.soldeshiran.catalogo_service.repository;

import pe.edu.upeu.soldeshiran.catalogo_service.model.Cabana;
import pe.edu.upeu.soldeshiran.catalogo_service.model.EstadoCabana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CabanaRepository extends JpaRepository<Cabana, Long> {

    List<Cabana> findByEstadoOrderByNombreAsc(EstadoCabana estado);

    @Query("""
            SELECT DISTINCT c
            FROM Cabana c
            LEFT JOIN FETCH c.imagenes
            WHERE c.idCabana = :idCabana
              AND c.estado = :estado
            """)
    Optional<Cabana> buscarDetallePorIdYEstado(
            @Param("idCabana") Long idCabana,
            @Param("estado") EstadoCabana estado
    );
}