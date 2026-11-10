package pe.edu.upeu.soldeshiran.reservas_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.reservas_service.dto.CheckoutRequest;
import pe.edu.upeu.soldeshiran.reservas_service.model.*;
import pe.edu.upeu.soldeshiran.reservas_service.repository.*;
import pe.edu.upeu.soldeshiran.reservas_service.security.Actor;

@Service
@Transactional
@RequiredArgsConstructor
public class GestionReservaService {
  private final ReservaRepository reservas;
  private final PagoPresencialRepository pagos;
  private final BloqueoFechaRepository bloqueos;
  private final pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient catalogo;

  @Value("${servicios.interno.token}")
  private String tokenOcupacion;

  @Value("${catalogo.url:http://localhost:8086}")
  private String catalogoUrl;

  @Value("${usuarios.url:http://localhost:8087}")
  private String usuariosUrl;

  @Value("${seguridad.internal-key:sol-shiran-interno-desarrollo-cambiar}")
  private String key;

  public record PagoRequest(
      @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal monto,
      @NotBlank String medio,
      @Size(max = 100) String referencia,
      @NotBlank @Size(max = 80) String claveOperacion) {}

  public record BloqueoRequest(
      @NotNull @Positive Long idCabana,
      @NotNull LocalDate desde,
      @NotNull LocalDate hasta,
      @NotBlank @Size(max = 200) String motivo) {}

