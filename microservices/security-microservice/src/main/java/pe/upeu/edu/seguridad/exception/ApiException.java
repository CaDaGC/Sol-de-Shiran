package pe.upeu.edu.seguridad.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException acceso() {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "Correo o contraseña incorrectos, o cuenta temporalmente no disponible.");
    }

    public static ApiException invalido(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
