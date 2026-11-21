package pe.edu.upeu.soldeshiran.reservas_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.reservas_service.client.*;
import pe.edu.upeu.soldeshiran.reservas_service.dto.*;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;
import pe.edu.upeu.soldeshiran.reservas_service.security.Actor;
import pe.edu.upeu.soldeshiran.reservas_service.service.*;

@SpringBootTest
@ActiveProfiles("test")
class CodigoReservaTest {
  @Autowired ReservaService servicio;
  @Autowired GestionReservaService gestion;
  @Autowired ReservaRepository repo;
  @Autowired ReservaSchemaInitializer inicializador;
  @Autowired JdbcTemplate jdbc;
  @MockitoBean CatalogoClient catalogo;
  @MockitoBean PagoDanioClient pagos;

  private CrearReservaRequest solicitud(int dias) {
    when(catalogo.obtenerDetalle(99881L)).thenReturn(
        new CabanaDetalleClientResponse(99881L,"Código",4,new BigDecimal("250")));
    var fecha=LocalDate.now().plusDays(dias);
    return new CrearReservaRequest(99881L,fecha,fecha.plusDays(1),2,LocalTime.NOON,LocalTime.NOON);
  }
  private long numero(String codigo) { return Long.parseLong(codigo.substring(3)); }

  @Test void consecutivoPersisteTrasEliminarYRestringeBusquedaDelCliente() {
    var primera=servicio.crearReservaTemporal(501L,solicitud(301));
    try {
      assertTrue(primera.codigo().matches("RSS[0-9]{5,}"));
      assertEquals(primera.idReserva(),gestion.buscarPorCodigo(primera.codigo(),new Actor(501L,"CLIENTE")).getIdReserva());
      assertEquals(primera.idReserva(),gestion.buscarPorCodigo(primera.codigo(),new Actor(1L,"ADMIN")).getIdReserva());
      assertEquals(primera.idReserva(),gestion.buscarPorCodigo(primera.codigo(),new Actor(2L,"TRABAJADOR")).getIdReserva());
      assertEquals(HttpStatus.NOT_FOUND,assertThrows(ResponseStatusException.class,
          ()->gestion.buscarPorCodigo(primera.codigo(),new Actor(502L,"CLIENTE"))).getStatusCode());
      assertEquals(HttpStatus.FORBIDDEN,assertThrows(ResponseStatusException.class,
          ()->gestion.buscarPorCodigo(primera.codigo(),new Actor(2L,"DUENO"))).getStatusCode());
      repo.deleteById(primera.idReserva());
      inicializador.run();
      var segunda=servicio.crearReservaTemporal(501L,solicitud(301));
      try { assertEquals(numero(primera.codigo())+1,numero(segunda.codigo())); }
      finally { repo.deleteById(segunda.idReserva()); }
    } finally { repo.findById(primera.idReserva()).ifPresent(repo::delete); }
  }

  @Test void concurrentesEnHorariosDistintosRecibenCodigosDistintosConsecutivos() throws Exception {
    var a=solicitud(310); var b=solicitud(313); var inicio=new CountDownLatch(1);
    try(var pool=Executors.newFixedThreadPool(2)) {
      var fa=pool.submit(()->{inicio.await();return servicio.crearReservaTemporal(501L,a);});
      var fb=pool.submit(()->{inicio.await();return servicio.crearReservaTemporal(502L,b);});
      inicio.countDown(); var ra=fa.get(20,TimeUnit.SECONDS); var rb=fb.get(20,TimeUnit.SECONDS);
      assertNotEquals(ra.codigo(),rb.codigo());
      assertEquals(1,Math.abs(numero(ra.codigo())-numero(rb.codigo())));
    } finally { repo.findAll().stream().filter(x->x.getIdCabana().equals(99881L)).forEach(repo::delete); }
  }

  @Test void migraReservasSinCodigoYNoReasignaCodigosAlReiniciar() {
    var original=servicio.crearReservaTemporal(501L,solicitud(320));
    try {
      jdbc.update("UPDATE reserva SET codigo=NULL WHERE id_reserva=?",original.idReserva());
      inicializador.run();
      var migrada=repo.findById(original.idReserva()).orElseThrow();
      assertNotNull(migrada.getCodigo());
      assertEquals(numero(original.codigo())+1,numero(migrada.getCodigo()));
      inicializador.run();
      assertEquals(migrada.getCodigo(),repo.findById(original.idReserva()).orElseThrow().getCodigo());
    } finally { repo.deleteById(original.idReserva()); }
  }
}
