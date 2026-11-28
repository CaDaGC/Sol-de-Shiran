package pe.edu.upeu.soldeshiran.restaurante_service.service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HorarioRestauranteService {

    @Value("${restaurante.horario.apertura:}")
    private String aperturaConfigurada;

    @Value("${restaurante.horario.cierre:}")
    private String cierreConfigurado;

    public void exigirHorarioDePedidos() {
        LocalTime apertura;
        LocalTime cierre;

        try {
            apertura = LocalTime.parse(aperturaConfigurada.trim());
            cierre = LocalTime.parse(cierreConfigurado.trim());
        } catch (DateTimeParseException exception) {
            throw sinConfiguracion();
        }

        if (apertura.equals(cierre)) {
            throw sinConfiguracion();
        }

        LocalTime ahora = LocalTime.now(ZoneId.of("America/Lima"));

        boolean permitido;

        if (apertura.isBefore(cierre)) {
            permitido = !ahora.isBefore(apertura)
                    && ahora.isBefore(cierre);
        } else {
            permitido = !ahora.isBefore(apertura)
                    || ahora.isBefore(cierre);
        }

        if (!permitido) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Los restaurantes no están recibiendo pedidos. "
                            + "Horario de atención: "
                            + apertura
                            + " a "
                            + cierre
                            + ", hora de Perú."
            );
        }
    }

    private ResponseStatusException sinConfiguracion() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "El horario común de los restaurantes todavía no está configurado."
        );
    }

    public String describirHorario() {
        try {
            var apertura = LocalTime.parse(aperturaConfigurada.trim());
            var cierre = LocalTime.parse(cierreConfigurado.trim());

            if (apertura.equals(cierre)) {
                throw sinConfiguracion();
            }

            return apertura + " a " + cierre + " (hora de Perú)";
        } catch (DateTimeParseException exception) {
            throw sinConfiguracion();
        }
    }
}