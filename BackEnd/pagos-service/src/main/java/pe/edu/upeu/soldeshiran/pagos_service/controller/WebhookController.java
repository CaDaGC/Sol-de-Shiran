package pe.edu.upeu.soldeshiran.pagos_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.soldeshiran.pagos_service.model.WebhookLog;
import pe.edu.upeu.soldeshiran.pagos_service.repository.WebhookLogRepository;
import pe.edu.upeu.soldeshiran.pagos_service.service.PagoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pagos")
@RequiredArgsConstructor
public class WebhookController {
    private final PagoService pagoService;
    private final WebhookLogRepository logRepository;
    private final ObjectMapper objectMapper;

    @Value("${mercadopago.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> recibir(
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId,
            @RequestBody String payload
    ) {
        String id = dataId;

        try {
            if (id == null || id.isBlank()) {
                JsonNode json = objectMapper.readTree(payload);
                id = json.path("data").path("id").asText("");
            }

            if (!firmaValida(signature, requestId, id)) {
                guardarLog(id, payload, "FIRMA_INVALIDA");
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Firma de webhook inválida."
                );
            }

            if (id == null || id.isBlank()) {
                guardarLog(null, payload, "SIN_ID_PAGO");
                return Map.of("mensaje", "Notificación recibida sin ID de pago.");
            }

            guardarLog(id, payload, "RECIBIDO");
            pagoService.procesarNotificacion(id);
            return Map.of("mensaje", "Notificación procesada.");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            guardarLog(id, payload, "ERROR_PROCESAMIENTO");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se pudo procesar el webhook."
            );
        }
    }

    private boolean firmaValida(String signature, String requestId, String dataId) {
        if (signature == null || requestId == null || dataId == null
                || webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }

        Map<String, String> partes = new HashMap<>();
        for (String parte : signature.split(",")) {
            String[] par = parte.trim().split("=", 2);
            if (par.length == 2) {
                partes.put(par[0], par[1]);
            }
        }

        String ts = partes.get("ts");
        String firmaRecibida = partes.get("v1");
        if (ts == null || firmaRecibida == null) {
            return false;
        }

        try {
            String manifiesto = "id:" + dataId
                    + ";request-id:" + requestId
                    + ";ts:" + ts + ";";

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));
            byte[] hash = mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder();
            for (byte b : hash) {
                hexadecimal.append(String.format("%02x", b));
            }

            return MessageDigest.isEqual(
                    hexadecimal.toString().getBytes(StandardCharsets.UTF_8),
                    firmaRecibida.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    private void guardarLog(String id, String payload, String estado) {
        WebhookLog log = new WebhookLog();
        log.setIdTransaccionPasarela(id);
        log.setPayloadJson(payload == null ? "{}" : payload);
        log.setEstadoProcesamiento(estado);
        logRepository.save(log);
    }
}