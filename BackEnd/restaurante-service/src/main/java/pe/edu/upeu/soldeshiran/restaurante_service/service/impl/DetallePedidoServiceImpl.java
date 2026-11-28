package pe.edu.upeu.soldeshiran.restaurante_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.PedidoResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.mapper.PedidoMapper;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.PedidoRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.DetallePedidoService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DetallePedidoServiceImpl implements DetallePedidoService {

    private final PedidoRepository pedidos;
    private final PedidoMapper mapper;

    @Override
    public PedidoResponse consultar(Long idPedido, Actor actor) {
        actor.exigir("CLIENTE");

        if (idPedido == null || idPedido <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del pedido debe ser positivo"
            );
        }

        var pedido = pedidos.findByIdPedidoAndIdUsuario(
                idPedido,
                actor.id()
        ).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No se encontró el pedido en tu cuenta"
        ));

        return mapper.toResponse(pedido);
    }
}