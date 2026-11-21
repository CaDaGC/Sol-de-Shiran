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
    Optional<Usuario> findByNombre(String nombre);



    @Query("""
            SELECT u
            FROM Usuario u
            WHERE LOWER(u.nombre) LIKE :patron ESCAPE '!'
               OR LOWER(u.email) LIKE :patron ESCAPE '!'
            """)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "persona")
    List<Usuario> buscarEntidadesResumen(@Param("patron") String patron);

    default List<UsuarioResumenResponse> buscarResumen(String patron) {
      return buscarEntidadesResumen(patron).stream().map(UsuarioResumenResponse::desde).toList();
    }

    @Query("""
            SELECT u
            FROM Usuario u WHERE u.id IN :ids
            """)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "persona")
    List<Usuario> resumirEntidadesPorIds(@Param("ids") List<Long> ids);

    default List<UsuarioResumenResponse> resumirPorIds(List<Long> ids) {
      return resumirEntidadesPorIds(ids).stream().map(UsuarioResumenResponse::desde).toList();
    }

}
