package pe.edu.upeu.soldeshiran.restaurante_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.upeu.soldeshiran.restaurante_service.security.Actor;
import pe.edu.upeu.soldeshiran.restaurante_service.service.FotoMenuService;

@RestController
@RequestMapping("/api/restaurante")
@RequiredArgsConstructor
public class FotoMenuController {
    private final FotoMenuService servicio;

    @PostMapping(value = "/admin/{tipo:locales|productos}/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> guardar(@PathVariable("tipo") String tipo, @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile archivo, HttpServletRequest request) {
        return Map.of("imagenUrl", servicio.guardar(tipo, id, archivo, Actor.desde(request)));
    }

    @DeleteMapping("/admin/{tipo:locales|productos}/{id}/foto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void quitar(@PathVariable("tipo") String tipo, @PathVariable("id") Long id, HttpServletRequest request) {
        servicio.quitar(tipo, id, Actor.desde(request));
    }

    @GetMapping("/imagenes/{tipo:locales|productos}/{id}")
    public ResponseEntity<byte[]> leer(@PathVariable("tipo") String tipo, @PathVariable("id") Long id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff").body(servicio.leer(tipo, id));
    }
}
