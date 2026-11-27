package pe.edu.upeu.soldeshiran.restaurante_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.restaurante_service.model.TipoCategoria;

@RestControllerAdvice
public class ApiErrors {

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail controlar(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(),
                exception.getReason() == null
                        ? "No se pudo completar la solicitud."
                        : exception.getReason()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail parametroInvalido(
            MethodArgumentTypeMismatchException exception) {

        String mensaje = exception.getRequiredType() == TipoCategoria.class
                ? "El tipo de categoría debe ser COMIDA o BEBIDA."
                : "El parámetro " + exception.getName()
                + " tiene un valor inválido.";

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                mensaje
        );
    }
}