package pe.upeu.edu.seguridad.controller;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import pe.upeu.edu.seguridad.dto.request.ActualizarAccesoRequest;
import pe.upeu.edu.seguridad.dto.request.CrearUsuarioRequest;
import pe.upeu.edu.seguridad.dto.request.EditarUsuarioRequest;
import pe.upeu.edu.seguridad.dto.response.UsuarioResponse;
import pe.upeu.edu.seguridad.entity.Usuario;
import pe.upeu.edu.seguridad.service.AdminService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/usuarios")
public class UsuarioController {
    private final AdminService admin;

    public UsuarioController(AdminService admin) {
        this.admin = admin;
    }

    @GetMapping
    public List<UsuarioResponse> listar(
            @AuthenticationPrincipal Usuario usuario,
            @RequestParam(defaultValue = "0") int pagina) {
        return admin.listar(usuario, pagina);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(
            @AuthenticationPrincipal Usuario usuario,
            @Valid @RequestBody CrearUsuarioRequest request) {
        return admin.crear(
                usuario, request.nombre(), request.email(), request.password(), request.rol());
    }

    @PatchMapping("/{id}/acceso")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void actualizarAcceso(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable long id,
            @Valid @RequestBody ActualizarAccesoRequest request) {
        admin.acceso(usuario, id, request.rol(), request.activo());
    }

    @PutMapping("/{id}")
    public UsuarioResponse editar(
            @AuthenticationPrincipal Usuario usuario,
            @PathVariable long id,
            @Valid @RequestBody EditarUsuarioRequest request) {
        return admin.editar(
                usuario, id, request.nombre(), request.email(), request.rol(), request.activo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@AuthenticationPrincipal Usuario usuario, @PathVariable long id) {
        admin.eliminar(usuario, id);
    }
}
