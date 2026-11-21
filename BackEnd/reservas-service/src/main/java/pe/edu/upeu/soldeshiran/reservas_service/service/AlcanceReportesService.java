package pe.edu.upeu.soldeshiran.reservas_service.service;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient;
@Service @RequiredArgsConstructor
public class AlcanceReportesService {
  private final CatalogoClient catalogo;
  @Value("${seguridad.internal-key}") private String key;
  public List<Long> cabanas(Long dueno) {
    try { return catalogo.listarPropias(dueno,key); }
    catch(feign.FeignException e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo verificar la propiedad de las cabañas.",e);
    }
  }
}
