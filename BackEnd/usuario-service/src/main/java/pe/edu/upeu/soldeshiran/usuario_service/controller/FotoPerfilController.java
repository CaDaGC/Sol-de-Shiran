package pe.edu.upeu.soldeshiran.usuario_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.usuario_service.security.Actor;
import pe.edu.upeu.soldeshiran.usuario_service.service.FotoPerfilService;

@RestController
@RequestMapping("/api/v1/usuarios/me/foto")
@RequiredArgsConstructor
public class FotoPerfilController {
  private final FotoPerfilService fotos;

  @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void guardar(@RequestParam("archivo") MultipartFile archivo, HttpServletRequest req) {
    fotos.guardar(archivo, Actor.desde(req));
  }

  @GetMapping
  public ResponseEntity<byte[]> obtener(HttpServletRequest req) {
    var usuario = fotos.obtener(Actor.desde(req));
    if (usuario.getFotoPerfil() == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Todavía no tienes foto de perfil.");
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .header("X-Content-Type-Options", "nosniff")
        .contentType(MediaType.parseMediaType(usuario.getTipoFotoPerfil()))
        .body(usuario.getFotoPerfil());
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void quitar(HttpServletRequest req) { fotos.quitar(Actor.desde(req)); }
}