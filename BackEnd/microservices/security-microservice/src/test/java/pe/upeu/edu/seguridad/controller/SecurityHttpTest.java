package pe.upeu.edu.seguridad.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import pe.upeu.edu.seguridad.config.SecurityConfig;
import pe.upeu.edu.seguridad.entity.Usuario;
import pe.upeu.edu.seguridad.enums.Rol;
import pe.upeu.edu.seguridad.exception.GlobalExceptionHandler;
import pe.upeu.edu.seguridad.security.RateLimitFilter;
import pe.upeu.edu.seguridad.security.TokenAuthenticationFilter;
import pe.upeu.edu.seguridad.service.AdminService;
import pe.upeu.edu.seguridad.service.AuthService;

import java.util.Optional;

@WebMvcTest(
        controllers = {
            AuthController.class,
            UsuarioController.class,
            PerfilController.class,
            SaludController.class
        },
        properties = "app.cors-origin=http://localhost:4200")
@Import({
    SecurityConfig.class,
    TokenAuthenticationFilter.class,
    RateLimitFilter.class,
    GlobalExceptionHandler.class
})
class SecurityHttpTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService auth;
    @MockitoBean AdminService admin;

    void token(Rol rol) {
        when(auth.autenticar("valido"))
                .thenReturn(
                        Optional.of(
                                new Usuario(
                                        4,
                                        "Usuario",
                                        "u@example.com",
                                        "hash",
                                        rol,
                                        true,
                                        0,
                                        null)));
    }

    @Test
    void saludEsPublica() throws Exception {
        mvc.perform(get("/api/salud")).andExpect(status().isOk());
    }

    @Test
    void perfilSinTokenEs401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoPuedeListarUsuarios() throws Exception {
        token(Rol.CLIENTE);
        mvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer valido"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(admin);
    }

    @Test
    void propietarioNoPuedeCrearPersonal() throws Exception {
        token(Rol.PROPIETARIO);
        mvc.perform(
                        post("/api/admin/usuarios")
                                .header("Authorization", "Bearer valido")
                                .contentType("application/json")
                                .content(
                                        "{\"nombre\":\"Otro\",\"email\":\"otro@example.com\",\"password\":\"Password-seguro-123\",\"rol\":\"ADMIN_GENERAL\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(admin);
    }

    @Test
    void clienteNoPuedeVerContextoPropietario() throws Exception {
        token(Rol.CLIENTE);
        mvc.perform(get("/api/propietario/contexto").header("Authorization", "Bearer valido"))
                .andExpect(status().isForbidden());
    }

    @Test
    void propietarioObtieneSoloSuIdentidad() throws Exception {
        token(Rol.PROPIETARIO);
        mvc.perform(get("/api/propietario/contexto").header("Authorization", "Bearer valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(4));
    }

    @Test
    void perfilNoExponeHash() throws Exception {
        token(Rol.CLIENTE);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void loginValidaCorreo() throws Exception {
        mvc.perform(
                        post("/api/auth/login")
                                .contentType("application/json")
                                .content("{\"email\":\"invalido\",\"password\":\"algo\"}"))
                .andExpect(status().isBadRequest());
        verify(auth, never()).login(anyString(), anyString());
    }

    @Test
    void registroIgnoraElRolSolicitado() throws Exception {
        mvc.perform(
                        post("/api/auth/registro")
                                .contentType("application/json")
                                .content(
                                        "{\"nombre\":\"Cliente\",\"email\":\"c@example.com\",\"password\":\"Password-seguro-123\",\"rol\":\"ADMIN_GENERAL\"}"))
                .andExpect(status().isCreated());
        verify(auth).registrar("Cliente", "c@example.com", "Password-seguro-123");
        verifyNoInteractions(admin);
    }

    @Test
    void tokenInvalidoEs401() throws Exception {
        when(auth.autenticar("invalido")).thenReturn(Optional.empty());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutExigeSesion() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void origenDesconocidoEsRechazado() throws Exception {
        mvc.perform(
                        options("/api/auth/login")
                                .header("Origin", "https://otro.example")
                                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
