package pe.edu.upeu.soldeshiran.resenas_service.repository;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.upeu.soldeshiran.resenas_service.model.Resena;

public interface ResenaRepository extends JpaRepository<Resena, Long> {
    Slice<Resena> findByIdCabanaOrderByIdResenaDesc(Long idCabana, Pageable pageable);

    @Query("select r.idReserva from Resena r where r.idUsuario = :usuario and r.idCabana = :cabana")
    List<Long> reservasResenadas(Long usuario, Long cabana);
}
