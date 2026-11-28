package pe.edu.upeu.soldeshiran.restaurante_service.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.restaurante_service.repository.RestauranteTrabajadorRepository;

@Component
@RequiredArgsConstructor
public class AccesoRestaurante {

    private final RestauranteTrabajadorRepository asignaciones;

    public void exigir(Long idRestaurante, Actor actor) {
        actor.exigir("ADMIN", "RESTAURANTE");

        if (actor.es("ADMIN")) {
            return;
        }

        var asignacion = asignaciones.findByIdUsuario(actor.id())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Tu cuenta no tiene un restaurante asignado"
                ));

        if (!asignacion.getRestaurante()
                .getIdRestaurante()
                .equals(idRestaurante)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo puedes administrar tu restaurante asignado"
            );
        }
    }
}