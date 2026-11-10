package pe.edu.upeu.soldeshiran.catalogo_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaRequest;
import pe.edu.upeu.soldeshiran.catalogo_service.model.*;
import pe.edu.upeu.soldeshiran.catalogo_service.repository.CabanaRepository;
import pe.edu.upeu.soldeshiran.catalogo_service.security.*;
import pe.edu.upeu.soldeshiran.catalogo_service.service.GestionCabanaService;

class GestionCabanaTest {
  CabanaRepository repo;
  GestionCabanaService service;
  Cabana c;

  CabanaRequest request() {
    return new CabanaRequest(
        "CabaÃ±a",
        "DescripciÃ³n",
        4,
        2,
        1,
        new BigDecimal("150"),
        "/assets/cabana.jpg",
        List.of(),
        null);
  }

  @BeforeEach
  void setup() {
    repo = mock(CabanaRepository.class);
    service = new GestionCabanaService(repo);
    c = new Cabana();
    c.setIdCabana(1L);
    c.setDuenoId(10L);
    when(repo.findById(1L)).thenReturn(Optional.of(c));
    when(repo.save(any()))
        .thenAnswer(
            i -> {
              Cabana saved = i.getArgument(0);
              saved.setIdCabana(2L);
              return saved;
            });
  }

  @Test
  void clienteNoCrea() {
    assertEquals(
        403,
        assertThrows(
                ResponseStatusException.class,
                () -> service.crear(request(), new Actor(1L, "USUARIO")))
            .getStatusCode()
            .value());
    verify(repo, never()).save(any());
  }

  @Test
  void trabajadorNoEdita() {
    assertEquals(
        403,
        assertThrows(
                ResponseStatusException.class,
                () -> service.editar(1L, request(), new Actor(3L, "TRABAJADOR")))
            .getStatusCode()
            .value());
  }

  @Test
  void duenoNoEditaAjena() {
    assertEquals(
        403,
        assertThrows(
                ResponseStatusException.class,
                () -> service.editar(1L, request(), new Actor(11L, "DUENO")))
            .getStatusCode()
            .value());
  }

  @Test
  void duenoNoPuedeCrearCabana() {
    var error = assertThrows(ResponseStatusException.class,
        () -> service.crear(request(), new Actor(10L, "DUENO")));
    assertEquals(403, error.getStatusCode().value());
  }

  @Test
  void adminPuedeCrearSinDueno() {
    var resultado = service.crear(request(), new Actor(1L, "ADMIN"));
    assertEquals("CabaÃ±a", resultado.get("nombre"));
  }

  @Test
  void eliminarEsDesactivar() {
    service.desactivar(1L, new Actor(1L, "ADMIN"));
    assertEquals(EstadoCabana.INACTIVA, c.getEstado());
    verify(repo, never()).delete(any());
  }

  @Test
  void duenoNoDesactivaAjena() {
    assertThrows(
        ResponseStatusException.class, () -> service.desactivar(1L, new Actor(11L, "DUENO")));
    assertEquals(EstadoCabana.ACTIVA, c.getEstado());
  }

  @Test
  void soloAdminListaGestionDeCabanas() {
    for (String rol : List.of("DUENO", "TRABAJADOR", "CLIENTE")) {
      var error = assertThrows(ResponseStatusException.class,
          () -> service.listar(new Actor(10L, rol)));
      assertEquals(403, error.getStatusCode().value());
    }
  }

  @Test
  void firmaFalsaSeRechaza() {
    var verifier = new TokenVerifier(new com.fasterxml.jackson.databind.ObjectMapper(), "clave");
    assertThrows(
        IllegalArgumentException.class,
        () -> verifier.verificar("Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjF9.AAAA"));
  }
}
