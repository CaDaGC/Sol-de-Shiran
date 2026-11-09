package pe.edu.upeu.soldeshiran.catalogo_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaRequest;
import pe.edu.upeu.soldeshiran.catalogo_service.model.*;
import pe.edu.upeu.soldeshiran.catalogo_service.repository.CabanaRepository;
import pe.edu.upeu.soldeshiran.catalogo_service.security.Actor;

@Service
@Transactional
@RequiredArgsConstructor
public class GestionCabanaService {
  private final CabanaRepository cabanas;

  @Value("${usuarios.url:http://localhost:8087}")
  private String usuariosUrl;

  @Value("${seguridad.internal-key:sol-shiran-interno-desarrollo-cambiar}")
  private String key;

  @Transactional(readOnly = true)
  public boolean tieneAsignaciones(Long usuarioId) {
    return cabanas.existsByDuenoId(usuarioId) || cabanas.contarAsignaciones(usuarioId) > 0;
  }

  /** Quita las referencias a una cuenta sin borrar ni desactivar las cabañas. */
  public void desvincularCuenta(Long usuarioId) {
    cabanas.findAll().forEach(c -> {
      boolean cambio = false;
      if (Objects.equals(c.getDuenoId(), usuarioId)) {
        c.setDuenoId(null);
        cambio = true;
      }
      if (c.getTrabajadores().remove(usuarioId)) cambio = true;
      if (cambio) cabanas.save(c);
    });
  }

  public Cabana obtener(Long id) {
    return cabanas
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cabaña no encontrada"));
  }

  public boolean puedeVer(Cabana c, Actor a) {
    return a.es("ADMIN")
        || (a.es("DUENO") && Objects.equals(c.getDuenoId(), a.id()))
        || (a.es("TRABAJADOR")
            && c.getTrabajadores().contains(a.id())
            && perteneceAlDuenoActual(c, a));
  }

  public void exigirGestion(Cabana c, Actor a) {
    a.exigir("ADMIN", "DUENO");
    if (a.es("DUENO") && !Objects.equals(c.getDuenoId(), a.id()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta cabaña pertenece a otro dueño");
  }

  public List<Map<String, Object>> listar(Actor a) {
    a.exigir("ADMIN", "DUENO", "TRABAJADOR");
    return cabanas.findAll().stream().filter(c -> puedeVer(c, a)).map(this::view).toList();
  }

  public Map<String, Object> crear(CabanaRequest r, Actor a) {
    a.exigir("ADMIN", "DUENO");
    Cabana c = new Cabana();
    c.setDuenoId(a.es("DUENO") ? a.id() : validarDueno(r.duenoId()));
    aplicar(c, r);
    return view(cabanas.save(c));
  }

  public Map<String, Object> editar(Long id, CabanaRequest r, Actor a) {
    Cabana c = obtener(id);
    exigirGestion(c, a);
    if (a.es("ADMIN") && !Objects.equals(c.getDuenoId(), r.duenoId())) {
      c.setDuenoId(validarDueno(r.duenoId()));
      c.getTrabajadores().clear();
    }
    if (a.es("DUENO") && r.duenoId() != null && !r.duenoId().equals(a.id()))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes transferir la cabaña");
    aplicar(c, r);
    return view(c);
  }

  public void desactivar(Long id, Actor a) {
    Cabana c = obtener(id);
    exigirGestion(c, a);
    c.setEstado(EstadoCabana.INACTIVA);
  }

  public Map<String, Object> activar(Long id, Actor a) {
    Cabana c = obtener(id);
    exigirGestion(c, a);
    c.setEstado(EstadoCabana.ACTIVA);
    return view(c);
  }

  public Map<String, Object> asignar(Long id, Set<Long> ids, Actor a) {
    Cabana c = obtener(id);
    exigirGestion(c, a);
    if (ids == null || ids.size() > 100)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asignación inválida");
    for (Long uid : ids) {
      JsonNode u = identidad(uid);
      if (!u.path("activo").asBoolean()
          || !Actor.normalizar(u.path("rol").asText()).equals("TRABAJADOR"))
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Solo se asignan trabajadores activos");
      Long duenoTrabajador = u.path("duenoId").isNumber() ? u.path("duenoId").asLong() : null;
      if (!Objects.equals(c.getDuenoId(), duenoTrabajador))
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Trabajador de otro dueño");
    }
    c.setTrabajadores(new HashSet<>(ids));
    return view(c);
  }

  private boolean perteneceAlDuenoActual(Cabana c, Actor a) {
    var u = identidad(a.id());
    Long actual = u.path("duenoId").isNumber() ? u.path("duenoId").asLong() : null;
    return java.util.Objects.equals(actual, c.getDuenoId());
  }

  private JsonNode identidad(Long id) {
    try {
      return pe.edu.upeu.soldeshiran.catalogo_service.security.InternalHttp.cliente(usuariosUrl)
          .get()
          .uri("/internal/usuarios/" + id)
          .header("X-Internal-Key", key)
          .retrieve()
          .body(JsonNode.class);
    } catch (org.springframework.web.client.RestClientResponseException e) {
      throw new ResponseStatusException(
          e.getStatusCode().is4xxClientError()
              ? HttpStatus.BAD_REQUEST
              : HttpStatus.SERVICE_UNAVAILABLE,
          "No se pudo validar el usuario");
    } catch (org.springframework.web.client.ResourceAccessException e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Usuarios no disponible");
    }
  }

  private Long validarDueno(Long id) {
    if (id == null) return null;
    JsonNode u = identidad(id);
    if (!u.path("activo").asBoolean() || !Actor.normalizar(u.path("rol").asText()).equals("DUENO"))
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El dueño debe ser una cuenta DUENO activa");
    return id;
  }

  private void aplicar(Cabana c, CabanaRequest r) {
    c.setNombre(r.nombre().trim());
    c.setDescripcion(r.descripcion().trim());
    c.setAforoMaximo(r.aforoMaximo());
    c.setNumHabitaciones(r.numHabitaciones());
    c.setNumBanos(r.numBanos());
    c.setTarifaNoche(r.tarifaNoche());
    c.setImagenPrincipal(r.imagenPrincipal());
    c.getImagenes().clear();
    if (r.imagenesSecundarias() != null)
      for (String url : r.imagenesSecundarias()) {
        CabanaImagen i = new CabanaImagen();
        i.setCabana(c);
        i.setUrlImagen(url);
        c.getImagenes().add(i);
      }
  }

  public Map<String, Object> view(Cabana c) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("idCabana", c.getIdCabana());
    m.put("nombre", c.getNombre());
    m.put("descripcion", c.getDescripcion());
    m.put("aforoMaximo", c.getAforoMaximo());
    m.put("numHabitaciones", c.getNumHabitaciones());
    m.put("numBanos", c.getNumBanos());
    m.put("tarifaNoche", c.getTarifaNoche());
    m.put("imagenPrincipal", c.getImagenPrincipal());
    m.put("estado", c.getEstado());
    m.put("duenoId", c.getDuenoId());
    m.put("trabajadores", c.getTrabajadores());
    m.put("imagenesSecundarias", c.getImagenes().stream().map(CabanaImagen::getUrlImagen).toList());
    return m;
  }
}
