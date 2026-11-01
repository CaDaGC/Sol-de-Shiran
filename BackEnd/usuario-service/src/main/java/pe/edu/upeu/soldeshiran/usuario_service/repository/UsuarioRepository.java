package pe.edu.upeu.soldeshiran.usuario_service.repository;

import pe.edu.upeu.soldeshiran.usuario_service.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

}
