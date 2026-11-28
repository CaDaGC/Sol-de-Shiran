package pe.edu.upeu.soldeshiran.restaurante_service.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = {
            "restaurante",
            "detalles",
            "detalles.producto"
    })
    Optional<Pedido> findByIdUsuarioAndClaveSolicitud(
            Long idUsuario,
            String claveSolicitud
    );

    @EntityGraph(attributePaths = "restaurante")
    @Query(
            value = """
                    select p
                    from Pedido p
                    where p.idUsuario = :idUsuario
                    and (:idReserva is null or p.idReserva = :idReserva)
                    order by p.fechaPedido desc, p.idPedido desc
                    """,
            countQuery = """
                    select count(p)
                    from Pedido p
                    where p.idUsuario = :idUsuario
                    and (:idReserva is null or p.idReserva = :idReserva)
                    """
    )
    Page<Pedido> consultarHistorial(
            @Param("idUsuario") Long idUsuario,
            @Param("idReserva") Long idReserva,
            Pageable pageable
    );
}