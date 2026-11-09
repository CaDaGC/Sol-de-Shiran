package pe.edu.upeu.soldeshiran.reservas_service.service;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;
import pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient;
import pe.edu.upeu.soldeshiran.reservas_service.model.*;
import pe.edu.upeu.soldeshiran.reservas_service.dto.CheckInRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.util.ReflectionTestUtils;
class CheckInTest {
 ReservaRepository repo; CatalogoClient catalogo; ReservaPagoInternoService service; Reserva r;
 @BeforeEach void setup() {
  repo=mock(ReservaRepository.class);catalogo=mock(CatalogoClient.class);service=new ReservaPagoInternoService(repo,catalogo);
  ReflectionTestUtils.setField(service,"token","internal");
  r=new Reserva();r.setIdReserva(3L);r.setIdCabana(4L);r.setEstadoReserva(EstadoReserva.CONFIRMADA);
  r.setSaldoPendiente(new BigDecimal("125.00"));r.setMontoTotal(new BigDecimal("250.00"));
  var hoy=LocalDate.now(ZoneId.of("America/Lima"));r.setFechaIngreso(hoy);r.setFechaSalida(hoy.plusDays(1));
  when(repo.bloquearPorId(3L)).thenReturn(Optional.of(r));
 }
 @Test void poneSaldoCeroYEstadia() {
  service.checkIn(3L,new CheckInRequest("RESTANTE-7",new BigDecimal("125.00")));
  assertEquals(EstadoReserva.OCUPADA,r.getEstadoReserva());assertEquals(0,r.getSaldoPendiente().signum());
  verify(catalogo).ocupar(4L,3L,"internal");
 }
 @Test void reintentoMismaReferenciaNoRepiteOcupacion() {
  var request=new CheckInRequest("RESTANTE-7",new BigDecimal("125.00"));service.checkIn(3L,request);service.checkIn(3L,request);
  verify(catalogo,times(1)).ocupar(any(),any(),any());
 }
 @Test void rechazaMontoAlterado() {
  assertThrows(ResponseStatusException.class,()->service.checkIn(3L,new CheckInRequest("x",BigDecimal.ONE)));
  verifyNoInteractions(catalogo);
 }
 @Test void rechazaIngresoFuturoYReservaCancelada() {
  r.setFechaIngreso(r.getFechaIngreso().plusDays(1));
  assertThrows(ResponseStatusException.class,()->service.checkIn(3L,new CheckInRequest("x",new BigDecimal("125.00"))));
  r.setEstadoReserva(EstadoReserva.CANCELADA);
  assertThrows(ResponseStatusException.class,()->service.checkIn(3L,new CheckInRequest("x",new BigDecimal("125.00"))));verifyNoInteractions(catalogo);
 }
}
