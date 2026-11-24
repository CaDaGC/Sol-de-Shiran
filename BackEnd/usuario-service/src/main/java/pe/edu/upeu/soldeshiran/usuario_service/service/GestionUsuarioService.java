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
  private final PersonaService personas;

  @Value("${catalogo.url:http://localhost:8086}")
  private String catalogoUrl;

  @Value("${seguridad.internal-key}")
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
    if (usuarios.findByNombre(r.getNombre().strip()).isPresent())
      conflicto("El nombre de usuario ya existe");
    Usuario u = mapper.toEntity(r);
    u.setEmail(r.getEmail().trim().toLowerCase());
    if (r.getPassword() == null || (r.getPassword().length() < 8 || r.getPassword().length() > 72))
      malo("La contraseÃ±a debe tener al menos 8 caracteres");
    u.setPassword(passwords.encode(r.getPassword()));
    u.setRol(rol(rol));
    if (!rol.equals("ADMIN") || r.getPersona() != null)
      u.setPersona(personas.preparar(null, r.getPersona()));
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
    if (!rolActual.equals(nuevoRol)) {
      a.exigir("ADMIN");
      if (id.equals(a.id())) malo("No puedes cambiar tu propio rol");
      exigirSinVinculos(u);
      u.setRol(rol(nuevoRol));
    }
    if (r.getPersona() != null || (!nuevoRol.equals("ADMIN") && !rolActual.equals(nuevoRol)))
      u.setPersona(personas.preparar(u.getPersona(), r.getPersona()));
    usuarios.findByNombre(r.getNombre().strip()).filter(otro -> !otro.getId().equals(id))
        .ifPresent(otro -> conflicto("El nombre de usuario ya existe"));
    u.setNombre(r.getNombre().trim());
    u.setEmail(r.getEmail().trim().toLowerCase());
    return mapper.toResponse(u);
  }

  public UsuarioResponse cambiarEstado(Long id, boolean activo, Actor a) {
    a.exigir("ADMIN");
    Usuario u = find(id);
    if (id.equals(a.id()) && !activo) malo("No puedes desactivar tu propia cuenta");
    u.setActivo(activo);
    return mapper.toResponse(u);
  }

  public void eliminar(Long id, Actor a) {
    a.exigir("ADMIN");
    Usuario u = find(id);
    if (id.equals(a.id())) malo("No puedes eliminar ni desactivar tu propia cuenta");

    u.setActivo(false);
  }

  private void exigirSinVinculos(Usuario u) {
    String rolActual = Actor.normalizar(u.getRol().getNombre());
    if (!rolActual.equals("DUENO")) return;

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
        || r.getNombre().strip().length() > 80
        || r.getEmail() == null
        || r.getEmail().length() > 150
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
