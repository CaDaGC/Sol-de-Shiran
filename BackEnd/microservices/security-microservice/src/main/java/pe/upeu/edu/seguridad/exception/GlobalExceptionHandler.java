package pe.upeu.edu.seguridad.exception;

import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, String>> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class
    })
    ResponseEntity<Map<String, String>> validacion(Exception e) {
        return ResponseEntity.badRequest()
                .body(
                        Map.of(
                                "message",
                                "Revisa los campos. Usa un correo válido y una contraseña de 12 a"
                                        + " 72 caracteres."));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<Map<String, String>> prohibido(Exception e) {
        return ResponseEntity.status(403)
                .body(Map.of("message", "No tienes permiso para esta acción."));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, String>> inesperado(Exception e) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
                .error("Fallo de la API: {}", e.getClass().getSimpleName());
        return ResponseEntity.internalServerError()
                .body(
                        Map.of(
                                "message",
                                "No fue posible completar la operación. Intenta nuevamente."));
    }
}
