package pe.edu.upeu.soldeshiran.usuario_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.soldeshiran.usuario_service.service.FotoPerfilService;

@RestController
@RequiredArgsConstructor
public class FotoPublicaController {
    private final FotoPerfilService fotos;
    @GetMapping("/api/v1/usuarios/fotos/{nombre}")
    public ResponseEntity<byte[]> obtener(@PathVariable String nombre) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(nombre.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
                .body(fotos.leerPublica(nombre));
    }
}