  private JsonNode acceso(Long id, Actor a) {
    try {
      return pe.edu.upeu.soldeshiran.reservas_service.security.InternalHttp.cliente(catalogoUrl)
          .get()
          .uri(
              b ->
                  b.path("/internal/cabanas/" + id + "/acceso")
                      .queryParam("usuarioId", a.id())
                      .queryParam("rol", a.rol())
                      .build())
          .header("X-Internal-Key", key)
          .retrieve()
          .body(JsonNode.class);
    } catch (org.springframework.web.client.RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 404) return null;
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudo validar la cabaña");
    } catch (org.springframework.web.client.ResourceAccessException ex) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Catálogo no disponible");
    }
  }

  boolean operativo(Long id, Actor a) {
    if (a.es("ADMIN")) return true;
    if (!a.es("DUENO", "TRABAJADOR")) return false;
    var x = acceso(id, a);
    return x != null && x.path("operativo").asBoolean();
  }

  private boolean propietario(Long id, Actor a) {
    if (a.es("ADMIN")) return true;
    if (!a.es("DUENO")) return false;
    var x = acceso(id, a);
    return x != null && x.path("propietario").asBoolean();
  }

  void exigirOperativo(Reserva r, Actor a) {
    if (!operativo(r.getIdCabana(), a)) prohibido();
  }

  private void exigirVer(Reserva r, Actor a) {
    if (!Objects.equals(r.getIdUsuario(), a.id()) && !operativo(r.getIdCabana(), a)) prohibido();
  }

  private Reserva find(Long id, boolean lock) {
    return (lock ? reservas.bloquearPorId(id) : reservas.findById(id))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
  }

  public List<Reserva> listar(Actor a, Long cabana, LocalDate desde, LocalDate hasta) {
    Map<Long, Boolean> scopes = new HashMap<>();
    return reservas.findAll().stream()
        .filter(r -> cabana == null || cabana.equals(r.getIdCabana()))
        .filter(r -> desde == null || !r.getFechaSalida().isBefore(desde))
        .filter(r -> hasta == null || !r.getFechaIngreso().isAfter(hasta))
        .filter(
            r ->
                Objects.equals(r.getIdUsuario(), a.id())
                    || scopes.computeIfAbsent(r.getIdCabana(), id -> operativo(id, a)))
        .toList();
  }

  public Reserva obtener(Long id, Actor a) {
    Reserva r = find(id, false);
    exigirVer(r, a);
    return r;
  }

  public Map<String, Object> detalle(Long id, Actor a) {
    Reserva r = obtener(id, a);
    JsonNode huesped = null;
    try {
      huesped =
          pe.edu.upeu.soldeshiran.reservas_service.security.InternalHttp.cliente(usuariosUrl)
              .get()
              .uri("/internal/usuarios/" + r.getIdUsuario() + "/perfil")
              .header("X-Internal-Key", key)
              .retrieve()
              .body(JsonNode.class);
    } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
      // La reserva conserva su ID de huésped aunque la cuenta se haya eliminado.
    } catch (org.springframework.web.client.RestClientException e) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar el perfil del huésped");
    }
    Map<String, Object> datos =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .convertValue(
                r, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
    datos.put("huesped", huesped);
    return datos;
  }

  public void exigirAccesoOperativo(Long id, Actor actor) {
    exigirOperativo(find(id, false), actor);
  }

  public List<PagoPresencial> pagos(Long id, Actor a) {
    obtener(id, a);
    return pagos.findByReservaId(id);
  }

  public Reserva pagar(Long id, PagoRequest p, Actor a) {
    Reserva r = find(id, true);
    exigirOperativo(r, a);
    var anterior = pagos.findByClaveOperacion(p.claveOperacion());
    if (anterior.isPresent()) {
      PagoPresencial x = anterior.get();
      if (!Objects.equals(x.getReservaId(), id)
          || x.getMonto().compareTo(p.monto()) != 0
          || !Objects.equals(x.getMedio(), p.medio())
          || !Objects.equals(x.getReferencia(), p.referencia()))
        conflicto("Clave de operación utilizada con otros datos");
      return r;
    }
    if (Boolean.TRUE.equals(r.getAdelantoEnLinea()))
      conflicto("Usa el pago en línea para el adelanto y el panel de reservas para el saldo.");

    if (!List.of("EFECTIVO", "TARJETA", "YAPE", "PLIN").contains(p.medio()))
      malo("Medio de pago inválido");
    if (!p.medio().equals("EFECTIVO") && (p.referencia() == null || p.referencia().isBlank()))
      malo("Referencia obligatoria");
    if (!List.of(EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA)
        .contains(r.getEstadoReserva())) conflicto("Estado no admite pagos");
    if (r.getEstadoReserva() == EstadoReserva.PENDIENTE_PAGO
        && !r.getExpiraEn().isAfter(LocalDateTime.now())) conflicto("Reserva temporal vencida");
    // Existing confirmed reservations already have their advance paid.
    BigDecimal pagado = r.getTotalPagado() == null ? BigDecimal.ZERO : r.getTotalPagado();
    if (r.getEstadoReserva() == EstadoReserva.CONFIRMADA
        && pagado.compareTo(r.getMontoAdelanto()) < 0) pagado = r.getMontoAdelanto();
    if (p.monto().compareTo(r.getMontoTotal().subtract(pagado)) > 0)
      malo("El monto supera el saldo real");
    PagoPresencial registro = new PagoPresencial();
    registro.setReservaId(id);
    registro.setRegistradoPor(a.id());
    registro.setMonto(p.monto());
    registro.setMedio(p.medio());
    registro.setReferencia(p.referencia());
    registro.setClaveOperacion(p.claveOperacion());
    pagos.saveAndFlush(registro);
    pagado = pagado.add(p.monto());
    r.setTotalPagado(pagado);
    r.setSaldoPendiente(r.getMontoTotal().subtract(pagado));
    if (pagado.compareTo(r.getMontoAdelanto()) >= 0) r.setEstadoReserva(EstadoReserva.CONFIRMADA);
    return r;
  }

  public Reserva checkin(Long id, Actor a) {
    Reserva r = find(id, true);
    exigirOperativo(r, a);
    if (r.getEstadoReserva() != EstadoReserva.CONFIRMADA
        || r.getSaldoPendiente().compareTo(BigDecimal.ZERO) != 0)
      conflicto("Debe estar confirmada y sin saldo pendiente");
    LocalDate hoy = LocalDate.now();
    if (hoy.isBefore(r.getFechaIngreso()) || !hoy.isBefore(r.getFechaSalida()))
      conflicto("El ingreso debe estar dentro de la estancia");
    actualizarOcupacion(r, true);
    r.setEstadoReserva(EstadoReserva.OCUPADA);
    return r;
  }

  public Reserva checkout(Long id, CheckoutRequest request, Actor a) {
    Reserva r = find(id, true);
    exigirOperativo(r, a);

    var danos = request == null ? null : request.danos();
    String descripcion = danos == null ? null : danos.descripcion().strip();
    BigDecimal monto = danos == null ? null : danos.monto().setScale(2);
    String medio = danos == null ? null : danos.medioPago();
    RegistroCheckout anterior = r.getCheckout();
    if (r.getEstadoReserva() == EstadoReserva.FINALIZADA && anterior != null) {
      if (Objects.equals(anterior.descripcionDanos(), descripcion)
          && Objects.equals(anterior.montoDanos(), monto)
          && Objects.equals(anterior.medioPago(), medio)) {
        return r;
      }
      conflicto("El check-out ya se registró con otros datos");
    }
    if (r.getEstadoReserva() != EstadoReserva.OCUPADA) conflicto("La reserva no está ocupada");

    r.setCheckout(new RegistroCheckout(
        LocalDateTime.now(ZoneId.of("America/Lima")).truncatedTo(ChronoUnit.SECONDS),
        a.id(), descripcion, monto, medio));
    r.setEstadoReserva(EstadoReserva.FINALIZADA);
    // Si catálogo falla, la transacción revierte el registro y permite reintentar.
    reservas.saveAndFlush(r);
    actualizarOcupacion(r, false);
    return r;
  }

  private void actualizarOcupacion(Reserva reserva, boolean ocupar) {
    try {
      if (ocupar) {
        catalogo.ocupar(reserva.getIdCabana(), reserva.getIdReserva(), tokenOcupacion);
      } else {
        catalogo.liberar(reserva.getIdCabana(), reserva.getIdReserva(), tokenOcupacion);
      }
    } catch (feign.FeignException exception) {
      if (exception.status() == 409) conflicto("La cabaña ya está ocupada");
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudo actualizar la ocupación de la cabaña", exception);
    }
  }

  public Reserva cancelar(Long id, Actor a) {
    a.exigir("ADMIN", "DUENO", "USUARIO");
    Reserva r = find(id, true);
    boolean gestion = propietario(r.getIdCabana(), a);
    if (!gestion && !Objects.equals(r.getIdUsuario(), a.id())) prohibido();
    if (!List.of(EstadoReserva.PENDIENTE_PAGO, EstadoReserva.CONFIRMADA)
        .contains(r.getEstadoReserva())) conflicto("No se puede cancelar este estado");
    if (!gestion && !LocalDate.now().isBefore(r.getFechaIngreso()))
      conflicto("La cancelación personal debe ser anterior al ingreso");
    if (r.getEstadoReserva() == EstadoReserva.PENDIENTE_PAGO
        && Boolean.TRUE.equals(r.getAdelantoEnLinea())
        && r.getExpiraEn().isAfter(LocalDateTime.now()))
      conflicto("Espera a que termine el intento de pago antes de cancelar.");
    r.setEstadoReserva(EstadoReserva.CANCELADA);
    return r;
  }

  public BloqueoFecha bloquear(BloqueoRequest b, Actor a) {
    if (!propietario(b.idCabana(), a)) prohibido();
    if (b.hasta().isBefore(b.desde()) || b.desde().isBefore(LocalDate.now()))
      malo("Rango inválido");
    boolean cruza =
        reservas.findAll().stream()
            .anyMatch(
                r ->
                    r.getIdCabana().equals(b.idCabana())
                        && r.getFechaIngreso().isBefore(b.hasta().plusDays(1))
                        && r.getFechaSalida().isAfter(b.desde())
                        && (List.of(EstadoReserva.CONFIRMADA, EstadoReserva.OCUPADA)
                                .contains(r.getEstadoReserva())
                            || (r.getEstadoReserva() == EstadoReserva.PENDIENTE_PAGO
                                && r.getExpiraEn().isAfter(LocalDateTime.now()))));
    if (cruza) conflicto("Existe una reserva en ese rango");
    BloqueoFecha x = new BloqueoFecha();
    x.setIdCabana(b.idCabana());
    x.setDesde(b.desde());
    x.setHasta(b.hasta());
    x.setMotivo(b.motivo());
    return bloqueos.save(x);
  }

  public List<BloqueoFecha> bloqueos(Long cabana, Actor a) {
    if (!propietario(cabana, a)) prohibido();
    return bloqueos.findByIdCabanaAndActivoTrue(cabana);
  }

  public void desbloquear(Long id, Actor a) {
    var b =
        bloqueos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!propietario(b.getIdCabana(), a)) prohibido();
    b.setActivo(false);
  }

  public Map<String, Object> reporte(Actor a) {
    a.exigir("ADMIN", "DUENO");
    var lista =
        listar(a, null, null, null).stream().filter(r -> propietario(r.getIdCabana(), a)).toList();
    BigDecimal cobrado =
        lista.stream()
            .map(r -> r.getTotalPagado() == null ? BigDecimal.ZERO : r.getTotalPagado())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    return Map.of("reservas", lista.size(), "ingresosRegistrados", cobrado);
  }

  private static void prohibido() {
    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, "No tienes permiso sobre esta cabaña o reserva");
  }

  private static void malo(String m) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
  }

  private static void conflicto(String m) {
    throw new ResponseStatusException(HttpStatus.CONFLICT, m);
  }
}
