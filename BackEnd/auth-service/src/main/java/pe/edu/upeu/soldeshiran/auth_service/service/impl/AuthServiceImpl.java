package pe.edu.upeu.soldeshiran.auth_service.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pe.edu.upeu.soldeshiran.auth_service.client.UsuarioClient;
import pe.edu.upeu.soldeshiran.auth_service.dto.AuthResponse;
import pe.edu.upeu.soldeshiran.auth_service.dto.LoginRequest;
import pe.edu.upeu.soldeshiran.auth_service.dto.UsuarioResponse;
import pe.edu.upeu.soldeshiran.auth_service.security.JwtService;
import pe.edu.upeu.soldeshiran.auth_service.service.AuthService;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UsuarioClient usuarioClient;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  @org.springframework.beans.factory.annotation.Value(
      "${seguridad.internal-key:sol-shiran-interno-desarrollo-cambiar}")
  private String internalKey;

  @Override
  public AuthResponse login(LoginRequest request) {
    // 1. Buscar usuario por correo mediante Feign Client
    UsuarioResponse usuario;
    try {
      usuario = usuarioClient.findByEmail(request.getEmail().trim().toLowerCase(), internalKey);
    } catch (feign.FeignException.NotFound ex) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
    }

    // 2. Validar si el usuario existe para evitar NullPointerException
    if (usuario == null || !usuario.isActivo()) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
    }

    // 3. Validar contraseña usando BCrypt
    if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
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
