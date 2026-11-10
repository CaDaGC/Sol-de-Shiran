package pe.edu.upeu.soldeshiran.usuario_service.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.usuario_service.dto.*;
import pe.edu.upeu.soldeshiran.usuario_service.mapper.UsuarioMapper;
import pe.edu.upeu.soldeshiran.usuario_service.model.*;
import pe.edu.upeu.soldeshiran.usuario_service.repository.*;
import pe.edu.upeu.soldeshiran.usuario_service.security.Actor;

@Service
@RequiredArgsConstructor
@Transactional
public class GestionUsuarioService {
  private final UsuarioRepository usuarios;
  private final RolRepository roles;
  private final UsuarioMapper mapper;
  private final PasswordEncoder passwords;

  @Value("${catalogo.url:http://localhost:8086}")
  private String catalogoUrl;

  @Value("${seguridad.internal-key:sol-shiran-interno-desarrollo-cambiar}")
  private String internalKey;

  public UsuarioResponse crear(UsuarioRequest r, Actor actor, boolean publico) {
    validar(r);
    String rol =
        publico ? "CLIENTE" : Actor.normalizar(r.getRol() == null ? "CLIENTE" : r.getRol());
    if (!publico) {
      actor.exigir("ADMIN");
    }
    if (usuarios.existsByEmail(r.getEmail().trim().toLowerCase()))
      conflicto("El correo ya estÃ¡ registrado");
    Usuario u = mapper.toEntity(r);
    u.setEmail(r.getEmail().trim().toLowerCase());
    if (r.getPassword() == null || r.getPassword().length() < 8)
      malo("La contraseÃ±a debe tener al menos 8 caracteres");
    u.setPassword(passwords.encode(r.getPassword()));
    u.setRol(rol(rol));
    if (!publico && rol.equals("TRABAJADOR")) u.setDuenoId(validarDueno(r.getDuenoId()));
    return mapper.toResponse(usuarios.save(u));
  }

  public List<UsuarioResponse> listar(Actor a) {
    a.exigir("ADMIN");
    return usuarios.findAll().stream()
        .map(mapper::toResponse)
        .toList();
  }

  public UsuarioResponse obtener(Long id, Actor a) {
    Usuario u = find(id);
    exigirVisible(u, a);
    return mapper.toResponse(u);
  }

  public UsuarioResponse editar(Long id, UsuarioRequest r, Actor a) {
    Usuario u = find(id);
    exigirVisible(u, a);
    validar(r);
    usuarios
        .findByEmail(r.getEmail().trim().toLowerCase())
        .filter(x -> !x.getId().equals(id))
        .ifPresent(x -> conflicto("El correo ya existe"));
    String rolActual = Actor.normalizar(u.getRol().getNombre());
    String nuevoRol = r.getRol() == null || r.getRol().isBlank()
        ? rolActual : Actor.normalizar(r.getRol());
    Long nuevoDueno = u.getDuenoId();
    if (a.es("ADMIN") && r.getRol() != null)
      nuevoDueno = nuevoRol.equals("TRABAJADOR") ? r.getDuenoId() : null;
    else if (r.getDuenoId() != null)
      nuevoDueno = r.getDuenoId();
    if (!rolActual.equals(nuevoRol) || !Objects.equals(nuevoDueno, u.getDuenoId())) {
      a.exigir("ADMIN");
      if (id.equals(a.id())) malo("No puedes cambiar tu propio rol o asignación");
      if (nuevoDueno != null && !nuevoRol.equals("TRABAJADOR"))
        malo("Solo un trabajador puede estar vinculado a un dueño");
      exigirSinVinculos(u);
      if (!rolActual.equals(nuevoRol)) u.setRol(rol(nuevoRol));
      u.setDuenoId(validarDueno(nuevoDueno));
    }
    u.setNombre(r.getNombre().trim());
    u.setEmail(r.getEmail().trim().toLowerCase());
    return mapper.toResponse(u);
  }

