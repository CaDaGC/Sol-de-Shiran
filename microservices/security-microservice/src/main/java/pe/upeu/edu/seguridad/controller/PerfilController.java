package pe.upeu.edu.seguridad.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import pe.upeu.edu.seguridad.entity.Usuario;

import java.util.Map;

@RestController
@RequestMapping("/api/propietario")
public class PerfilController {
    @GetMapping("/contexto")
    public Map<String, Object> contexto(@AuthenticationPrincipal Usuario usuario) {
        return Map.of(
                "usuarioId",
                usuario.id(),
                "rol",
                usuario.rol(),
                "alcance",
                "Solo unidades asignadas al propietario; el módulo de cabañas se implementará"
                        + " después.");
    }
}
