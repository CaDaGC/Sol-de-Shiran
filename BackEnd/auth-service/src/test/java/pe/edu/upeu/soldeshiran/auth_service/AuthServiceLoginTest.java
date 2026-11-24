package pe.edu.upeu.soldeshiran.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.auth_service.client.UsuarioClient;
import pe.edu.upeu.soldeshiran.auth_service.dto.LoginRequest;
import pe.edu.upeu.soldeshiran.auth_service.dto.UsuarioResponse;
import pe.edu.upeu.soldeshiran.auth_service.security.JwtService;
import pe.edu.upeu.soldeshiran.auth_service.service.impl.AuthServiceImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceLoginTest {
  private final UsuarioClient usuarios = mock(UsuarioClient.class);
  private final JwtService jwt = mock(JwtService.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final AuthServiceImpl service = new AuthServiceImpl(usuarios, jwt, passwordEncoder);

  @Test
  void normalizaEmailYDevuelveTokenYPerfilPublico() {
    ReflectionTestUtils.setField(service, "internalKey", "internal");
    UsuarioResponse usuario = usuarioActivo();
    when(usuarios.findByEmail("ana@example.com", "internal")).thenReturn(usuario);
    when(passwordEncoder.matches("Segura123", "hash")).thenReturn(true);
    when(jwt.generateToken(usuario)).thenReturn("jwt-de-prueba");

    LoginRequest request = new LoginRequest();
    request.setEmail(" ANA@Example.com ");
    request.setPassword("Segura123");
    var respuesta = service.login(request);

    assertEquals("jwt-de-prueba", respuesta.getToken());
    assertEquals(7L, respuesta.getUserId());
    assertEquals("Ana", respuesta.getNombre());
    assertEquals("ana@example.com", respuesta.getEmail());
    assertEquals("CLIENTE", respuesta.getRol());
    verify(passwordEncoder).matches("Segura123", "hash");
  }

  @Test
  void rechazaPasswordIncorrectoSinEmitirToken() {
    ReflectionTestUtils.setField(service, "internalKey", "internal");
    UsuarioResponse usuario = usuarioActivo();
    when(usuarios.findByEmail("ana@example.com", "internal")).thenReturn(usuario);
    when(passwordEncoder.matches("incorrecta", "hash")).thenReturn(false);
    LoginRequest request = new LoginRequest();
    request.setEmail("ana@example.com");
    request.setPassword("incorrecta");

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
            () -> service.login(request));
    assertEquals(401, error.getStatusCode().value());
    verifyNoInteractions(jwt);
  }

  private UsuarioResponse usuarioActivo() {
    UsuarioResponse usuario = new UsuarioResponse();
    usuario.setId(7L);
    usuario.setNombre("Ana");
    usuario.setEmail("ana@example.com");
    usuario.setPassword("hash");
    usuario.setRol("CLIENTE");
    usuario.setActivo(true);
    return usuario;
  }
}
