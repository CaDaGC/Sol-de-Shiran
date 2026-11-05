package pe.edu.upeu.soldeshiran.pagos_service.controller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import pe.edu.upeu.soldeshiran.pagos_service.service.PagoRestanteService;
import pe.edu.upeu.soldeshiran.pagos_service.model.PagoRestante;
import pe.edu.upeu.soldeshiran.pagos_service.dto.RegistrarPagoRestanteRequest;
@RestController @Validated @RequiredArgsConstructor
@RequestMapping("/api/v1/pagos/reservas/{idReserva}/restante")
public class PagoRestanteController {
 private final PagoRestanteService service;
 private void autorizar(String rol) {
  if(!"GUARDIAN".equals(rol)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el guardián puede registrar el check-in.");
 }
 @PostMapping public PagoRestante registrar(@RequestHeader("X-User-Role") String rol,
   @RequestHeader("X-User-Id") @Positive Long trabajador, @PathVariable @Positive Long idReserva,
   @Valid @RequestBody RegistrarPagoRestanteRequest request) {
  autorizar(rol); return service.registrar(trabajador,idReserva,request);
 }
 @GetMapping public PagoRestante consultar(@RequestHeader("X-User-Role") String rol, @PathVariable @Positive Long idReserva) {
  autorizar(rol); return service.consultar(idReserva);
 }
}
