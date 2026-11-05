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
import pe.edu.upeu.soldeshiran.pagos_service.repository.PagoRestanteRepository;
import pe.edu.upeu.soldeshiran.pagos_service.client.ReservaInternaClient;
import pe.edu.upeu.soldeshiran.pagos_service.dto.*;
import pe.edu.upeu.soldeshiran.pagos_service.model.PagoRestante;
class PagoRestanteTest {
 PagoRestanteRepository repo; ReservaInternaClient reservas; PagoRestanteService service;
 RegistrarPagoRestanteRequest request;
 @BeforeEach void setup() {
  repo=mock(PagoRestanteRepository.class); reservas=mock(ReservaInternaClient.class);
  service=new PagoRestanteService(repo,reservas); ReflectionTestUtils.setField(service,"token","internal");
  request=new RegistrarPagoRestanteRequest(RegistrarPagoRestanteRequest.MetodoPago.EFECTIVO,null,UUID.randomUUID());
  when(reservas.saldoCheckIn(3L,"internal")).thenReturn(new SaldoCheckInResponse(3L,new BigDecimal("125.00"),"CONFIRMADA"));
  when(repo.saveAndFlush(any())).thenAnswer(i->{PagoRestante p=i.getArgument(0);p.setId(7L);return p;});
 }
 @Test void registraSaldoDelServidorYCompleta() {
  var pago=service.registrar(2L,3L,request);
  assertTrue(pago.isCompletado()); assertEquals(new BigDecimal("125.00"),pago.getMonto());
  verify(reservas).checkIn(3L,"internal",new CheckInRequest("RESTANTE-7",new BigDecimal("125.00")));
 }
 @Test void reintentoCompletadoNoRepiteCheckInNiInserta() {
  var pago=service.registrar(2L,3L,request); clearInvocations(repo,reservas);
  when(repo.findByIdReserva(3L)).thenReturn(Optional.of(pago));
  assertSame(pago,service.registrar(2L,3L,request));
  verify(repo,never()).saveAndFlush(any()); verifyNoInteractions(reservas);
 }
 @Test void recuperaFalloRemotoSinDuplicarPago() {
  doThrow(new feign.RetryableException(503,"offline",feign.Request.HttpMethod.POST,(Long)null,
   feign.Request.create(feign.Request.HttpMethod.POST,"http://test",Map.of(),null,java.nio.charset.StandardCharsets.UTF_8,null)))
   .doNothing().when(reservas).checkIn(any(),any(),any());
  final PagoRestante[] guardado={null};
  doAnswer(i->{PagoRestante p=i.getArgument(0);p.setId(7L);guardado[0]=p;return p;}).when(repo).saveAndFlush(any());
  assertThrows(ResponseStatusException.class,()->service.registrar(2L,3L,request));
  assertNotNull(guardado[0]); assertFalse(guardado[0].isCompletado());
  when(repo.findByIdReserva(3L)).thenReturn(Optional.of(guardado[0]));
  assertTrue(service.registrar(2L,3L,request).isCompletado());
  verify(reservas,times(1)).saldoCheckIn(any(),any());
 }
 @Test void rechazaOtroMetodoParaReservaYaRegistrada() {
  var pago=service.registrar(2L,3L,request);when(repo.findByIdReserva(3L)).thenReturn(Optional.of(pago));
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
