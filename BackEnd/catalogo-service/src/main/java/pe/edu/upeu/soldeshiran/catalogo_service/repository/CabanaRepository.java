package pe.edu.upeu.soldeshiran.catalogo_service.repository;

import pe.edu.upeu.soldeshiran.catalogo_service.model.Cabana;
import pe.edu.upeu.soldeshiran.catalogo_service.model.EstadoCabana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaResumenResponse;

import java.util.List;
import java.util.Optional;

public interface CabanaRepository extends JpaRepository<Cabana, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cabana c WHERE c.idCabana = :id")
    Optional<Cabana> bloquearPorId(@Param("id") Long id);


    @Query("""
            SELECT new pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaResumenResponse(c.idCabana, c.nombre, c.imagenPrincipal)
            FROM Cabana c WHERE c.idCabana IN :ids
            """)
    List<CabanaResumenResponse> resumirPorIds(@Param("ids") List<Long> ids);

    List<Cabana> findByEstadoOrderByNombreAsc(EstadoCabana estado);

    boolean existsByDuenoId(Long duenoId);

    @Query("select count(c) from Cabana c where :id member of c.trabajadores")
    long contarAsignaciones(@Param("id") Long trabajadorId);

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
