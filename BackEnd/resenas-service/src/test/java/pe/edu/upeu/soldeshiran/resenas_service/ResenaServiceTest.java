package pe.edu.upeu.soldeshiran.resenas_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.resenas_service.client.ReservasClient;
import pe.edu.upeu.soldeshiran.resenas_service.client.UsuariosClient;
import pe.edu.upeu.soldeshiran.resenas_service.dto.ResenaRequest;
import pe.edu.upeu.soldeshiran.resenas_service.dto.IdentidadUsuarioResponse;
import pe.edu.upeu.soldeshiran.resenas_service.dto.AutorResenaResponse;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import pe.edu.upeu.soldeshiran.resenas_service.security.TokenVerifier;
import pe.edu.upeu.soldeshiran.resenas_service.service.ResenaService;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false", "spring.config.import=", "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:resenas", "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ResenaServiceTest {
    @Autowired MockMvc mvc;
    @Autowired
    ResenaService servicio;
    @MockitoBean TokenVerifier tokens;
    @MockitoBean
    UsuariosClient usuarios;
    @MockitoBean
    ReservasClient reservas;

    @Test
    void permiteUnaResenaPorEstanciaPropiaYListaElAutor() {
        when(reservas.estancias(eq(7L), eq(2L), anyString())).thenReturn(List.of(10L, 11L));
        when(reservas.estancias(eq(7L), eq(3L), anyString())).thenReturn(List.of());
        when(usuarios.autores(eq(List.of(7L)), anyString())).thenReturn(List.of(new AutorResenaResponse(7L, "Carlos")));

        assertEquals(10L, servicio.elegibilidad(2L, 7L).idReserva());
        var solicitud = new ResenaRequest(10L, 5, "Muy buena estancia");
        assertEquals(HttpStatus.FORBIDDEN, assertThrows(ResponseStatusException.class,
                () -> servicio.publicar(3L, 7L, solicitud)).getStatusCode());
        servicio.publicar(2L, 7L, solicitud);
        assertEquals(HttpStatus.CONFLICT, assertThrows(ResponseStatusException.class,
                () -> servicio.publicar(2L, 7L, solicitud)).getStatusCode());
        assertEquals(11L, servicio.elegibilidad(2L, 7L).idReserva());
        var opinion = servicio.listar(2L, 0).opiniones().getFirst();
        assertEquals("Carlos", opinion.autor());
        assertEquals(5, opinion.puntuacion());
        assertEquals("Muy buena estancia", opinion.comentario());
        assertTrue(servicio.listar(3L, 0).opiniones().isEmpty());
    }

    @Test
    void trabajadorNoPuedePublicar() throws Exception {
        when(tokens.verificar("Bearer token")).thenReturn(9L);
        when(usuarios.identidad(eq(9L), anyString())).thenReturn(new IdentidadUsuarioResponse("TRABAJADOR", true));
        mvc.perform(post("/api/v1/resenas/cabanas/2").servletPath("/api/v1/resenas/cabanas/2")
                .header("Authorization", "Bearer token")
                .contentType("application/json")
                .content("""
                        {"idReserva":10,"puntuacion":5,"comentario":"Comentario"}
                        """))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reservas);
    }
}

