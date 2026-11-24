package pe.edu.upeu.soldeshiran.auth_service;

import feign.FeignException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upeu.soldeshiran.auth_service.client.UsuarioClient;
import pe.edu.upeu.soldeshiran.auth_service.controller.AuthController;
import pe.edu.upeu.soldeshiran.auth_service.dto.RegistroRequest;
import pe.edu.upeu.soldeshiran.auth_service.dto.RegistroResponse;
import pe.edu.upeu.soldeshiran.auth_service.security.JwtService;
import pe.edu.upeu.soldeshiran.auth_service.service.impl.AuthServiceImpl;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RegistroTest {
  private static final String URL = "/api/v1/auth/register";
  private static final String DATOS = """
      {"nombre":" Ana ","email":"ANA@example.com","password":"Segura123","rol":"ADMIN"}
      """;
  private final UsuarioClient usuarios = mock(UsuarioClient.class);
  private final RegistroRequest normalizado = new RegistroRequest("Ana", "ana@example.com", "Segura123");
  private final org.springframework.test.web.servlet.MockMvc mvc = MockMvcBuilders.standaloneSetup(
      new AuthController(new AuthServiceImpl(usuarios, mock(JwtService.class), mock(PasswordEncoder.class))))
      .build();

  @Test
  void validaNormalizaYDevuelveSoloDatosPublicos() throws Exception {
    when(usuarios.register(normalizado))
        .thenReturn(new RegistroResponse(1L, "Ana", "ana@example.com", "CLIENTE"));
    mvc.perform(post(URL).contentType("application/json").content(DATOS))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rol").value("CLIENTE"))
        .andExpect(jsonPath("$.password").doesNotExist());
    verify(usuarios).register(normalizado);
  }

  @Test
  void rechazaPasswordInvalidoAntesDeGuardar() throws Exception {
    mvc.perform(post(URL).contentType("application/json").content(DATOS.replace("Segura123", "debil")))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(usuarios);
  }

  @Test
  void conservaElConflictoDelServicioDeUsuarios() throws Exception {
    when(usuarios.register(normalizado)).thenThrow(mock(FeignException.Conflict.class));
    mvc.perform(post(URL).contentType("application/json").content(DATOS))
        .andExpect(status().isConflict());
  }
}
