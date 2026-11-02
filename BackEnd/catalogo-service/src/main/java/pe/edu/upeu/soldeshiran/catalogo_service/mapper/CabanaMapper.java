package pe.edu.upeu.soldeshiran.catalogo_service.mapper;

import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaCatalogoResponse;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaDetalleResponse;
import pe.edu.upeu.soldeshiran.catalogo_service.model.Cabana;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public CabanaDetalleResponse toDetalleResponse(Cabana cabana) {
        List<String> imagenesSecundarias = cabana.getImagenes()
                .stream()
                .map(imagen -> imagen.getUrlImagen())
                .toList();

        return new CabanaDetalleResponse(
                cabana.getIdCabana(),
                cabana.getNombre(),
                cabana.getDescripcion(),
                cabana.getAforoMaximo(),
                cabana.getNumHabitaciones(),
                cabana.getNumBanos(),
                cabana.getTarifaNoche(),
                cabana.getImagenPrincipal(),
                imagenesSecundarias
        );
    }
}