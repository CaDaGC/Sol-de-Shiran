package pe.edu.upeu.soldeshiran.reservas_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = ReservaPagoInternoController.class)
public class ReservaInternaErrorHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> manejar(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("detail",
                error.getReason() == null ? "No se pudo completar la operación de reserva." : error.getReason()));
    }
}
