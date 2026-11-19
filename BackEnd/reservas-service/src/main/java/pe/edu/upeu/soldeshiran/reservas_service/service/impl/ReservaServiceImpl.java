package pe.edu.upeu.soldeshiran.reservas_service.service.impl;

import feign.FeignException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.reservas_service.client.CabanaDetalleClientResponse;
import pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient;
import pe.edu.upeu.soldeshiran.reservas_service.dto.CotizacionReservaResponse;
import pe.edu.upeu.soldeshiran.reservas_service.dto.CrearReservaRequest;
import pe.edu.upeu.soldeshiran.reservas_service.dto.FechasOcupadasResponse;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservaResponse;
import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;
import pe.edu.upeu.soldeshiran.reservas_service.model.Reserva;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;
import pe.edu.upeu.soldeshiran.reservas_service.service.ReservaService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservaServiceImpl implements ReservaService {

  private static final BigDecimal PORCENTAJE_ADELANTO = new BigDecimal("0.50");

  private static final List<EstadoReserva> ESTADOS_BLOQUEANTES =
      List.of(EstadoReserva.CONFIRMADA, EstadoReserva.OCUPADA);

  private final ReservaRepository reservaRepository;
  private final CatalogoClient catalogoClient;
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;
  private final pe.edu.upeu.soldeshiran.reservas_service.repository.BloqueoFechaRepository bloqueos;

  @Value("${reservas.tiempo-expiracion-minutos:15}")
  private long tiempoExpiracionMinutos;

  @Override
  public CotizacionReservaResponse consultarDisponibilidad(
      Long idCabana, LocalDate fechaIngreso, LocalDate fechaSalida, Integer cantidadHuespedes) {
    return consultarDisponibilidad(idCabana, fechaIngreso, fechaSalida, cantidadHuespedes,
        java.time.LocalTime.MIDNIGHT, java.time.LocalTime.MIDNIGHT);
  }

  @Override
  public CotizacionReservaResponse consultarDisponibilidad(Long idCabana, LocalDate fechaIngreso,
      LocalDate fechaSalida, Integer cantidadHuespedes, java.time.LocalTime horaIngreso, java.time.LocalTime horaSalida) {
    validarFechas(fechaIngreso, fechaSalida);
    if (horaIngreso == null || horaSalida == null)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecciona la hora de ingreso y salida.");
    var inicio = fechaIngreso.atTime(horaIngreso);
    var fin = fechaSalida.atTime(horaSalida);
    if (!fin.equals(inicio.plusHours(24)))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La reserva debe durar exactamente 24 horas.");
    if (inicio.isBefore(LocalDateTime.now(java.time.ZoneId.of("America/Lima"))))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora de ingreso no puede estar en el pasado.");
    CabanaDetalleClientResponse cabana = obtenerCabanaActiva(idCabana);
    validarAforo(cabana, cantidadHuespedes);
    long noches = 1;
    Montos montos = calcularMontos(cabana.tarifaNoche(), noches);
    boolean bloqueada = bloqueos.findByIdCabanaAndActivoTrue(idCabana).stream().anyMatch(b ->
        b.getDesde().atStartOfDay().isBefore(fin) && b.getHasta().plusDays(1).atStartOfDay().isAfter(inicio));
    boolean disponible = !bloqueada && reservaRepository.buscarReservasOcupadasEnRango(
        idCabana, fechaIngreso, fechaSalida, ESTADOS_BLOQUEANTES, EstadoReserva.PENDIENTE_PAGO,
        LocalDateTime.now()).stream().noneMatch(r -> r.inicioEstancia().isBefore(fin) && r.finEstancia().isAfter(inicio));

    String mensaje =
        disponible
            ? "Las fechas están disponibles."
            : "La cabaña ya tiene una reserva para alguna de esas fechas.";

    return new CotizacionReservaResponse(
        idCabana,
        cabana.nombre(),
        fechaIngreso,
        fechaSalida,
        cantidadHuespedes,
        noches,
        cabana.tarifaNoche(),
        montos.total(),
        montos.adelanto(),
        montos.saldo(),
        disponible,
        mensaje, horaIngreso, horaSalida);
  }

  @Override
  public FechasOcupadasResponse consultarFechasOcupadas(
      Long idCabana, LocalDate desde, LocalDate hasta) {
    validarRangoConsulta(desde, hasta);
    obtenerCabanaActiva(idCabana);

    List<Reserva> reservas =
        reservaRepository.buscarReservasOcupadasEnRango(
            idCabana,
            desde,
            hasta,
            ESTADOS_BLOQUEANTES,
            EstadoReserva.PENDIENTE_PAGO,
            LocalDateTime.now());

    TreeSet<LocalDate> fechasOcupadas = new TreeSet<>();
    LocalDate finExclusivoConsulta = hasta.plusDays(1);

    for (Reserva reserva : reservas) {
      LocalDate inicioOcupacion =
          reserva.getFechaIngreso().isAfter(desde) ? reserva.getFechaIngreso() : desde;

      // La fecha de salida no representa una noche ocupada.
      LocalDate finExclusivoOcupacion =
          reserva.getFechaSalida().isBefore(finExclusivoConsulta)
              ? reserva.getFechaSalida()
              : finExclusivoConsulta;

      for (LocalDate fecha = inicioOcupacion;
          fecha.isBefore(finExclusivoOcupacion);
          fecha = fecha.plusDays(1)) {
        fechasOcupadas.add(fecha);
      }
    }

    for (var bloqueo : bloqueos.findByIdCabanaAndActivoTrue(idCabana)) {
      LocalDate inicio = bloqueo.getDesde().isBefore(desde) ? desde : bloqueo.getDesde();
      LocalDate fin = bloqueo.getHasta().isAfter(hasta) ? hasta : bloqueo.getHasta();
      for (LocalDate fecha = inicio; !fecha.isAfter(fin); fecha = fecha.plusDays(1))
        fechasOcupadas.add(fecha);
    }
    var intervalos = new java.util.ArrayList<FechasOcupadasResponse.Intervalo>();
    reservas.forEach(r -> intervalos.add(new FechasOcupadasResponse.Intervalo(r.inicioEstancia(), r.finEstancia())));
    bloqueos.findByIdCabanaAndActivoTrue(idCabana).forEach(b -> intervalos.add(
        new FechasOcupadasResponse.Intervalo(b.getDesde().atStartOfDay(), b.getHasta().plusDays(1).atStartOfDay())));
    return new FechasOcupadasResponse(idCabana, desde, hasta, List.copyOf(fechasOcupadas), intervalos);
  }

  @Override
  @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
  public ReservaResponse crearReservaTemporal(Long idUsuario, CrearReservaRequest request) {
    // A database row lock serializes check-and-insert across requests and service instances.
    jdbc.queryForObject("SELECT id FROM cerrojo_reservas WHERE id=1 FOR UPDATE", Long.class);
    validarFechas(request.fechaIngreso(), request.fechaSalida());

    CabanaDetalleClientResponse cabana = obtenerCabanaActiva(request.idCabana());

    validarAforo(cabana, request.cantidadHuespedes());

    CotizacionReservaResponse cotizacion =
        consultarDisponibilidad(
            request.idCabana(),
            request.fechaIngreso(),
            request.fechaSalida(),
            request.cantidadHuespedes(), request.horaIngreso(), request.horaSalida());

    if (!cotizacion.disponible()) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La cabaña no está disponible para las fechas seleccionadas.");
    }

    LocalDateTime expiraEn = LocalDateTime.now().plusMinutes(tiempoExpiracionMinutos);

    Reserva reserva = new Reserva();
    reserva.setIdCabana(request.idCabana());
    reserva.setIdUsuario(idUsuario);
    reserva.setFechaIngreso(request.fechaIngreso());
    reserva.setFechaSalida(request.fechaSalida());
    reserva.setHoraIngreso(request.horaIngreso());
    reserva.setHoraSalida(request.horaSalida());
    reserva.setCantidadHuespedes(request.cantidadHuespedes());
    reserva.setTarifaNocheAplicada(cotizacion.tarifaNoche());
    reserva.setMontoTotal(cotizacion.montoTotal());
    reserva.setMontoAdelanto(cotizacion.montoAdelanto());
    reserva.setSaldoPendiente(cotizacion.saldoPendiente());
    reserva.setEstadoReserva(EstadoReserva.PENDIENTE_PAGO);
    reserva.setExpiraEn(expiraEn);

    Reserva guardada = reservaRepository.save(reserva);
    return toResponse(guardada);
  }

  private boolean hayBloqueo(Long id, LocalDate entrada, LocalDate salida) {
    return bloqueos.findByIdCabanaAndActivoTrue(id).stream()
        .anyMatch(b -> !b.getHasta().isBefore(entrada) && b.getDesde().isBefore(salida));
  }

  private void validarRangoConsulta(LocalDate desde, LocalDate hasta) {
    if (desde == null || hasta == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Los parámetros desde y hasta son obligatorios.");
    }

    if (hasta.isBefore(desde)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La fecha hasta debe ser igual o posterior a desde.");
    }

    long diasConsultados = ChronoUnit.DAYS.between(desde, hasta);

    if (diasConsultados > 366) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El rango consultado no puede superar 367 días.");
    }
  }

  private CabanaDetalleClientResponse obtenerCabanaActiva(Long idCabana) {
    try {
      return catalogoClient.obtenerDetalle(idCabana);
    } catch (FeignException.NotFound exception) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "No existe una cabaña activa con el ID " + idCabana);
    } catch (FeignException exception) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudo consultar catalogo-service.");
    }
  }

  private void validarFechas(LocalDate fechaIngreso, LocalDate fechaSalida) {
    if (fechaIngreso == null || fechaSalida == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Las fechas de ingreso y salida son obligatorias.");
    }

    if (fechaIngreso.isBefore(LocalDate.now(java.time.ZoneId.of("America/Lima")))) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La fecha de ingreso no puede estar en el pasado.");
    }

    if (!fechaSalida.isAfter(fechaIngreso)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La fecha de salida debe ser posterior a la de ingreso.");
    }
  }

  private void validarAforo(CabanaDetalleClientResponse cabana, Integer cantidadHuespedes) {
    if (cantidadHuespedes == null || cantidadHuespedes < 1) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La cantidad de huéspedes debe ser al menos 1.");
    }

    if (cantidadHuespedes > cabana.aforoMaximo()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "La cabaña admite hasta " + cabana.aforoMaximo() + " huéspedes.");
    }
  }

  private Montos calcularMontos(BigDecimal tarifaNoche, long noches) {
    BigDecimal total =
        tarifaNoche.multiply(BigDecimal.valueOf(noches)).setScale(2, RoundingMode.HALF_UP);

    BigDecimal adelanto = total.multiply(PORCENTAJE_ADELANTO).setScale(2, RoundingMode.HALF_UP);

    BigDecimal saldo = total.subtract(adelanto).setScale(2, RoundingMode.HALF_UP);

    return new Montos(total, adelanto, saldo);
  }

  private ReservaResponse toResponse(Reserva reserva) {
    return new ReservaResponse(
        reserva.getIdReserva(),
        reserva.getIdCabana(),
        reserva.getIdUsuario(),
        reserva.getFechaIngreso(),
        reserva.getFechaSalida(),
        reserva.getCantidadHuespedes(),
        reserva.getTarifaNocheAplicada(),
        reserva.getMontoTotal(),
        reserva.getMontoAdelanto(),
        reserva.getSaldoPendiente(),
        reserva.getEstadoReserva(),
        reserva.getExpiraEn(), reserva.getHoraIngreso(), reserva.getHoraSalida());
  }

  private record Montos(BigDecimal total, BigDecimal adelanto, BigDecimal saldo) {}
}
