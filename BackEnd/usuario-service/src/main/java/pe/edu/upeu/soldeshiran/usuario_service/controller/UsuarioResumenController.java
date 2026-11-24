package pe.edu.upeu.soldeshiran.usuario_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioResumenResponse;
import pe.edu.upeu.soldeshiran.usuario_service.repository.UsuarioRepository;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/internal/usuarios/resumen")
@RequiredArgsConstructor
public class UsuarioResumenController {
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public List<UsuarioResumenResponse> consultar(
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) List<Long> ids) {
        if (buscar != null && !buscar.isBlank()) {
            if (buscar.length() > 100 || ids != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Búsqueda inválida.");
            }
            String patron = buscar.strip().toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_");
            return usuarioRepository.buscarResumen("%" + patron + "%");
        }
        if (ids == null || ids.isEmpty() || ids.size() > 100
                || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica de 1 a 100 IDs válidos.");
        }
        return usuarioRepository.resumirPorIds(ids);
    }
}
