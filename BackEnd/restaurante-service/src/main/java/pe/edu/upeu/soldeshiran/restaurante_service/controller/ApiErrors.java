package pe.edu.upeu.soldeshiran.restaurante_service.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.restaurante_service.model.TipoCategoria;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ProblemDetail imagenGrande() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen debe pesar como máximo 2 MB");
    }


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
                : "El parámetro "
                + exception.getName()
                + " tiene un valor inválido.";

        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                mensaje
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(
            MethodArgumentNotValidException exception) {

        var errores = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error ->
                        error.getField() + ": " + error.getDefaultMessage()
                )
                .distinct()
                .toList();

        var respuesta = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Revisa los campos de la solicitud."
        );

        respuesta.setProperty("errores", errores);
        return respuesta;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail cuerpoInvalido() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "El contenido de la solicitud no es válido. "
                        + "Revisa los tipos de datos y los valores permitidos."
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflictoDeDatos() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "No se pudo registrar la operación porque los datos "
                        + "cambiaron o la solicitud ya fue procesada. "
                        + "Si reintentas un pedido, conserva su clave de solicitud."
        );
    }
}