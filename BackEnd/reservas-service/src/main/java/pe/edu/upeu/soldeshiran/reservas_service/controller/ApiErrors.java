package pe.edu.upeu.soldeshiran.reservas_service.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ProblemDetail> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(
            ProblemDetail.forStatusAndDetail(
                e.getStatusCode(),
                e.getReason() == null ? "No se pudo completar la solicitud" : e.getReason()));
  }

  @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> validation(
      org.springframework.web.bind.MethodArgumentNotValidException e) {
    String detalle =
        e.getBindingResult().getFieldErrors().stream()
            .map(x -> x.getField() + ": " + x.getDefaultMessage())
            .collect(java.util.stream.Collectors.joining("; "));
    return ResponseEntity.badRequest()
        .body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detalle));
  }

  @ExceptionHandler({
    org.springframework.dao.DataIntegrityViolationException.class,
    org.springframework.orm.ObjectOptimisticLockingFailureException.class
  })
  public ResponseEntity<ProblemDetail> conflict(Exception e) {
    return ResponseEntity.status(409)
        .body(
            ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "La operaciÃ³n entra en conflicto con un registro existente. Actualiza y vuelve a"
                    + " consultar."));
  }
}
