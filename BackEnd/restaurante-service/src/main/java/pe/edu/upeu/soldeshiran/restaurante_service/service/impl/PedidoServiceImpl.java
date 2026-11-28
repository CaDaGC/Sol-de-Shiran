package pe.edu.upeu.soldeshiran.restaurante_service.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.CrearPedidoRequest;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.CreacionPedidoResultado;
import pe.edu.upeu.soldeshiran.restaurante_service.mapper.PedidoMapper;
import pe.edu.upeu.soldeshiran.restaurante_service.model.DetallePedido;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoRestaurante;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Pedido;
import pe.edu.upeu.soldeshiran.restaurante_service.model.ProductoMenu;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.PedidoRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.ProductoMenuRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.RestauranteRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.HorarioRestauranteService;
import pe.edu.upeu.soldeshiran.restaurante_service.service.PedidoService;
import pe.edu.upeu.soldeshiran.restaurante_service.service.RestauranteConsultaService;

@Service
@RequiredArgsConstructor
@Transactional
public class PedidoServiceImpl implements PedidoService {

    private static final BigDecimal MONTO_MAXIMO =
            new BigDecimal("99999999.99");

    private final PedidoRepository pedidos;
    private final RestauranteRepository restaurantes;
    private final ProductoMenuRepository productos;
    private final RestauranteConsultaService consultas;
    private final HorarioRestauranteService horarios;
    private final PedidoMapper mapper;
    private final ObjectMapper json;

    @Override
    public CreacionPedidoResultado crear(
            CrearPedidoRequest request,
            Actor actor) {

        actor.exigir("CLIENTE");

        String clave = request.claveSolicitud().toString();
        String huella = calcularHuella(request);

        var existente = pedidos.findByIdUsuarioAndClaveSolicitud(
                actor.id(),
                clave
        );

        if (existente.isPresent()) {
            return responderExistente(existente.get(), huella);
        }

        var restaurante = restaurantes
                .bloquearPorId(request.idRestaurante())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El restaurante no está disponible."
                ));

        if (restaurante.getEstado() != EstadoRestaurante.ACTIVO) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El restaurante no está activo."
            );
        }

        existente = pedidos.findByIdUsuarioAndClaveSolicitud(
                actor.id(),
                clave
        );

        if (existente.isPresent()) {
            return responderExistente(existente.get(), huella);
        }

        horarios.exigirHorarioDePedidos();

        var seleccion = consultas.seleccionarRestaurante(
                request.idRestaurante(),
                request.idReserva(),
                actor
        );

        var idsProductos = request.detalles().stream()
                .map(detalle -> detalle.idProducto())
                .distinct()
                .sorted()
                .toList();

        var encontrados = productos.buscarParaPedido(
                        idsProductos,
                        request.idRestaurante()
                )
                .stream()
                .collect(Collectors.toMap(
                        ProductoMenu::getIdProducto,
                        Function.identity()
                ));

        if (encontrados.size() != idsProductos.size()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Uno o más productos no pertenecen al restaurante "
                            + "o ya no existen. Actualiza la carta."
            );
        }

        var pedido = new Pedido();

        pedido.setCodigoPedido(
                "TMP" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 17)
        );

        pedido.setRestaurante(restaurante);
        pedido.setIdReserva(seleccion.entrega().idReserva());
        pedido.setIdUsuario(actor.id());
        pedido.setNombreCabana(seleccion.entrega().nombreCabana());
        pedido.setNombreHuesped(seleccion.entrega().nombreHuesped());
        pedido.setMetodoPagoPresencial(
                request.metodoPagoPresencial()
        );
        pedido.setClaveSolicitud(clave);
        pedido.setHuellaSolicitud(huella);
        pedido.setFechaPedido(
                LocalDateTime.now(ZoneId.of("America/Lima"))
        );

        BigDecimal total = BigDecimal.ZERO;

        for (var solicitado : request.detalles()) {
            var producto = encontrados.get(solicitado.idProducto());

            if (!Boolean.TRUE.equals(producto.getDisponible())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El producto "
                                + producto.getNombre()
                                + " ya no está disponible. Actualiza tu pedido."
                );
            }

            BigDecimal precio = producto.getPrecio();

            if (precio == null
                    || precio.signum() <= 0
                    || precio.compareTo(MONTO_MAXIMO) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El producto "
                                + producto.getNombre()
                                + " no tiene un precio válido."
                );
            }

            BigDecimal subtotal = precio.multiply(
                    BigDecimal.valueOf(solicitado.cantidad())
            );

            total = total.add(subtotal);

            if (subtotal.compareTo(MONTO_MAXIMO) > 0
                    || total.compareTo(MONTO_MAXIMO) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El importe del pedido supera el máximo admitido."
                );
            }

            var detalle = new DetallePedido();
            detalle.setProducto(producto);
            detalle.setNombreProducto(producto.getNombre());
            detalle.setCantidad(solicitado.cantidad());
            detalle.setPrecioUnitario(precio);
            detalle.setSubtotal(subtotal);
            detalle.setObservaciones(
                    normalizarObservaciones(solicitado.observaciones())
            );

            pedido.agregarDetalle(detalle);
        }

        pedido.setMontoTotal(total);

        pedidos.saveAndFlush(pedido);

        String codigo = String.format(
                Locale.ROOT,
                "PED%05d",
                pedido.getIdPedido()
        );

        if (codigo.length() > 20) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La numeración de pedidos superó el formato permitido."
            );
        }

        pedido.setCodigoPedido(codigo);
        pedidos.saveAndFlush(pedido);

        return new CreacionPedidoResultado(
                mapper.toResponse(pedido),
                true
        );
    }

    private CreacionPedidoResultado responderExistente(
            Pedido pedido,
            String huella) {

        if (!pedido.getHuellaSolicitud().equals(huella)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Esta clave de solicitud ya fue utilizada "
                            + "para un pedido diferente."
            );
        }

        return new CreacionPedidoResultado(
                mapper.toResponse(pedido),
                false
        );
    }

    private String calcularHuella(CrearPedidoRequest request) {
        try {
            String contenido = json.writeValueAsString(request);

            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(contenido.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(resumen);

        } catch (JsonProcessingException
                 | NoSuchAlgorithmException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo preparar la solicitud del pedido."
            );
        }
    }

    private String normalizarObservaciones(String observaciones) {
        if (observaciones == null || observaciones.isBlank()) {
            return null;
        }

        return observaciones.strip();
    }
}