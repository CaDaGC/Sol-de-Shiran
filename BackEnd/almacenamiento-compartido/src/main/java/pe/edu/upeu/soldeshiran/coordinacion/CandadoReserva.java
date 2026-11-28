package pe.edu.upeu.soldeshiran.coordinacion;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CandadoReserva {
    private final String url;
    private final String usuario;
    private final String password;

    public CandadoReserva(@Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String usuario,
            @Value("${spring.datasource.password}") String password) {
        this.url = url;
        this.usuario = usuario;
        this.password = password;
    }

    public <T> T ejecutar(Long idReserva, Supplier<T> operacion) {
        if (idReserva == null || idReserva <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La reserva debe ser válida");
        }
        String nombre = "solshiran:reserva:" + idReserva + ":pedidos";
        try (var conexion = DriverManager.getConnection(url, usuario, password)) {
            try (var consulta = conexion.prepareStatement("SELECT GET_LOCK(?, 10)")) {
                consulta.setString(1, nombre);
                try (var resultado = consulta.executeQuery()) {
                    if (!resultado.next() || resultado.getInt(1) != 1 || resultado.wasNull()) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "La reserva tiene otra operación en curso. Inténtalo nuevamente.");
                    }
                }
            }
            try {
                return operacion.get();
            } finally {
                try (var liberar = conexion.prepareStatement("SELECT RELEASE_LOCK(?)")) {
                    liberar.setString(1, nombre);
                    liberar.execute();
                }
            }
        } catch (SQLException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo coordinar la operación de la reserva", exception);
        }
    }
}
