package pe.edu.upeu.soldeshiran.pagos_service.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pe.edu.upeu.soldeshiran.pagos_service.client.ReservaInternaClient;
import pe.edu.upeu.soldeshiran.pagos_service.dto.*;
import pe.edu.upeu.soldeshiran.pagos_service.model.EstadoTransaccion;
import pe.edu.upeu.soldeshiran.pagos_service.model.TransaccionPago;
import pe.edu.upeu.soldeshiran.pagos_service.repository.TransaccionPagoRepository;
import pe.edu.upeu.soldeshiran.pagos_service.service.PagoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.*;

@Service
public class PagoServiceImpl implements PagoService {
    private static final Logger log = LoggerFactory.getLogger(PagoServiceImpl.class);
    private final TransaccionPagoRepository transaccionRepository;
    private final ReservaInternaClient reservaClient;
    private final RestClient mercadoPago;
    private final ObjectMapper objectMapper;
    @Value("${servicios.interno.token}") private String internalToken;
    @Value("${mercadopago.frontend-url:http://localhost:4200}") private String frontendUrl;

    public PagoServiceImpl(TransaccionPagoRepository repository, ReservaInternaClient client,
            RestClient.Builder builder, ObjectMapper mapper,
            @Value("${mercadopago.access-token}") String accessToken) {
        transaccionRepository = repository;
        reservaClient = client;
        objectMapper = mapper;
        var requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(12000);
        mercadoPago = builder.requestFactory(requestFactory).baseUrl("https://api.mercadopago.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).build();
    }

    @Override
    public PagoResponse procesarPagoBrick(Long usuario, Long reserva, CrearPagoBrickRequest request) {
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Esta aplicación utiliza Checkout Pro con Orders. Inicia el checkout de la reserva.");
    }

    // Se mantiene el endpoint anterior como alias para clientes existentes.
    @Override
    public PreferenciaResponse crearPreferencia(Long usuario, Long idReserva, CrearPreferenciaRequest request) {
        ReservaPagoResponse reserva = validarReserva(usuario, idReserva);
        var pendiente = transaccionRepository.findFirstByIdReservaAndEstadoTransaccionOrderByIdTransaccionDesc(
                idReserva, EstadoTransaccion.PENDIENTE);
        if (pendiente.isPresent()) {
            TransaccionPago anterior = pendiente.get();
            if (anterior.getIdExternoPasarela() == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Hay un intento de resultado incierto. Verifica su estado antes de iniciar otro pago.");
            }
            JsonNode existente = obtenerOrden(anterior.getIdExternoPasarela());
            actualizarOrden(anterior, existente);
            if (anterior.getEstadoTransaccion() == EstadoTransaccion.APROBADA) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva ya tiene un pago aprobado.");
            }
            if (anterior.getEstadoTransaccion() == EstadoTransaccion.PENDIENTE) {
                String checkout = existente.path("checkout_url").asText("");
                if (checkout.isBlank()) throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La orden está pendiente. Consulta su estado antes de volver a pagar.");
                return new PreferenciaResponse(anterior.getIdTransaccion(), anterior.getIdExternoPasarela(),
                        checkout, anterior.getMonto(), anterior.getMoneda());
            }
        }
        TransaccionPago tx = transaccionRepository.saveAndFlush(nuevaTransaccion(reserva));
        String retorno = frontendUrl.replaceAll("/+$", "") + "/pago/resultado?transaccion=" + tx.getIdTransaccion();
        Map<String, Object> body = new HashMap<>();
        body.put("type", "online");
        body.put("processing_mode", "manual");
        body.put("total_amount", tx.getMonto().toPlainString());
        body.put("external_reference", tx.getIdTransaccion().toString());
        body.put("expiration_time", Duration.ofSeconds(Math.max(1,
                Duration.between(LocalDateTime.now(), reserva.expiraEn()).getSeconds())).toString());
        body.put("payer", Map.of("email", request.payerEmail()));
        body.put("items", List.of(Map.of("title", "Adelanto de reserva #" + idReserva,
                "quantity", 1, "unit_price", tx.getMonto().toPlainString(),
                "external_code", "reserva-" + idReserva)));
        body.put("config", Map.of("online", Map.of("success_url", retorno,
                "failure_url", retorno, "pending_url", retorno)));
        JsonNode order;
        try {
            order = mercadoPago.post().uri("/v1/orders")
                    .header("X-Idempotency-Key", "orden-" + tx.getIdTransaccion())
                    .body(body).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException e) {
            tx.setEstadoTransaccion(e.getStatusCode().is4xxClientError()
                    ? EstadoTransaccion.RECHAZADA : EstadoTransaccion.PENDIENTE);
            transaccionRepository.save(tx);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Mercado Pago respondió HTTP " + e.getStatusCode().value() + ": "
                            + detalleRespuestaProveedor(e.getResponseBodyAsString()), e);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo comunicar con Mercado Pago. Consulta el estado antes de reintentar.", e);
        }
        if (order == null || order.path("id").asText("").isBlank()
                || order.path("checkout_url").asText("").isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Mercado Pago no devolvió el checkout de la orden.");
        }
        tx.setIdExternoPasarela(order.path("id").asText());
        transaccionRepository.save(tx);
        return new PreferenciaResponse(tx.getIdTransaccion(), order.path("id").asText(),
                order.path("checkout_url").asText(), tx.getMonto(), tx.getMoneda());
    }

    @Override
    public PagoResponse consultarEstado(Long usuario, Long idTransaccion) {
        // Validar que la transacción exista y pertenezca al usuario
        TransaccionPago tx = transaccionRepository.findById(idTransaccion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la transacción."));
        ReservaPagoResponse reserva = reservaClient.obtenerReserva(tx.getIdReserva(), internalToken);
        if (!Objects.equals(usuario, reserva.idUsuario())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La transacción no pertenece al usuario.");
        }
        if (tx.getIdExternoPasarela() != null && tx.getIdExternoPasarela().startsWith("ORD")) {
            actualizarOrden(tx, obtenerOrden(tx.getIdExternoPasarela()));
        }
        return respuesta(tx, "Estado verificado con Mercado Pago.");
    }

    @Override
    public void procesarNotificacion(String orderId) {
        JsonNode order = obtenerOrden(orderId);
        TransaccionPago tx = transaccionRepository.findByIdExternoPasarela(orderId)
                .orElseGet(() -> {
                    try {
                        return transaccionRepository.findById(Long.parseLong(order.path("external_reference").asText()))
                                .orElseThrow();
                    } catch (Exception e) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Orden sin transacción local.");
                    }
                });
        actualizarOrden(tx, order);
    }

    private JsonNode obtenerOrden(String id) {
        if (!id.matches("ORD[A-Za-z0-9]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Se esperaba un ID de Orders.");
        }
        try {
            JsonNode order = mercadoPago.get().uri("/v1/orders/{id}", id).retrieve().body(JsonNode.class);
            if (order == null) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta vacía de Orders.");
            return order;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar la orden en Mercado Pago.", e);
        }
    }

    private void actualizarOrden(TransaccionPago tx, JsonNode order) {
        String id = order.path("id").asText("");
        String referencia = order.path("external_reference").asText("");
        String moneda = order.path("currency").asText(order.path("currency_id").asText(""));
        BigDecimal monto;
        try { monto = new BigDecimal(order.path("total_amount").asText()); }
        catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Orden sin monto válido."); }
        if (!referencia.equals(tx.getIdTransaccion().toString()) || !tx.getMoneda().equals(moneda)
                || tx.getMonto().compareTo(monto) != 0 || id.isBlank()
                || (tx.getIdExternoPasarela() != null && !id.equals(tx.getIdExternoPasarela()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La orden no coincide con la transacción local.");
        }
        tx.setIdExternoPasarela(id);
        String status = order.path("status").asText();
        String detail = order.path("status_detail").asText();
        if ("processed".equals(status) && "accredited".equals(detail)) {
            BigDecimal pagado;
            try { pagado = new BigDecimal(order.path("total_paid_amount").asText()); }
            catch (NumberFormatException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Orden sin importe pagado."); }
            if (pagado.compareTo(tx.getMonto()) != 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El importe pagado no coincide con la reserva.");
            }
            tx.setEstadoTransaccion(EstadoTransaccion.APROBADA);
            transaccionRepository.saveAndFlush(tx);
            // Confirmación idempotente: si reservas falla, el reenvío del webhook vuelve a intentarlo.
            reservaClient.confirmarPago(tx.getIdReserva(), internalToken, new ConfirmarPagoRequest(id, tx.getMonto()));
        } else if (tx.getEstadoTransaccion() != EstadoTransaccion.APROBADA) {
            tx.setEstadoTransaccion(Set.of("failed", "canceled", "cancelled", "expired").contains(status)
                    ? EstadoTransaccion.RECHAZADA : EstadoTransaccion.PENDIENTE);
            transaccionRepository.save(tx);
        }
    }
    private ReservaPagoResponse validarReserva(Long idUsuario, Long idReserva) {
        ReservaPagoResponse reserva;
        try {
            reserva = reservaClient.obtenerReserva(idReserva, internalToken);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo validar la reserva en ms-reservas."
            );
        }

        if (!Objects.equals(idUsuario, reserva.idUsuario())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "La reserva no pertenece al usuario autenticado."
            );
        }

        if (!"PENDIENTE_PAGO".equals(reserva.estadoReserva())
                || reserva.expiraEn() == null
                || !reserva.expiraEn().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La reserva ya no está pendiente de pago o ha vencido."
            );
        }

        return reserva;
    }

    private TransaccionPago nuevaTransaccion(ReservaPagoResponse reserva) {
        TransaccionPago tx = new TransaccionPago();
        tx.setIdReserva(reserva.idReserva());
        tx.setTipoPago("ONLINE_ADELANTO");
        tx.setMonto(reserva.montoAdelanto());
        tx.setMoneda("PEN");
        tx.setMetodoPago("MERCADO_PAGO_ORDERS");
        tx.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        return tx;
    }

    private PagoResponse respuesta(TransaccionPago tx, String mensaje) {
        return new PagoResponse(
                tx.getIdTransaccion(),
                tx.getIdReserva(),
                tx.getEstadoTransaccion().name(),
                tx.getMonto(),
                tx.getMoneda(),
                tx.getIdExternoPasarela(),
                mensaje
        );
    }

    private String detalleRespuestaProveedor(String body) {
        if (body == null || body.isBlank()) {
            return "Mercado Pago no incluyó detalles en la respuesta.";
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            String message = root.path("message").asText("");
            String error = root.path("error").asText("");
            String causes = root.path("cause").isMissingNode()
                    ? ""
                    : root.path("cause").toString();

            String detalle = String.join("; ",
                    java.util.stream.Stream.of(message, error, causes)
                            .filter(value -> value != null && !value.isBlank())
                            .toList());

            if (detalle.isBlank()) {
                detalle = body;
            }

            return detalle.length() > 1200
                    ? detalle.substring(0, 1200)
                    : detalle;
        } catch (Exception ignored) {
            return body.length() > 1200 ? body.substring(0, 1200) : body;
        }
    }

    private String causaRaiz(Throwable error) {
        Throwable causa = error;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        String mensaje = causa.getMessage();
        return mensaje == null || mensaje.isBlank()
                ? causa.getClass().getSimpleName()
                : causa.getClass().getSimpleName() + ": " + mensaje;
    }
}


