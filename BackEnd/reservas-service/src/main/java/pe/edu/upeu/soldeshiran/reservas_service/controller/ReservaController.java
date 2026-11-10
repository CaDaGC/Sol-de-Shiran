package pe.edu.upeu.soldeshiran.reservas_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.soldeshiran.reservas_service.dto.CotizacionReservaResponse;
import pe.edu.upeu.soldeshiran.reservas_service.dto.CrearReservaRequest;
import pe.edu.upeu.soldeshiran.reservas_service.dto.FechasOcupadasResponse;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservaResponse;
import pe.edu.upeu.soldeshiran.reservas_service.service.ReservaService;
import pe.edu.upeu.soldeshiran.reservas_service.service.ReservaConsultaService;
import pe.edu.upeu.soldeshiran.reservas_service.security.Actor;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservaFiltroRequest;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservaDetalleResponse;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservasPaginaResponse;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
@Validated
public class ReservaController {

  private final ReservaService reservaService;
  private final ReservaConsultaService reservaConsultaService;

  @GetMapping("/panel/cabanas")
  public java.util.List<pe.edu.upeu.soldeshiran.reservas_service.client.PanelReservasClient.CabanaResumen>
      consultarCabanasDelPanel(HttpServletRequest request) {
    return reservaConsultaService.consultarCabanas(Actor.desde(request));
  }

  @GetMapping("/panel/{idReserva}")
  public ReservaDetalleResponse consultarDetalle(
      HttpServletRequest request, @PathVariable @Positive Long idReserva) {
    return reservaConsultaService.consultarDetalle(Actor.desde(request), idReserva);
  }

  @GetMapping("/panel")
  public ReservasPaginaResponse consultarReservas(
      HttpServletRequest request, @Valid @ModelAttribute ReservaFiltroRequest filtro) {
    return reservaConsultaService.consultar(Actor.desde(request), filtro.buscar(), filtro.idCabana(),
        filtro.page(), filtro.size());
  }

  @GetMapping("/cabanas/{idCabana}/disponibilidad")
  public ResponseEntity<CotizacionReservaResponse> consultarDisponibilidad(
      @PathVariable @Positive Long idCabana,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaIngreso,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaSalida,
      @RequestParam @Min(1) Integer cantidadHuespedes) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(
            reservaService.consultarDisponibilidad(
                idCabana, fechaIngreso, fechaSalida, cantidadHuespedes));
  }

  @GetMapping("/cabanas/{idCabana}/fechas-ocupadas")
  public ResponseEntity<FechasOcupadasResponse> consultarFechasOcupadas(
      @PathVariable @Positive Long idCabana,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(reservaService.consultarFechasOcupadas(idCabana, desde, hasta));
  }

  @PostMapping
  public ResponseEntity<ReservaResponse> crearReservaTemporal(
      jakarta.servlet.http.HttpServletRequest servletRequest,
      @Valid @RequestBody CrearReservaRequest request) {
    Actor actor = Actor.desde(servletRequest);
    actor.exigir("ADMIN", "CLIENTE");
    ReservaResponse reserva = reservaService.crearReservaTemporal(actor.id(), request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .contentType(MediaType.APPLICATION_JSON)
        .body(reserva);
  }
}
