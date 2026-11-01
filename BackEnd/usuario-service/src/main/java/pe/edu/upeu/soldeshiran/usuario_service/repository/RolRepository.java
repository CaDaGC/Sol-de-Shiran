package pe.edu.upeu.soldeshiran.usuario_service.repository;

import pe.edu.upeu.soldeshiran.usuario_service.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol,Long> {

    Optional<Rol> findByNombre(String nombre);

}
