package pe.edu.upeu.soldeshiran.restaurante_service.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Pedido;
import java.time.LocalDateTime;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoPedido;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

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

    @EntityGraph(attributePaths = {
            "restaurante",
            "detalles",
            "detalles.producto"
    })
    Optional<Pedido> findByIdPedidoAndIdUsuario(
            Long idPedido,
            Long idUsuario
    );

    @EntityGraph(attributePaths = "restaurante")
    @Query(
            value = """
                    select p
                    from Pedido p
                    where p.restaurante.idRestaurante = :idRestaurante
                    and (:estado is null or p.estadoPedido = :estado)
                    and (
                        :cabana is null
                        or lower(p.nombreCabana) like :cabana escape '!'
                    )
                    order by p.fechaPedido desc, p.idPedido desc
                    """,
            countQuery = """
                    select count(p)
                    from Pedido p
                    where p.restaurante.idRestaurante = :idRestaurante
                    and (:estado is null or p.estadoPedido = :estado)
                    and (
                        :cabana is null
                        or lower(p.nombreCabana) like :cabana escape '!'
                    )
                    """
    )
    Page<Pedido> consultarPanelRestaurante(
            @Param("idRestaurante") Long idRestaurante,
            @Param("estado") EstadoPedido estado,
            @Param("cabana") String cabana,
            Pageable pageable
    );

    long countByRestauranteIdRestauranteAndEstadoPedido(
            Long idRestaurante,
            EstadoPedido estadoPedido
    );

    @Query("""
            select count(p)
            from Pedido p
            where p.restaurante.idRestaurante = :idRestaurante
            and p.estadoPedido = :estado
            and p.fechaEntrega >= :desde
            and p.fechaEntrega < :hasta
            """)
    long contarFinalizadosEnPeriodo(
            @Param("idRestaurante") Long idRestaurante,
            @Param("estado") EstadoPedido estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Pedido p
            where p.idPedido = :idPedido
            """)
    Optional<Pedido> bloquearPorId(
            @Param("idPedido") Long idPedido
    );

    @EntityGraph(attributePaths = {
            "restaurante",
            "detalles",
            "detalles.producto"
    })
    Optional<Pedido> findByIdPedidoAndRestauranteIdRestaurante(
            Long idPedido,
            Long idRestaurante
    );
}