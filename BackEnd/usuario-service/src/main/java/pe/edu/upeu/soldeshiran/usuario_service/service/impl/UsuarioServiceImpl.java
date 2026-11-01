package pe.edu.upeu.soldeshiran.usuario_service.service.impl;

import pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioRequest;
import pe.edu.upeu.soldeshiran.usuario_service.dto.UsuarioResponse;
import pe.edu.upeu.soldeshiran.usuario_service.mapper.UsuarioMapper;
import pe.edu.upeu.soldeshiran.usuario_service.model.Rol;
import pe.edu.upeu.soldeshiran.usuario_service.model.Usuario;
import pe.edu.upeu.soldeshiran.usuario_service.repository.RolRepository;
import pe.edu.upeu.soldeshiran.usuario_service.repository.UsuarioRepository;
import pe.edu.upeu.soldeshiran.usuario_service.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final RolRepository rolRepository;

    @Override
    public UsuarioResponse register(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        return createUser(request, "CLIENTE");
    }

    @Override
    public UsuarioResponse createAdmin(UsuarioRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("El email ya está registrado");
        }

        return createUser(request, "ADMIN");
    }

    private UsuarioResponse createUser(UsuarioRequest request, String nombreRol) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("La contraseña es obligatoria");
        }

        Rol rol = rolRepository.findByNombre(nombreRol)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + nombreRol));

        Usuario usuario = usuarioMapper.toEntity(request);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setRol(rol);

        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    public UsuarioResponse findByEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return usuarioMapper.toResponse(usuario);
    }

    @Override
    public UsuarioResponse findById(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return usuarioMapper.toResponse(usuario);
    }

    @Override
    public List<UsuarioResponse> findAll() {
        return usuarioRepository.findAll()
                .stream()
                .map(usuarioMapper::toResponse)
                .toList();
    }

    @Override
    public UsuarioResponse update(Long id, UsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuarioRepository.findByEmail(request.getEmail())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new RuntimeException("El email ya está registrado");
                });

        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());

        if (request.getRol() != null && !request.getRol().isBlank()) {
            Rol rol = rolRepository.findByNombre(request.getRol())
                    .orElseThrow(() -> new RuntimeException("Rol no encontrado: " + request.getRol()));
            usuario.setRol(rol);
        }

        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

    @Override
    public UsuarioResponse updatePassword(Long id, String password) {
        if (password == null || password.isBlank()) {
            throw new RuntimeException("La contraseña es obligatoria");
        }

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setPassword(passwordEncoder.encode(password));

        return usuarioMapper.toResponse(usuarioRepository.save(usuario));
    }

}
