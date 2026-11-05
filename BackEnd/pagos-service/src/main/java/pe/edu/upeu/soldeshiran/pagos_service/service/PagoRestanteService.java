package pe.edu.upeu.soldeshiran.pagos_service.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.pagos_service.client.ReservaInternaClient;
import pe.edu.upeu.soldeshiran.pagos_service.dto.*;
import pe.edu.upeu.soldeshiran.pagos_service.model.PagoRestante;
import pe.edu.upeu.soldeshiran.pagos_service.repository.PagoRestanteRepository;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PagoRestanteService {
    private final PagoRestanteRepository repository;
    private final ReservaInternaClient reservas;
    @Value("${servicios.interno.token}")
    private String token;

    // Cada saveAndFlush confirma una transacción local antes de invocar otro servicio.
    // Si la respuesta remota se pierde, se conserva el registro y se reintenta su misma referencia.
    public PagoRestante registrar(Long trabajador, Long idReserva, RegistrarPagoRestanteRequest request) {
        String operacion = request.numeroOperacion() == null ? null : request.numeroOperacion().trim();
        if (operacion != null && operacion.isEmpty()) operacion = null;

        PagoRestante pago = repository.findByIdReserva(idReserva).orElse(null);
        if (pago == null) {
            pago = preparar(trabajador, idReserva, request, operacion);
        }
        if (!pago.getMetodoPago().equals(request.metodoPago().name())
                || !Objects.equals(pago.getNumeroOperacion(), operacion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un pago restante con otros datos. Recupera el registro existente antes de continuar.");
        }
        if (!pago.isCompletado()) {
            try {
                reservas.checkIn(idReserva, token, new CheckInRequest("RESTANTE-" + pago.getId(), pago.getMonto()));
            } catch (FeignException e) {
                throw remoto(e);
            }
            pago.setCompletado(true);
            pago = repository.saveAndFlush(pago);
        }
        return pago;
    }

    private PagoRestante preparar(Long trabajador, Long idReserva,
            RegistrarPagoRestanteRequest request, String operacion) {
        SaldoCheckInResponse saldo;
        try {
            saldo = reservas.saldoCheckIn(idReserva, token);
        } catch (FeignException e) {
            // Otra solicitud puede haber completado el check-in mientras consultábamos el saldo.
            PagoRestante concurrente = repository.findByIdReserva(idReserva).orElse(null);
            if (concurrente != null) return concurrente;
            throw remoto(e);
        }
        if (!"CONFIRMADA".equals(saldo.estadoReserva()) || saldo.saldoPendiente().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La reserva debe estar confirmada para registrar el check-in.");
        }
        PagoRestante pago = new PagoRestante();
        pago.setIdReserva(idReserva);
        pago.setIdTrabajador(trabajador);
        pago.setClaveIdempotencia(request.claveIdempotencia().toString());
        pago.setMonto(saldo.saldoPendiente());
        pago.setMetodoPago(request.metodoPago().name());
        pago.setNumeroOperacion(operacion);
        try {
            return repository.saveAndFlush(pago);
        } catch (DataIntegrityViolationException e) {
            // La restricción UNIQUE de reserva arbitra incluso entre distintas instancias de pagos.
            return repository.findByIdReserva(idReserva).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.CONFLICT, "La clave ya identifica otro pago."));
        }
    }

    public PagoRestante consultar(Long idReserva) {
        return repository.findByIdReserva(idReserva).orElse(null);
    }

    private ResponseStatusException remoto(FeignException e) {
        if (e.status() == 404) {
            return new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la reserva o cabaña.");
        }
        if (e.status() == 409 || e.status() == 503) {
            try {
                String detalle = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(e.contentUTF8()).path("detail").asText("");
                if (!detalle.isBlank()) return new ResponseStatusException(HttpStatus.valueOf(e.status()), detalle);
            } catch (java.io.IOException ignored) {
                // Compatibilidad con servicios que todavía no devuelven el detalle estructurado.
            }
            return new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede completar el check-in: verifica fechas, estado y ocupación de la cabaña. El registro existente se conserva.");
        }
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo completar el check-in. Reintenta el mismo registro; no se duplicará el pago.");
    }
}
