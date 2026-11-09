package pe.edu.upeu.soldeshiran.catalogo_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.soldeshiran.catalogo_service.security.Actor;
import pe.edu.upeu.soldeshiran.catalogo_service.service.GestionCabanaService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/cabanas")
public class InternalCabanaController {
  private final GestionCabanaService gestion;

  @GetMapping("/{id}/acceso")
  public Access acceso(
      @PathVariable Long id, @RequestParam Long usuarioId, @RequestParam String rol) {
    var c = gestion.obtener(id);
    Actor a = new Actor(usuarioId, Actor.normalizar(rol));
    boolean ver = gestion.puedeVer(c, a);
    boolean modificar =
        a.es("ADMIN") || (a.es("DUENO") && java.util.Objects.equals(c.getDuenoId(), a.id()));
    return new Access(ver, modificar);
  }

  @GetMapping("/asignaciones/{usuarioId}")
  public boolean tieneAsignaciones(@PathVariable Long usuarioId) {
    return gestion.tieneAsignaciones(usuarioId);
  }

  @DeleteMapping("/asignaciones/{usuarioId}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  public void desvincularCuenta(@PathVariable Long usuarioId) {
    gestion.desvincularCuenta(usuarioId);
  }

  public record Access(boolean operativo, boolean propietario) {}
}
