package pe.edu.upeu.soldeshiran.restaurante_service.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Pedido;

public interface PedidoRepository
        extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = {
            "restaurante",
            "detalles",
            "detalles.producto"
    })
    Optional<Pedido> findByIdUsuarioAndClaveSolicitud(
            Long idUsuario,
            String claveSolicitud
    );
}