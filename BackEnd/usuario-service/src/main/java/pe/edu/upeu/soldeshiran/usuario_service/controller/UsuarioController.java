package pe.edu.upeu.soldeshiran.usuario_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.soldeshiran.usuario_service.dto.*;
import pe.edu.upeu.soldeshiran.usuario_service.security.Actor;
import pe.edu.upeu.soldeshiran.usuario_service.service.GestionUsuarioService;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
  private final GestionUsuarioService gestion;

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioResponse register(@RequestBody UsuarioRequest r) {
    return gestion.crear(r, null, true);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioResponse crear(@RequestBody UsuarioRequest r, HttpServletRequest q) {
    return gestion.crear(r, Actor.desde(q), false);
  }

  @PostMapping("/admin")
  @ResponseStatus(HttpStatus.CREATED)
  public UsuarioResponse admin(@RequestBody UsuarioRequest r, HttpServletRequest q) {
    Actor a = Actor.desde(q);
    a.exigir("ADMIN");
    r.setRol("ADMIN");
    return gestion.crear(r, a, false);
  }

  @GetMapping
  public List<UsuarioResponse> listar(HttpServletRequest q) {
    return gestion.listar(Actor.desde(q));
  }

  @GetMapping("/{id}")
  public UsuarioResponse obtener(@PathVariable Long id, HttpServletRequest q) {
    return gestion.obtener(id, Actor.desde(q));
  }

  @GetMapping("/me")
  public UsuarioResponse perfil(HttpServletRequest q) {
    Actor a = Actor.desde(q);
    return gestion.obtener(a.id(), a);
  }

  @PutMapping("/{id}")
  public UsuarioResponse editar(
      @PathVariable Long id, @RequestBody UsuarioRequest r, HttpServletRequest q) {
    return gestion.editar(id, r, Actor.desde(q));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id, HttpServletRequest q) {
    gestion.eliminar(id, Actor.desde(q));
  }

  @PutMapping("/{id}/password")
  public UsuarioResponse password(
      @PathVariable Long id, @RequestBody PasswordUpdateRequest r, HttpServletRequest q) {
    return gestion.password(id, r.getPassword(), r.getPasswordActual(), Actor.desde(q));
  }
}
