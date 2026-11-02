package pe.edu.upeu.soldeshiran.catalogo_service.controller;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaCatalogoResponse;
import pe.edu.upeu.soldeshiran.catalogo_service.service.CabanaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cabanas")
@RequiredArgsConstructor
public class CabanaController {

    private final CabanaService cabanaService;

    @GetMapping
    public ResponseEntity<List<CabanaCatalogoResponse>> listarCabanas() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                .contentType(MediaType.APPLICATION_JSON)
                .body(cabanaService.listarCabanasActivas());
    }
}
