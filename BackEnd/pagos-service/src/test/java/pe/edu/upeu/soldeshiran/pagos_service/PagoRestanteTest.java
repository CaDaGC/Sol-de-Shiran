package pe.edu.upeu.soldeshiran.pagos_service;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Validation;
import pe.edu.upeu.soldeshiran.pagos_service.service.PagoRestanteService;
import pe.edu.upeu.soldeshiran.pagos_service.repository.TransaccionPagoRepository;
import pe.edu.upeu.soldeshiran.pagos_service.client.ReservaInternaClient;
import pe.edu.upeu.soldeshiran.pagos_service.dto.*;
import pe.edu.upeu.soldeshiran.pagos_service.model.TransaccionPago;
import pe.edu.upeu.soldeshiran.pagos_service.model.EstadoTransaccion;
class PagoRestanteTest {
 TransaccionPagoRepository repo; ReservaInternaClient reservas; PagoRestanteService service;
 RegistrarPagoRestanteRequest request;
 @BeforeEach void setup() {
  repo=mock(TransaccionPagoRepository.class); reservas=mock(ReservaInternaClient.class);
  service=new PagoRestanteService(repo,reservas); ReflectionTestUtils.setField(service,"token","internal");
  request=new RegistrarPagoRestanteRequest(RegistrarPagoRestanteRequest.MetodoPago.EFECTIVO,null,UUID.randomUUID());
  when(reservas.saldoCheckIn(3L,"internal")).thenReturn(new SaldoCheckInResponse(3L,new BigDecimal("125.00"),"CONFIRMADA"));
  when(repo.saveAndFlush(any())).thenAnswer(i->{TransaccionPago p=i.getArgument(0);p.setIdTransaccion(7L);return p;});
 }
 @Test void registraSaldoDelServidorYCompleta() {
  var pago=service.registrar(2L,3L,request);
  assertEquals(EstadoTransaccion.APROBADA, pago.getEstadoTransaccion());
  assertEquals(TransaccionPago.RESTANTE, pago.getTipoPago()); assertNull(pago.getIdExternoPasarela()); assertEquals(new BigDecimal("125.00"),pago.getMonto());
  verify(reservas).checkIn(3L,"internal",new CheckInRequest("RESTANTE-TX-7",new BigDecimal("125.00")));
 }
 @Test void reintentoCompletadoNoRepiteCheckInNiInserta() {
  var pago=service.registrar(2L,3L,request); clearInvocations(repo,reservas);
  when(repo.findByReservaPagoRestante(3L)).thenReturn(Optional.of(pago));
  assertSame(pago,service.registrar(2L,3L,request));
  verify(repo,never()).saveAndFlush(any()); verifyNoInteractions(reservas);
 }
 @Test void recuperaFalloRemotoSinDuplicarPago() {
  doThrow(new feign.RetryableException(503,"offline",feign.Request.HttpMethod.POST,(Long)null,
   feign.Request.create(feign.Request.HttpMethod.POST,"http://test",Map.of(),null,java.nio.charset.StandardCharsets.UTF_8,null)))
   .doNothing().when(reservas).checkIn(any(),any(),any());
  final TransaccionPago[] guardado={null};
  doAnswer(i->{TransaccionPago p=i.getArgument(0);p.setIdTransaccion(7L);guardado[0]=p;return p;}).when(repo).saveAndFlush(any());
  assertThrows(ResponseStatusException.class,()->service.registrar(2L,3L,request));
  assertNotNull(guardado[0]); assertEquals(EstadoTransaccion.PENDIENTE,guardado[0].getEstadoTransaccion());
  when(repo.findByReservaPagoRestante(3L)).thenReturn(Optional.of(guardado[0]));
  assertEquals(EstadoTransaccion.APROBADA,service.registrar(2L,3L,request).getEstadoTransaccion());
  verify(reservas,times(1)).saldoCheckIn(any(),any());
 }
 @Test void rechazaOtroMetodoParaReservaYaRegistrada() {
  var pago=service.registrar(2L,3L,request);when(repo.findByReservaPagoRestante(3L)).thenReturn(Optional.of(pago));
  assertThrows(ResponseStatusException.class,()->service.registrar(2L,3L,new RegistrarPagoRestanteRequest(
   RegistrarPagoRestanteRequest.MetodoPago.TARJETA,"123",UUID.randomUUID())));
 }
 @Test void validacionCondicionalDeReferencia() {
  try(var factory=Validation.buildDefaultValidatorFactory()) {
   var validator=factory.getValidator();
   assertTrue(validator.validate(request).isEmpty());
   for(var metodo:List.of(RegistrarPagoRestanteRequest.MetodoPago.TARJETA,RegistrarPagoRestanteRequest.MetodoPago.YAPE_BILLETERA)) {
    assertFalse(validator.validate(new RegistrarPagoRestanteRequest(metodo,"  ",UUID.randomUUID())).isEmpty());
    assertTrue(validator.validate(new RegistrarPagoRestanteRequest(metodo,"ABC123",UUID.randomUUID())).isEmpty());
    assertFalse(validator.validate(new RegistrarPagoRestanteRequest(metodo,"A".repeat(101),UUID.randomUUID())).isEmpty());
   }
  }
 }
}
