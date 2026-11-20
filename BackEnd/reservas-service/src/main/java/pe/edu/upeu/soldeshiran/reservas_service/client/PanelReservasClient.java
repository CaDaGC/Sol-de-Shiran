package pe.edu.upeu.soldeshiran.reservas_service.client;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import pe.edu.upeu.soldeshiran.reservas_service.security.InternalHttp;
import java.util.List;

@Component
public class PanelReservasClient {
    @Value("${usuarios.url:http://localhost:8087}")
    private String usuariosUrl;

    @Value("${catalogo.url:http://localhost:8086}")
    private String catalogoUrl;

    public List<UsuarioResumen> consultarUsuarios(String key, String buscar, List<Long> ids) {
        return InternalHttp.cliente(usuariosUrl).get()
                .uri(uri -> uri.path("/internal/usuarios/resumen")
                        .queryParamIfPresent("buscar", java.util.Optional.ofNullable(buscar))
                        .queryParamIfPresent("ids", java.util.Optional.ofNullable(ids)).build())
                .header("X-Internal-Key", key).retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<UsuarioResumen>>() {});
    }

    public List<CabanaResumen> consultarCabanas(String key, List<Long> ids) {
        var resultado = new java.util.ArrayList<CabanaResumen>();
        for (int inicio = 0; inicio < ids.size(); inicio += 100) {
            resultado.addAll(consultarLoteCabanas(key, ids.subList(inicio, Math.min(inicio + 100, ids.size()))));
        }
        return resultado;
    }

    private List<CabanaResumen> consultarLoteCabanas(String key, List<Long> ids) {
        return InternalHttp.cliente(catalogoUrl).get()
                .uri(uri -> uri.path("/internal/cabanas/resumen").queryParam("ids", ids).build())
                .header("X-Internal-Key", key).retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<CabanaResumen>>() {});
    }

    public record UsuarioResumen(Long id, String nombre, String email) { }
    public record CabanaResumen(Long idCabana, String nombre) { }
}
