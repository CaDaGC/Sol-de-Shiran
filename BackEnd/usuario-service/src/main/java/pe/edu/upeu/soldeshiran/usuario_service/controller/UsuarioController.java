package pe.edu.upeu.soldeshiran.usuario_service.controller;

import pe.edu.upeu.soldeshiran.usuario_service.dto.PasswordUpdateRequest;
import pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioRequest;
import pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioResponse;
import pe.edu.upeu.soldeshiran.usuario_service.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/register")
    public UsuarioResponse register(@RequestBody UsuarioRequest request) {
        return usuarioService.register(request);
    }

    @PostMapping("/admin")
    public UsuarioResponse createAdmin(@RequestBody UsuarioRequest request) {
        return usuarioService.createAdmin(request);
    }

    @GetMapping("/email/{email}")
    public UsuarioResponse findByEmail(@PathVariable String email) {
        return usuarioService.findByEmail(email);
    }

    @GetMapping("/{id}")
    public UsuarioResponse findById(@PathVariable Long id) {
        return usuarioService.findById(id);
    }

    @GetMapping
    public List<UsuarioResponse> findAll() {
        return usuarioService.findAll();
    }

    @PutMapping("/{id}")
    public UsuarioResponse update(@PathVariable Long id, @RequestBody UsuarioRequest request) {
        return usuarioService.update(id, request);
    }

    @PutMapping("/{id}/password")
    public UsuarioResponse updatePassword(@PathVariable Long id, @RequestBody PasswordUpdateRequest request) {
        return usuarioService.updatePassword(id, request.getPassword());
    }

}
