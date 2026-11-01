package pe.upeu.edu.seguridad.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SaludController {
    @GetMapping("/salud")
    public Map<String, String> salud() {
        return Map.of("servicio", "security-microservice", "estado", "disponible");
    }
}
