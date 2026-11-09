package pe.edu.upeu.soldeshiran.reservas_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.reservas_service.model.*;
import pe.edu.upeu.soldeshiran.reservas_service.repository.*;
import pe.edu.upeu.soldeshiran.reservas_service.security.Actor;
import pe.edu.upeu.soldeshiran.reservas_service.service.GestionReservaService;

class GestionReservaTest {
  ReservaRepository repo;
  PagoPresencialRepository pagos;
  GestionReservaService service;
  Reserva r;
  Actor admin = new Actor(1L, "ADMIN");

  @BeforeEach
  void setup() {
    repo = mock(ReservaRepository.class);
    pagos = mock(PagoPresencialRepository.class);
    service = new GestionReservaService(repo, pagos, mock(BloqueoFechaRepository.class),
        mock(pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient.class));
    r = new Reserva();
    r.setIdReserva(1L);
    r.setIdCabana(1L);
    r.setIdUsuario(5L);
    r.setMontoTotal(new BigDecimal("200"));
    r.setMontoAdelanto(new BigDecimal("100"));
    r.setSaldoPendiente(new BigDecimal("100"));
    r.setExpiraEn(LocalDateTime.now().plusMinutes(15));
    r.setFechaIngreso(LocalDate.now());
    r.setFechaSalida(LocalDate.now().plusDays(2));
    when(repo.bloquearPorId(1L)).thenReturn(Optional.of(r));
    when(repo.findById(1L)).thenReturn(Optional.of(r));
  }

  GestionReservaService.PagoRequest pago(String monto) {
    return new GestionReservaService.PagoRequest(
        new BigDecimal(monto), "EFECTIVO", null, "operacion-1");
  }

  @Test
  void clienteNoRegistraPagos() {
    assertEquals(
        403,
        assertThrows(
                ResponseStatusException.class,
                () -> service.pagar(1L, pago("100"), new Actor(5L, "USUARIO")))
            .getStatusCode()
            .value());
    verify(pagos, never()).saveAndFlush(any());
  }

  @Test
  void pagoAdelantoConfirma() {
    service.pagar(1L, pago("100"), admin);
    assertEquals(EstadoReserva.CONFIRMADA, r.getEstadoReserva());
    assertEquals(new BigDecimal("100"), r.getSaldoPendiente());
    verify(pagos).saveAndFlush(any());
  }

  @Test
  void noSeAceptaSobrepago() {
    assertThrows(ResponseStatusException.class, () -> service.pagar(1L, pago("201"), admin));
    verify(pagos, never()).saveAndFlush(any());
  }

  @Test
  void pagoRepetidoNoDuplica() {
    PagoPresencial p = new PagoPresencial();
    p.setReservaId(1L);
    p.setMonto(new BigDecimal("100"));
    p.setMedio("EFECTIVO");
    when(pagos.findByClaveOperacion("operacion-1")).thenReturn(Optional.of(p));
    service.pagar(1L, pago("100"), admin);
    verify(pagos, never()).saveAndFlush(any());
  }

  @Test
  void claveRepetidaConOtroMontoSeRechaza() {
    PagoPresencial p = new PagoPresencial();
    p.setReservaId(1L);
    p.setMonto(new BigDecimal("90"));
    p.setMedio("EFECTIVO");
    when(pagos.findByClaveOperacion("operacion-1")).thenReturn(Optional.of(p));
    assertThrows(ResponseStatusException.class, () -> service.pagar(1L, pago("100"), admin));
  }

  @Test
  void reservaVencidaNoCobra() {
    r.setExpiraEn(LocalDateTime.now().minusMinutes(1));
    assertThrows(ResponseStatusException.class, () -> service.pagar(1L, pago("100"), admin));
  }

  @Test
  void checkinRequiereSaldoCero() {
    r.setEstadoReserva(EstadoReserva.CONFIRMADA);
    assertThrows(ResponseStatusException.class, () -> service.checkin(1L, admin));
  }

  @Test
  void checkinCorrecto() {
    r.setEstadoReserva(EstadoReserva.CONFIRMADA);
    r.setSaldoPendiente(BigDecimal.ZERO);
    service.checkin(1L, admin);
    assertEquals(EstadoReserva.OCUPADA, r.getEstadoReserva());
  }

  @Test
  void checkoutRequiereOcupada() {
    assertThrows(ResponseStatusException.class, () -> service.checkout(1L, admin));
  }

  @Test
  void clienteNoConsultaReservaAjena() {
    assertThrows(
        ResponseStatusException.class, () -> service.obtener(1L, new Actor(99L, "USUARIO")));
  }

  @Test
  void trabajadorNoCancela() {
    assertThrows(
        ResponseStatusException.class, () -> service.cancelar(1L, new Actor(5L, "TRABAJADOR")));
  }
}
