package pe.edu.upeu.soldeshiran.usuario_service.repository;

import pe.edu.upeu.soldeshiran.usuario_service.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioResumenResponse;

import java.util.List;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findAllByRol(pe.edu.upeu.soldeshiran.usuario_service.model.Rol rol);
    boolean existsByEmail(String email);

    boolean existsByDuenoId(Long duenoId);

    List<Usuario> findAllByDuenoId(Long duenoId);

    @Query("""
            SELECT new pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioResumenResponse(u.id, u.nombre, u.email)
            FROM Usuario u
            WHERE LOWER(u.nombre) LIKE :patron ESCAPE '!'
               OR LOWER(u.email) LIKE :patron ESCAPE '!'
            """)
    List<UsuarioResumenResponse> buscarResumen(@Param("patron") String patron);

    @Query("""
            SELECT new pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioResumenResponse(u.id, u.nombre, u.email)
            FROM Usuario u WHERE u.id IN :ids
            """)
    List<UsuarioResumenResponse> resumirPorIds(@Param("ids") List<Long> ids);

}
