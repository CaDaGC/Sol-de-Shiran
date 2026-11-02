package pe.edu.upeu.soldeshiran.catalogo_service.controller;

import lombok.RequiredArgsConstructor;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaCatalogoResponse;
import pe.edu.upeu.soldeshiran.catalogo_service.dto.CabanaDetalleResponse;
import pe.edu.upeu.soldeshiran.catalogo_service.service.CabanaService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cabanas")
@RequiredArgsConstructor
public class CabanaController {

    private final CabanaService cabanaService;

    @GetMapping
    public ResponseEntity<List<CabanaCatalogoResponse>> listarCabanas() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.APPLICATION_JSON)
                .body(cabanaService.listarCabanasActivas());
    }

    @GetMapping("/{idCabana}")
    public ResponseEntity<CabanaDetalleResponse> obtenerDetalle(
            @PathVariable Long idCabana
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(cabanaService.obtenerDetalleCabanaActiva(idCabana));
    }
}