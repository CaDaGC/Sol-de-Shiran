package pe.edu.upeu.soldeshiran.catalogo_service.repository;

import pe.edu.upeu.soldeshiran.catalogo_service.model.Cabana;
import pe.edu.upeu.soldeshiran.catalogo_service.model.EstadoCabana;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CabanaRepository extends JpaRepository<Cabana, Long> {

    List<Cabana> findByEstadoOrderByNombreAsc(EstadoCabana estado);
}