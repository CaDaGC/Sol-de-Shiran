package pe.edu.upeu.soldeshiran.auth_service.service.impl;

import pe.edu.upeu.soldeshiran.auth_service.client.UsuarioClient;
import pe.edu.upeu.soldeshiran.auth_service.dto.AuthResponse;
import pe.edu.upeu.soldeshiran.auth_service.dto.LoginRequest;
import pe.edu.upeu.soldeshiran.auth_service.dto.UsuarioResponse;
import pe.edu.upeu.soldeshiran.auth_service.security.JwtService;
import pe.edu.upeu.soldeshiran.auth_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UsuarioClient usuarioClient;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse login(LoginRequest request) {
        // 1. Buscar usuario por correo mediante Feign Client
        UsuarioResponse usuario = usuarioClient.findByEmail(request.getEmail());

        // 2. Validar si el usuario existe para evitar NullPointerException
        if (usuario == null) {
            throw new RuntimeException("Usuario no encontrado");
        }

        // 3. Validar contraseña usando BCrypt
        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        // 4. Construir y retornar la respuesta con el Token JWT y datos del usuario
        AuthResponse response = new AuthResponse();
        response.setToken(jwtService.generateToken(usuario));
        response.setUserId(usuario.getId());
        response.setNombre(usuario.getNombre());
        response.setEmail(usuario.getEmail());
        response.setRol(usuario.getRol());

        return response;
    }
}