package pe.edu.upeu.soldeshiran.reservas_service;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.reservas_service.client.*;
import pe.edu.upeu.soldeshiran.reservas_service.service.ReservaService;
import pe.edu.upeu.soldeshiran.reservas_service.dto.CrearReservaRequest;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;
import java.time.*;
import java.math.BigDecimal;
import java.util.concurrent.*;
@SpringBootTest @ActiveProfiles("test")
class ReservasConcurrentesTest {
 @Autowired ReservaService servicio;
 @Autowired ReservaRepository repo;
 @MockitoBean CatalogoClient catalogo;
 @MockitoBean PagoDanioClient pagos;
 @Test void soloUnoDeDosClientesConsigueElMismoHorario() throws Exception {
  when(catalogo.obtenerDetalle(99991L)).thenReturn(new CabanaDetalleClientResponse(99991L,"Concurrente",4,new BigDecimal("250")));
  var dia=LocalDate.now().plusDays(40);
  var request=new CrearReservaRequest(99991L,dia,dia.plusDays(1),2,LocalTime.NOON,LocalTime.NOON);
  var inicio=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)) {
   Callable<Integer> reservar=()-> { inicio.await(); try {servicio.crearReservaTemporal(Thread.currentThread().threadId(),request);return 201;} catch(ResponseStatusException e) {return e.getStatusCode().value();} };
   var a=pool.submit(reservar);var c=pool.submit(reservar);inicio.countDown();
   assertEquals(java.util.Set.of(201,409),java.util.Set.of(a.get(20,TimeUnit.SECONDS),c.get(20,TimeUnit.SECONDS)));
  } finally {repo.findAll().stream().filter(r->r.getIdCabana().equals(99991L)).forEach(repo::delete);}
 }
}
