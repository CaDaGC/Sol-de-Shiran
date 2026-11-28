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
  private final BloqueoFechaRepository bloqueos;
  private final pe.edu.upeu.soldeshiran.coordinacion.CandadoReserva candado;
  private final org.springframework.transaction.support.TransactionTemplate transacciones;
  private final pe.edu.upeu.soldeshiran.reservas_service.client.RestauranteClient restaurante;
  private final pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient catalogo;

  @Value("${usuarios.url:http://localhost:8087}")
  private String usuariosUrl;

  @Value("${seguridad.internal-key}")
  private String key;

  public record BloqueoRequest(
      @NotNull @Positive Long idCabana,
      @NotNull LocalDate desde,
      @NotNull LocalDate hasta,
      @NotBlank @Size(max = 200) String motivo) {}

  private boolean operativo(Actor a) {
    return a.es("ADMIN", "TRABAJADOR");
  }

  void exigirOperativo(Actor a) {
    if (!operativo(a)) prohibido();
  }

  private void exigirVer(Reserva r, Actor a) {
    a.exigir("ADMIN", "CLIENTE", "TRABAJADOR");
    if (!Objects.equals(r.getIdUsuario(), a.id()) && !operativo(a)) prohibido();
  }

  private Reserva find(Long id, boolean lock) {
    return (lock ? reservas.bloquearPorId(id) : reservas.findById(id))
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
  }

  public List<Reserva> listar(Actor a, Long cabana, LocalDate desde, LocalDate hasta) {
    a.exigir("ADMIN", "CLIENTE", "TRABAJADOR");
    return reservas.findAll().stream()
        .filter(r -> cabana == null || cabana.equals(r.getIdCabana()))
        .filter(r -> desde == null || !r.getFechaSalida().isBefore(desde))
        .filter(r -> hasta == null || !r.getFechaIngreso().isAfter(hasta))
        .filter(
            r ->
                Objects.equals(r.getIdUsuario(), a.id())
                    || operativo(a))
        .toList();
  }

  public Reserva buscarPorCodigo(String codigo, Actor actor) {
    actor.exigir("ADMIN", "TRABAJADOR", "CLIENTE");
    String normalizado = codigo.strip().toUpperCase(java.util.Locale.ROOT);
    if (!normalizado.matches("RSS[0-9]{5,19}")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Código de reserva inválido");
    }

    return (actor.es("CLIENTE")
        ? reservas.findByCodigoAndIdUsuario(normalizado, actor.id())
        : reservas.findByCodigo(normalizado))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));
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
    exigirOperativo(actor);
    find(id, false);
  }

  public Reserva checkin(Long id, Actor a) {
    Reserva r = find(id, true);
    exigirOperativo(a);
    if (r.getEstadoReserva() != EstadoReserva.CONFIRMADA
        || r.getSaldoPendiente().compareTo(BigDecimal.ZERO) != 0)
      conflicto("Debe estar confirmada y sin saldo pendiente");
    var hoy = java.time.LocalDateTime.now(java.time.ZoneId.of("America/Lima"));
    if (hoy.isBefore(r.inicioEstancia()) || !hoy.isBefore(r.finEstancia()))
      conflicto("El ingreso debe estar dentro de la estancia");
    actualizarOcupacion(r, true);
    r.setEstadoReserva(EstadoReserva.OCUPADA);
    return r;
  }

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  public Reserva checkout(Long id, CheckoutRequest request, Actor a) {
    exigirOperativo(a);
    return candado.ejecutar(id, () -> transacciones.execute(
        estado -> checkoutInterno(id, request, a)));
  }

  private Reserva checkoutInterno(Long id, CheckoutRequest request, Actor a) {
    Reserva r = find(id, true);
    exigirOperativo(a);

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

    try {
      Boolean pendientes = restaurante.tienePendientes(id, key);
      if (pendientes == null) throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudo verificar los pedidos de la reserva");
      if (pendientes) conflicto("Finaliza o rechaza los pedidos pendientes antes de registrar el check-out");
    } catch (feign.FeignException exception) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "No se pudieron verificar los pedidos. El check-out no se ha registrado.", exception);
    }

    r.setCheckout(new RegistroCheckout(
        LocalDateTime.now(ZoneId.of("America/Lima")).truncatedTo(ChronoUnit.SECONDS),
        a.id(), descripcion, monto, medio));
    r.setEstadoReserva(EstadoReserva.FINALIZADA);

    reservas.saveAndFlush(r);
    actualizarOcupacion(r, false);
    return r;
  }

  private void actualizarOcupacion(Reserva reserva, boolean ocupar) {
    try {
      if (ocupar) {
        catalogo.ocupar(reserva.getIdCabana(), reserva.getIdReserva(), key);
      } else {
        catalogo.liberar(reserva.getIdCabana(), reserva.getIdReserva(), key);
      }
    } catch (feign.FeignException exception) {
      if (exception.status() == 409) conflicto("La cabaña ya está ocupada");
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudo actualizar la ocupación de la cabaña", exception);
    }
  }

  public Reserva cancelar(Long id, Actor a) {
    a.exigir("ADMIN", "CLIENTE");
    Reserva r = find(id, true);
    boolean gestion = a.es("ADMIN");
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
    a.exigir("ADMIN");
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
    a.exigir("ADMIN");
    return bloqueos.findByIdCabanaAndActivoTrue(cabana);
  }

  public void desbloquear(Long id, Actor a) {
    var b =
        bloqueos.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    a.exigir("ADMIN");
    b.setActivo(false);
  }

  public Map<String, Object> reporte(Actor a) {
    a.exigir("ADMIN", "DUENO");
    var lista = reservas.findAll();
    if (a.es("DUENO")) {
      var propias = catalogo.listarPropias(a.id(), key);
      lista = lista.stream().filter(r -> propias.contains(r.getIdCabana())).toList();
    }
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
