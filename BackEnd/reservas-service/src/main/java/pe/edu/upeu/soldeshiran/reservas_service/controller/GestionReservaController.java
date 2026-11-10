package pe.edu.upeu.soldeshiran.reservas_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.soldeshiran.reservas_service.dto.CheckoutRequest;
import pe.edu.upeu.soldeshiran.reservas_service.model.*;
import pe.edu.upeu.soldeshiran.reservas_service.security.Actor;
import pe.edu.upeu.soldeshiran.reservas_service.service.GestionReservaService;

@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
public class GestionReservaController {
  private final GestionReservaService gestion;

  @GetMapping
  public List<Reserva> listar(
      HttpServletRequest q,
      @RequestParam(required = false) Long idCabana,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta) {
    return gestion.listar(Actor.desde(q), idCabana, desde, hasta);
  }

  @GetMapping("/{id}")
  public Map<String, Object> obtener(@PathVariable Long id, HttpServletRequest q) {
    return gestion.detalle(id, Actor.desde(q));
  }

  @GetMapping("/panel/{id}/acceso")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void exigirAccesoOperativo(@PathVariable Long id, HttpServletRequest request) {
    gestion.exigirAccesoOperativo(id, Actor.desde(request));
  }

  @GetMapping("/{id}/pagos")
  public List<PagoPresencial> pagos(@PathVariable Long id, HttpServletRequest q) {
    return gestion.pagos(id, Actor.desde(q));
  }

  @PostMapping("/{id}/pagos")
  public Reserva pagar(
      @PathVariable Long id,
      @Valid @RequestBody GestionReservaService.PagoRequest r,
      HttpServletRequest q) {
    return gestion.pagar(id, r, Actor.desde(q));
  }

  @PostMapping("/{id}/check-in")
  public Reserva checkin(@PathVariable Long id, HttpServletRequest q) {
    return gestion.checkin(id, Actor.desde(q));
  }

  @PostMapping("/{id}/check-out")
  public Reserva checkout(
      @PathVariable Long id,
      @Valid @RequestBody(required = false) CheckoutRequest request,
      HttpServletRequest q) {
    return gestion.checkout(id, request, Actor.desde(q));
  }

  @DeleteMapping("/{id}")
  public Reserva cancelar(@PathVariable Long id, HttpServletRequest q) {
    return gestion.cancelar(id, Actor.desde(q));
  }

  @GetMapping("/reportes")
  public Map<String, Object> reporte(HttpServletRequest q) {
    return gestion.reporte(Actor.desde(q));
  }

  @PostMapping("/bloqueos")
  @ResponseStatus(HttpStatus.CREATED)
  public BloqueoFecha bloquear(
      @Valid @RequestBody GestionReservaService.BloqueoRequest r, HttpServletRequest q) {
    return gestion.bloquear(r, Actor.desde(q));
  }

  @GetMapping("/bloqueos")
  public List<BloqueoFecha> bloqueos(@RequestParam Long idCabana, HttpServletRequest q) {
    return gestion.bloqueos(idCabana, Actor.desde(q));
  }

  @DeleteMapping("/bloqueos/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void desbloquear(@PathVariable Long id, HttpServletRequest q) {
    gestion.desbloquear(id, Actor.desde(q));
  }
}
