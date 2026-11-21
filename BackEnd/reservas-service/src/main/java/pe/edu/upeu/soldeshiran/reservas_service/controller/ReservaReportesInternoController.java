package pe.edu.upeu.soldeshiran.reservas_service.controller;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;
import pe.edu.upeu.soldeshiran.reservas_service.service.AlcanceReportesService;
@RestController @RequiredArgsConstructor
@RequestMapping("/internal/reservas/reportes")
public class ReservaReportesInternoController {
  private final ReservaRepository reservas;
  private final AlcanceReportesService alcance;
  @GetMapping("/duenos/{id}/reservas")
  public List<Long> propias(@PathVariable Long id) {
    var cabanas=alcance.cabanas(id);
    return cabanas.isEmpty() ? List.of() : reservas.listarIdsDeCabanas(cabanas);
  }
}