  public void eliminar(Long id, Actor a) {
    a.exigir("ADMIN");
    Usuario u = find(id);
    if (id.equals(a.id())) malo("No puedes eliminar ni desactivar tu propia cuenta");
    if ("ADMIN".equals(Actor.normalizar(u.getRol().getNombre()))) {
      u.setActivo(false);
      return;
    }
    String rolActual = Actor.normalizar(u.getRol().getNombre());
    if (List.of("DUENO", "TRABAJADOR").contains(rolActual)) {
      desvincularEnCatalogo(id);
      if (rolActual.equals("DUENO")) {
        usuarios.findAllByDuenoId(id).forEach(trabajador -> trabajador.setDuenoId(null));
      }
    }
    usuarios.delete(u);
  }

  private void desvincularEnCatalogo(Long id) {
    try {
      var factory = new JdkClientHttpRequestFactory(
          HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
      factory.setReadTimeout(Duration.ofSeconds(5));
      RestClient.builder().baseUrl(catalogoUrl).requestFactory(factory).build()
          .delete().uri("/internal/cabanas/asignaciones/{id}", id)
          .header("X-Internal-Key", internalKey)
          .retrieve().toBodilessEntity();
    } catch (RestClientException e) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudieron retirar las asignaciones de cabañas", e);
    }
  }

  private void exigirSinVinculos(Usuario u) {
    String rolActual = Actor.normalizar(u.getRol().getNombre());
    if (!List.of("DUENO", "TRABAJADOR").contains(rolActual)) return;
    if (rolActual.equals("DUENO") && usuarios.existsByDuenoId(u.getId()))
      conflicto("Reasigna los trabajadores de este dueño antes de continuar.");

    Boolean asignado;
    try {
      var factory = new JdkClientHttpRequestFactory(
          HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
      factory.setReadTimeout(Duration.ofSeconds(5));
      asignado = RestClient.builder().baseUrl(catalogoUrl).requestFactory(factory).build()
          .get().uri("/internal/cabanas/asignaciones/{id}", u.getId())
          .header("X-Internal-Key", internalKey)
          .retrieve().body(Boolean.class);
    } catch (RestClientException e) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "No se pudieron comprobar las asignaciones de cabañas", e);
    }
    if (asignado == null)
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Catálogo no devolvió las asignaciones de cabañas");
    if (asignado)
      conflicto("Retira o reasigna esta cuenta en sus cabañas antes de continuar.");
  }

  public UsuarioResponse password(Long id, String password, String actual, Actor a) {
    a.exigir("ADMIN", "CLIENTE", "TRABAJADOR");
    if (!id.equals(a.id())) prohibido();
    Usuario u = find(id);
    if (actual == null || !passwords.matches(actual, u.getPassword())) prohibido();
    if (password == null || password.length() < 8) malo("Usa al menos 8 caracteres");
    u.setPassword(passwords.encode(password));
    return mapper.toResponse(u);
  }

  private void exigirVisible(Usuario u, Actor a) {
    a.exigir("ADMIN", "CLIENTE", "TRABAJADOR");
    if (!a.es("ADMIN") && !u.getId().equals(a.id()))
      prohibido();
  }

  private Long validarDueno(Long id) {
    if (id == null) return null;
    Usuario u = find(id);
    if (!Actor.normalizar(u.getRol().getNombre()).equals("DUENO")
        || Boolean.FALSE.equals(u.getActivo())) malo("DueÃ±o invÃ¡lido");
    return id;
  }

  private Rol rol(String nombre) {
    if (!List.of("ADMIN", "DUENO", "TRABAJADOR", "CLIENTE").contains(nombre)) malo("Rol invÃ¡lido");
    return roles
        .findByNombre(nombre)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.CONFLICT, "Reinicia usuarios para inicializar roles"));
  }

  private Usuario find(Long id) {
    return usuarios
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
  }

  private void validar(UsuarioRequest r) {
    if (r.getNombre() == null
        || r.getNombre().isBlank()
        || r.getEmail() == null
        || !r.getEmail().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
      malo("Nombre y correo vÃ¡lido obligatorios");
  }

  private static void malo(String m) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
  }

  private static void conflicto(String m) {
    throw new ResponseStatusException(HttpStatus.CONFLICT, m);
  }

  private static void prohibido() {
    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AcciÃ³n no permitida");
  }
}
