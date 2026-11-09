package pe.edu.upeu.soldeshiran.catalogo_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaRequest;
import pe.edu.upeu.soldeshiran.catalogo_service.security.Actor;
import pe.edu.upeu.soldeshiran.catalogo_service.service.GestionCabanaService;

@RestController
@RequestMapping("/api/v1/cabanas")
@RequiredArgsConstructor
public class GestionCabanaController {
  private final GestionCabanaService gestion;

  @GetMapping("/gestion")
  public List<Map<String, Object>> listar(HttpServletRequest q) {
    return gestion.listar(Actor.desde(q));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, Object> crear(@Valid @RequestBody CabanaRequest r, HttpServletRequest q) {
    return gestion.crear(r, Actor.desde(q));
  }

  @PutMapping("/{id}")
  public Map<String, Object> editar(
      @PathVariable Long id, @Valid @RequestBody CabanaRequest r, HttpServletRequest q) {
    return gestion.editar(id, r, Actor.desde(q));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void eliminar(@PathVariable Long id, HttpServletRequest q) {
    gestion.desactivar(id, Actor.desde(q));
  }

  @PutMapping("/{id}/activar")
  public Map<String, Object> activar(@PathVariable Long id, HttpServletRequest q) {
    return gestion.activar(id, Actor.desde(q));
  }

  @PutMapping("/{id}/trabajadores")
  public Map<String, Object> asignar(
      @PathVariable Long id, @RequestBody Set<Long> ids, HttpServletRequest q) {
    return gestion.asignar(id, ids, Actor.desde(q));
  }
}
