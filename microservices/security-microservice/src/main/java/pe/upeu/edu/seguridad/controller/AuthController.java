package pe.upeu.edu.seguridad.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import pe.upeu.edu.seguridad.dto.request.CambiarPasswordRequest;
import pe.upeu.edu.seguridad.dto.request.LoginRequest;
import pe.upeu.edu.seguridad.dto.request.RecuperacionRequest;
import pe.upeu.edu.seguridad.dto.request.RegistroRequest;
import pe.upeu.edu.seguridad.dto.request.RestablecerPasswordRequest;
import pe.upeu.edu.seguridad.dto.response.LoginResponse;
import pe.upeu.edu.seguridad.dto.response.UsuarioResponse;
import pe.upeu.edu.seguridad.entity.Usuario;
import pe.upeu.edu.seguridad.mapper.UsuarioMapper;
import pe.upeu.edu.seguridad.service.AuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.email(), request.password());
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest request) {
        return auth.registrar(request.nombre(), request.email(), request.password());
    }

    @GetMapping("/me")
    public UsuarioResponse perfil(@AuthenticationPrincipal Usuario usuario) {
        return UsuarioMapper.toResponse(usuario);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @AuthenticationPrincipal Usuario usuario,
            @RequestHeader("Authorization") String header) {
        auth.logout(header.substring(7), usuario);
    }

    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPassword(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CambiarPasswordRequest request) {
        auth.cambiarPassword(usuario, request.actual(), request.nueva());
    }

    @PostMapping("/recuperacion")
    public Map<String, String> recuperar(@Valid @RequestBody RecuperacionRequest request) {
        auth.solicitarRecuperacion(request.email());
        return Map.of(
                "message",
                "Si existe una cuenta activa con ese correo, recibirás un enlace de recuperación.");
    }

    @PostMapping("/restablecer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void restablecer(@Valid @RequestBody RestablecerPasswordRequest request) {
        auth.restablecer(request.codigo(), request.password());
    }
}
