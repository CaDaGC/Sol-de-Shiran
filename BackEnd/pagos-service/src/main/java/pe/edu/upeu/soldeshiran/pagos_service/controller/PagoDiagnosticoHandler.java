package pe.edu.upeu.soldeshiran.pagos_service.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.edu.upeu.soldeshiran.pagos_service.dto.PagoDiagnosticoException;

@RestControllerAdvice(assignableTypes = PagoController.class)
public class PagoDiagnosticoHandler {
    @ExceptionHandler(PagoDiagnosticoException.class)
    public ResponseEntity<Map<String, String>> handle(PagoDiagnosticoException error) {
        return ResponseEntity.status(502).body(Map.of("detail", error.getMessage()));
    }
}