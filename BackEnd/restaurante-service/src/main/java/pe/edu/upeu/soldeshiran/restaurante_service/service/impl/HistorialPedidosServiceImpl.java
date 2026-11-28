package pe.edu.upeu.soldeshiran.restaurante_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.HistorialPedidosResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.mapper.HistorialPedidosMapper;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.PedidoRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.HistorialPedidosService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HistorialPedidosServiceImpl implements HistorialPedidosService {

    private final PedidoRepository pedidos;
    private final HistorialPedidosMapper mapper;

    @Override
    public HistorialPedidosResponse consultar(
            Long idUsuario,
            Long idReserva,
            int pagina,
            int tamanio,
            Actor actor) {

        actor.exigir("CLIENTE");

        if (idUsuario == null || idUsuario <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del usuario debe ser positivo"
            );
        }

        if (!idUsuario.equals(actor.id())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo puedes consultar tu propio historial de pedidos"
            );
        }

        if (idReserva != null && idReserva <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador de la reserva debe ser positivo"
            );
        }

        if (pagina < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La página no puede ser negativa"
            );
        }

        if (tamanio < 1 || tamanio > 50) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El tamaño de página debe estar entre 1 y 50"
            );
        }

        var resultado = pedidos.consultarHistorial(
                actor.id(),
                idReserva,
                PageRequest.of(pagina, tamanio)
        );

        var contenido = resultado.getContent()
                .stream()
                .map(mapper::toResponse)
                .toList();

        return new HistorialPedidosResponse(
                contenido,
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                resultado.isFirst(),
                resultado.isLast()
        );
    }
}