package pe.edu.upeu.soldeshiran.restaurante_service.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.almacenamiento.AlmacenamientoService;
import pe.edu.upeu.soldeshiran.almacenamiento.CategoriaArchivo;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.EstadoPedidoRequest;
import pe.edu.upeu.soldeshiran.restaurante_service.dto.PedidoResponse;
import pe.edu.upeu.soldeshiran.restaurante_service.mapper.PedidoMapper;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoPagoPedido;
import pe.edu.upeu.soldeshiran.restaurante_service.model.EstadoPedido;
import pe.edu.upeu.soldeshiran.restaurante_service.model.Pedido;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.PedidoRepository;
import pe.edu.upeu.soldeshiran.restaurante_service.security.AccesoRestaurante;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.AtencionPedidoService;
import pe.edu.upeu.soldeshiran.restaurante_service.service.ComandaPdf;

@Service
@RequiredArgsConstructor
@Transactional
public class AtencionPedidoServiceImpl implements AtencionPedidoService {

    private final PedidoRepository pedidos;
    private final PedidoMapper mapper;
    private final AccesoRestaurante acceso;
    private final ComandaPdf pdf;
    private final AlmacenamientoService archivos;

    @Override
    @Transactional(readOnly = true)
    public PedidoResponse consultar(
            Long idRestaurante,
            Long idPedido,
            Actor actor) {

        validarIds(idRestaurante, idPedido);
        acceso.exigir(idRestaurante, actor);

        return mapper.toResponse(buscar(idRestaurante, idPedido));
    }

    @Override
    public PedidoResponse cambiarEstado(
            Long idRestaurante,
            Long idPedido,
            EstadoPedidoRequest request,
            Actor actor) {

        validarIds(idRestaurante, idPedido);
        acceso.exigir(idRestaurante, actor);

        var pedido = pedidos.bloquearPorId(idPedido)
                .orElseThrow(this::noEncontrado);

        if (!pedido.getRestaurante()
                .getIdRestaurante()
                .equals(idRestaurante)) {

            throw noEncontrado();
        }

        var destino = request.estado();
        var motivo = normalizar(request.motivo());

        if (destino == EstadoPedido.RECHAZADO && motivo == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Indica el motivo del rechazo"
            );
        }

        if (destino != EstadoPedido.RECHAZADO && motivo != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El motivo solo corresponde al rechazo del pedido"
            );
        }

        if (pedido.getEstadoPedido() == destino) {
            if (destino == EstadoPedido.RECHAZADO
                    && !Objects.equals(
                    pedido.getMotivoRechazo(),
                    motivo
            )) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El pedido ya fue rechazado con otro motivo"
                );
            }

            return mapper.toResponse(pedido);
        }

        boolean permitido = switch (pedido.getEstadoPedido()) {
            case NUEVO ->
                    destino == EstadoPedido.EN_PREPARACION
                            || destino == EstadoPedido.RECHAZADO;
            case EN_PREPARACION ->
                    destino == EstadoPedido.EN_ENTREGA;
            case EN_ENTREGA ->
                    destino == EstadoPedido.FINALIZADO;
            case FINALIZADO, RECHAZADO -> false;
        };

        if (!permitido) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se permite cambiar de "
                            + pedido.getEstadoPedido()
                            + " a "
                            + destino
            );
        }

        if (pedido.getEstadoPago() != EstadoPagoPedido.PENDIENTE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pedido no tiene un pago pendiente de cobro"
            );
        }

        var ahora = LocalDateTime.now(ZoneId.of("America/Lima"));

        if (destino == EstadoPedido.EN_PREPARACION) {
            pedido.setFechaAceptacion(ahora);

            try {
                byte[] contenido = pdf.generar(pedido);

                String referencia = archivos.guardar(
                        CategoriaArchivo.COMANDA,
                        pedido.getCodigoPedido()
                                + "-"
                                + UUID.randomUUID()
                                + ".pdf",
                        contenido,
                        "application/pdf"
                );

                pedido.setComandaReferencia(referencia);
            } catch (IOException exception) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo generar la comanda",
                        exception
                );
            }
        }

        if (destino == EstadoPedido.RECHAZADO) {
            pedido.setMotivoRechazo(motivo);
        }

        if (destino == EstadoPedido.FINALIZADO) {
            pedido.setEstadoPago(EstadoPagoPedido.PAGADO);
            pedido.setFechaEntrega(ahora);
            pedido.setIdUsuarioRestaurante(actor.id());
        }

        pedido.setEstadoPedido(destino);
        pedidos.saveAndFlush(pedido);

        return mapper.toResponse(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] descargarComanda(
            Long idRestaurante,
            Long idPedido,
            Actor actor) {

        validarIds(idRestaurante, idPedido);
        acceso.exigir(idRestaurante, actor);

        var pedido = buscar(idRestaurante, idPedido);

        if (pedido.getComandaReferencia() == null
                || pedido.getComandaReferencia().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pedido todavía no tiene una comanda generada"
            );
        }

        return archivos.leer(
                CategoriaArchivo.COMANDA,
                pedido.getComandaReferencia()
        );
    }

    private Pedido buscar(Long idRestaurante, Long idPedido) {
        return pedidos.findByIdPedidoAndRestauranteIdRestaurante(
                idPedido,
                idRestaurante
        ).orElseThrow(this::noEncontrado);
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank()
                ? null
                : valor.strip();
    }

    private void validarIds(Long idRestaurante, Long idPedido) {
        if (idRestaurante == null || idRestaurante <= 0
                || idPedido == null || idPedido <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los identificadores deben ser positivos"
            );
        }
    }

    private ResponseStatusException noEncontrado() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No se encontró el pedido en este restaurante"
        );
    }
}