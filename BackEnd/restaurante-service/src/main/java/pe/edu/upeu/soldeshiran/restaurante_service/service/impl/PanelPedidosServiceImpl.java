package pe.edu.upeu.soldeshiran.restaurante_service.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.PanelPedidosResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.ResumenPanelPedidosResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.mapper.PanelPedidosMapper;
import pe.edu.upeu.soldeshiran.restaurante_service.mapper.RestauranteMapper;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoPedido;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.PedidoRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.RestauranteTrabajadorRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.PanelPedidosService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PanelPedidosServiceImpl implements PanelPedidosService {

    private final PedidoRepository pedidos;
    private final RestauranteTrabajadorRepository asignaciones;
    private final PanelPedidosMapper pedidosMapper;
    private final RestauranteMapper restauranteMapper;

    @Override
    public PanelPedidosResponse consultar(
            Long idRestaurante,
            EstadoPedido estado,
            String cabana,
            int pagina,
            int tamanio,
            Actor actor) {

        actor.exigir("RESTAURANTE");

        if (idRestaurante == null || idRestaurante <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador del restaurante debe ser positivo"
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

        var asignacion = asignaciones.findByIdUsuario(actor.id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tu cuenta no tiene un restaurante asignado"
                ));

        var restaurante = asignacion.getRestaurante();

        if (!restaurante.getIdRestaurante().equals(idRestaurante)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo puedes consultar los pedidos de tu restaurante"
            );
        }

        var filtroCabana = prepararFiltroCabana(cabana);

        var resultado = pedidos.consultarPanelRestaurante(
                restaurante.getIdRestaurante(),
                estado,
                filtroCabana,
                PageRequest.of(pagina, tamanio)
        );

        var hoy = LocalDate.now(ZoneId.of("America/Lima"));
        var desde = hoy.atStartOfDay();
        var hasta = hoy.plusDays(1).atStartOfDay();

        var resumen = new ResumenPanelPedidosResponse(
                pedidos.countByRestauranteIdRestauranteAndEstadoPedido(
                        idRestaurante,
                        EstadoPedido.NUEVO
                ),
                pedidos.countByRestauranteIdRestauranteAndEstadoPedido(
                        idRestaurante,
                        EstadoPedido.EN_PREPARACION
                ),
                pedidos.countByRestauranteIdRestauranteAndEstadoPedido(
                        idRestaurante,
                        EstadoPedido.EN_ENTREGA
                ),
                pedidos.contarFinalizadosEnPeriodo(
                        idRestaurante,
                        EstadoPedido.FINALIZADO,
                        desde,
                        hasta
                )
        );

        var contenido = resultado.getContent()
                .stream()
                .map(pedidosMapper::toResponse)
                .toList();

        return new PanelPedidosResponse(
                restauranteMapper.toResponse(restaurante),
                resumen,
                contenido,
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages(),
                resultado.isFirst(),
                resultado.isLast()
        );
    }

    private String prepararFiltroCabana(String cabana) {
        if (cabana == null || cabana.isBlank()) {
            return null;
        }

        var texto = cabana.strip();

        if (texto.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El filtro de cabaña debe tener como máximo 100 caracteres"
            );
        }

        texto = texto.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");

        return "%" + texto + "%";
    }
}