package pe.edu.upeu.soldeshiran.catalogo_service;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import pe.edu.upeu.soldeshiran.catalogo_service.repository.CabanaRepository;
import pe.edu.upeu.soldeshiran.catalogo_service.model.*;
import pe.edu.upeu.soldeshiran.catalogo_service.service.OcupacionService;
import org.springframework.web.server.ResponseStatusException;
class OcupacionTest {
 @Test void ocupacionIdempotenteConservaEstadoOperativoYRechazaOtraReserva() {
  var repo=mock(CabanaRepository.class);var c=new Cabana();when(repo.bloquearPorId(4L)).thenReturn(Optional.of(c));
  var service=new OcupacionService(repo); assertEquals("OCUPADA",service.ocupar(4L,3L));
  assertEquals("OCUPADA",service.ocupar(4L,3L));assertEquals(EstadoCabana.ACTIVA,c.getEstado());
  assertThrows(ResponseStatusException.class,()->service.ocupar(4L,9L));verify(repo,times(1)).saveAndFlush(c);
 }
}
