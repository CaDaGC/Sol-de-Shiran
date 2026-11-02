package pe.edu.upeu.soldeshiran.catalogo_service.mapper;

import org.springframework.stereotype.Component;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaCatalogoResponse;
import pe.edu.upeu.soldeshiran.catalogo_service.model.Cabana;

@Component
public class CabanaMapper {

    public CabanaCatalogoResponse toCatalogoResponse(Cabana cabana) {
        return new CabanaCatalogoResponse(
                cabana.getIdCabana(),
                cabana.getNombre(),
                cabana.getDescripcion(),
                cabana.getAforoMaximo(),
                cabana.getNumHabitaciones(),
                cabana.getNumBanos(),
                cabana.getTarifaNoche(),
                cabana.getImagenPrincipal()
        );
    }
}
