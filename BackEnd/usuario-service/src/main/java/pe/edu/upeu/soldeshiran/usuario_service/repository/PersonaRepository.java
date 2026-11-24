package pe.edu.upeu.soldeshiran.usuario_service.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.soldeshiran.usuario_service.model.Persona;
public interface PersonaRepository extends JpaRepository<Persona, Long> {
  Optional<Persona> findByNumeroDocumento(String numeroDocumento);
}
