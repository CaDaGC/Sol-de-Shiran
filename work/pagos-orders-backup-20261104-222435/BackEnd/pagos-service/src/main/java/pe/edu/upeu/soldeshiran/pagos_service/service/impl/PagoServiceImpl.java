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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PagoServiceImpl implements PagoService {
    private static final Logger log = LoggerFactory.getLogger(PagoServiceImpl.class);

    private final TransaccionPagoRepository transaccionRepository;
    private final ReservaInternaClient reservaClient;
    private final RestClient mercadoPago;
    private final ObjectMapper objectMapper;

    @Value("${servicios.interno.token}")
    private String internalToken;

    @Value("${mercadopago.webhook-url}")
    private String webhookUrl;

    public PagoServiceImpl(
            TransaccionPagoRepository transaccionRepository,
            ReservaInternaClient reservaClient,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${mercadopago.access-token}") String accessToken
    ) {
        this.transaccionRepository = transaccionRepository;
        this.reservaClient = reservaClient;
        this.objectMapper = objectMapper;
        this.mercadoPago = restClientBuilder
                .baseUrl("https://api.mercadopago.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public PagoResponse procesarPagoBrick(
            Long idUsuario,
            Long idReserva,
            CrearPagoBrickRequest request
    ) {
        ReservaPagoResponse reserva = validarReserva(idUsuario, idReserva);

        TransaccionPago tx = nuevaTransaccion(reserva);
        tx = transaccionRepository.saveAndFlush(tx);

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", reserva.montoAdelanto());
        body.put("token", request.token());
        body.put("payment_method_id", request.paymentMethodId());
        body.put("installments", request.installments() == null ? 1 : request.installments());
        Map<String, Object> payer = new HashMap<>();
        payer.put("email", request.payerEmail());
        payer.put("identification", Map.of(
                "type", request.identificationType(),
                "number", request.identificationNumber()
        ));
        body.put("payer", payer);
        body.put("external_reference", tx.getIdTransaccion().toString());

        if (request.issuerId() != null && !request.issuerId().isBlank()) {
            body.put("issuer_id", request.issuerId());
        }

        JsonNode respuesta;
        try {
            respuesta = mercadoPago.post()
                    .uri("/v1/payments")
                    .header("X-Idempotency-Key", "pago-" + tx.getIdTransaccion())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            String detalle = detalleRespuestaProveedor(e.getResponseBodyAsString());
            int statusProveedor = e.getStatusCode().value();

            // Un 4xx confirma que Mercado Pago rechazó la solicitud. En un 5xx
            // el resultado puede ser incierto, así que se conserva PENDIENTE.
            tx.setEstadoTransaccion(statusProveedor >= 400 && statusProveedor < 500
                    ? EstadoTransaccion.RECHAZADA
                    : EstadoTransaccion.PENDIENTE);
            transaccionRepository.save(tx);

            log.error("Mercado Pago respondió HTTP {} al crear el pago de la transacción {}: {}",
                    statusProveedor, tx.getIdTransaccion(), detalle, e);
            HttpStatus statusAplicacion = statusProveedor == 400 || statusProveedor == 422
                    ? HttpStatus.UNPROCESSABLE_ENTITY
                    : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(
                    statusAplicacion,
                    "Mercado Pago respondió HTTP " + statusProveedor + ": " + detalle,
                    e
            );
        } catch (RestClientException e) {
            // Si se pierde la conexión no se sabe si el proveedor alcanzó a
            // crear el pago. No marcar RECHAZADA ni descartar un posible ID.
            tx.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
            transaccionRepository.save(tx);
            String detalle = causaRaiz(e);
            log.error("Fallo de comunicación con Mercado Pago al crear la transacción {}: {}",
                    tx.getIdTransaccion(), detalle, e);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo completar la comunicación con Mercado Pago: " + detalle,
                    e
            );
        }

        if (respuesta == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Mercado Pago devolvió una respuesta vacía."
            );
        }

        aplicarRespuestaMercadoPago(tx, respuesta);

        return respuesta(tx, "Respuesta recibida de Mercado Pago.");
    }

    @Override
    public PreferenciaResponse crearPreferencia(
            Long idUsuario,
            Long idReserva,
            CrearPreferenciaRequest request
    ) {
        ReservaPagoResponse reserva = validarReserva(idUsuario, idReserva);

        TransaccionPago tx = transaccionRepository.saveAndFlush(nuevaTransaccion(reserva));

        Map<String, Object> item = Map.of(
                "title", "Adelanto de reserva #" + idReserva,
                "quantity", 1,
                "currency_id", "PEN",
                "unit_price", reserva.montoAdelanto()
        );

        Map<String, Object> payer = Map.of("email", request.payerEmail());

        Map<String, Object> body = new HashMap<>();
        body.put("items", List.of(item));
        body.put("payer", payer);
        body.put("external_reference", tx.getIdTransaccion().toString());
        body.put("notification_url", webhookUrl);

        JsonNode respuesta;
        try {
            respuesta = mercadoPago.post()
                    .uri("/checkout/preferences")
                    .header("X-Idempotency-Key", "preferencia-" + tx.getIdTransaccion())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            String detalle = detalleRespuestaProveedor(e.getResponseBodyAsString());
            int statusProveedor = e.getStatusCode().value();
            tx.setEstadoTransaccion(statusProveedor >= 400 && statusProveedor < 500
                    ? EstadoTransaccion.RECHAZADA
                    : EstadoTransaccion.PENDIENTE);
            transaccionRepository.save(tx);
            log.error("Mercado Pago respondió HTTP {} al crear la preferencia para transacción {}: {}",
                    statusProveedor, tx.getIdTransaccion(), detalle, e);
            HttpStatus statusAplicacion = statusProveedor == 400 || statusProveedor == 422
                    ? HttpStatus.UNPROCESSABLE_ENTITY
                    : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(
                    statusAplicacion,
                    "Mercado Pago respondió HTTP " + statusProveedor + " al crear la preferencia: " + detalle,
                    e
            );
        } catch (RestClientException e) {
            tx.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
            transaccionRepository.save(tx);
            String detalle = causaRaiz(e);
            log.error("Fallo de comunicación con Mercado Pago al crear preferencia {}: {}",
                    tx.getIdTransaccion(), detalle, e);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo conectar con Mercado Pago al crear la preferencia: " + detalle,
                    e
            );
        }

        if (respuesta == null || respuesta.path("id").isMissingNode()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Mercado Pago no devolvió el ID de la preferencia."
            );
        }

        return new PreferenciaResponse(
                tx.getIdTransaccion(),
                respuesta.path("id").asText(),
                respuesta.path("sandbox_init_point").asText(""),
                tx.getMonto(),
                tx.getMoneda()
        );
    }

    @Override
    public void procesarNotificacion(String paymentId) {
        JsonNode pago;
        try {
            pago = mercadoPago.get()
                    .uri("/v1/payments/{id}", paymentId)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException e) {
            String detalle = detalleRespuestaProveedor(e.getResponseBodyAsString());
            log.error("Mercado Pago respondió HTTP {} al consultar el pago {}: {}",
                    e.getStatusCode().value(), paymentId, detalle, e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Mercado Pago respondió HTTP " + e.getStatusCode().value()
                            + " al consultar el pago: " + detalle,
                    e
            );
        } catch (RestClientException e) {
            String detalle = causaRaiz(e);
            log.error("Fallo de comunicación al consultar el pago {}: {}", paymentId, detalle, e);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo conectar con Mercado Pago al consultar el pago: " + detalle,
                    e
            );
        }

        if (pago == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Mercado Pago devolvió una respuesta vacía."
            );
        }

        String referencia = pago.path("external_reference").asText("");
        TransaccionPago tx = transaccionRepository.findByIdExternoPasarela(paymentId)
                .orElseGet(() -> {
                    try {
                        return transaccionRepository.findById(Long.parseLong(referencia))
                                .orElseThrow();
                    } catch (Exception e) {
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "No se encontró la transacción asociada al pago."
                        );
                    }
                });

        BigDecimal monto = pago.path("transaction_amount").decimalValue();
        String moneda = pago.path("currency_id").asText("");

        if (tx.getMonto().compareTo(monto) != 0 || !"PEN".equals(moneda)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El monto o la moneda del pago no coincide con la transacción."
            );
        }

        aplicarRespuestaMercadoPago(tx, pago);
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
        tx.setMetodoPago("MERCADO_PAGO_BRICK");
        tx.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        return tx;
    }

    private void aplicarRespuestaMercadoPago(TransaccionPago tx, JsonNode pago) {
        String id = pago.path("id").asText("");
        String estado = pago.path("status").asText("");

        if (!id.isBlank()) {
            tx.setIdExternoPasarela(id);
        }

        if ("approved".equalsIgnoreCase(estado)) {
            tx.setEstadoTransaccion(EstadoTransaccion.APROBADA);
            transaccionRepository.save(tx);
            reservaClient.confirmarPago(
                    tx.getIdReserva(),
                    internalToken,
                    new ConfirmarPagoRequest(id, tx.getMonto())
            );
            return;
        }

        if ("rejected".equalsIgnoreCase(estado)
                || "cancelled".equalsIgnoreCase(estado)) {
            tx.setEstadoTransaccion(EstadoTransaccion.RECHAZADA);
        } else {
            tx.setEstadoTransaccion(EstadoTransaccion.PENDIENTE);
        }

        transaccionRepository.save(tx);
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
