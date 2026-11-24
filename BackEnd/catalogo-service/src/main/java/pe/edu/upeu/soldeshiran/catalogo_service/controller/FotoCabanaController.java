package pe.edu.upeu.soldeshiran.catalogo_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.soldeshiran.catalogo_service.security.Actor;
import pe.edu.upeu.soldeshiran.catalogo_service.service.FotoCabanaService;

@RestController
@RequestMapping("/api/v1/cabanas/fotos")
@RequiredArgsConstructor
public class FotoCabanaController {
  private final FotoCabanaService fotos;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, String> subir(@RequestParam("archivo") MultipartFile archivo,
      HttpServletRequest request) {
    var foto = fotos.guardar(archivo, Actor.desde(request));
    return Map.of("url", "/api/v1/cabanas/fotos/" + foto.getId());
  }

  @GetMapping("/{id}")
  public ResponseEntity<byte[]> ver(@PathVariable String id) {
    var foto = fotos.obtener(id);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.getTipo()))
        .header("X-Content-Type-Options", "nosniff")
        .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)).cachePublic())
        .body(fotos.contenido(foto));
  }
}