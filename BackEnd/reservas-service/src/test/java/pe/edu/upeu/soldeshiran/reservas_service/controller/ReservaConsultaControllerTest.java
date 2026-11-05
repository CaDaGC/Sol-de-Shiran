package pe.edu.upeu.soldeshiran.reservas_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upeu.soldeshiran.reservas_service.dto.ReservasPaginaResponse;
import pe.edu.upeu.soldeshiran.reservas_service.service.ReservaConsultaService;
import pe.edu.upeu.soldeshiran.reservas_service.service.ReservaService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservaConsultaControllerTest {
    MockMvc mvc;
    ReservaConsultaService consultas;

    @BeforeEach
    void preparar() {
        consultas = mock(ReservaConsultaService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ReservaController(mock(ReservaService.class), consultas)).build();
    }

    @Test
    void parametrosOpcionalesTienenValoresPorDefecto() throws Exception {
        when(consultas.consultar("Bearer admin", null, null, 0, 20))
                .thenReturn(new ReservasPaginaResponse(List.of(), 0, 20, 0, 0));
        mvc.perform(get("/api/v1/reservas").header("Authorization", "Bearer admin"))
                .andExpect(status().isOk()).andExpect(jsonPath("size").value(20));
    }

    @Test
    void parametrosInvalidosDevuelven400SinConsultar() throws Exception {
        for (String query : List.of("page=-1", "size=0", "size=101", "idCabana=0", "page=abc",
                "buscar=" + "a".repeat(101))) {
            mvc.perform(get("/api/v1/reservas?" + query).header("Authorization", "Bearer admin"))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(consultas);
    }
}
