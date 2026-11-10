package pe.edu.upeu.soldeshiran.reservas_service.controller;

import feign.FeignException;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import pe.edu.upeu.soldeshiran.reservas_service.security.Actor;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import pe.edu.upeu.soldeshiran.reservas_service.client.PanelReservasClient;
import pe.edu.upeu.soldeshiran.reservas_service.client.PanelReservasClient.CabanaResumen;
import pe.edu.upeu.soldeshiran.reservas_service.client.PanelReservasClient.UsuarioResumen;
import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;
import pe.edu.upeu.soldeshiran.reservas_service.model.Reserva;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;
import pe.edu.upeu.soldeshiran.reservas_service.service.ReservaConsultaService;
import pe.edu.upeu.soldeshiran.reservas_service.service.ReservaService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservaDetalleControllerTest {
    private ReservaRepository repository;
    private PanelReservasClient client;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        repository = mock(ReservaRepository.class);
        client = mock(PanelReservasClient.class);
        var consultas = new ReservaConsultaService(repository, client);
        ReflectionTestUtils.setField(consultas, "internalKey", "test-key");
        var mapper = Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
        mvc = MockMvcBuilders.standaloneSetup(new ReservaController(mock(ReservaService.class), consultas))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();

        Reserva reserva = new Reserva();
        reserva.setIdReserva(17L);
        reserva.setIdUsuario(5L);
        reserva.setIdCabana(3L);
        reserva.setFechaIngreso(LocalDate.of(2026, 12, 1));
        reserva.setFechaSalida(LocalDate.of(2026, 12, 3));
        reserva.setCantidadHuespedes(2);
        reserva.setTarifaNocheAplicada(new BigDecimal("100.00"));
        reserva.setMontoTotal(new BigDecimal("200.00"));
        reserva.setMontoAdelanto(new BigDecimal("100.00"));
        reserva.setSaldoPendiente(new BigDecimal("100.00"));
        reserva.setEstadoReserva(EstadoReserva.CONFIRMADA);
        reserva.setExpiraEn(LocalDateTime.of(2026, 11, 5, 12, 15));
        reserva.setFechaCreacion(LocalDateTime.of(2026, 11, 5, 12, 0));
        when(repository.findById(17L)).thenReturn(Optional.of(reserva));
        when(client.consultarUsuarios("test-key", null, List.of(5L)))
                .thenReturn(List.of(new UsuarioResumen(5L, "Antonella", "antonella@example.com")));
        when(client.consultarCabanas("test-key", List.of(3L)))
                .thenReturn(List.of(new CabanaResumen(3L, "Familiar")));
    }

    @Test
    void devuelveInformacionCompletaSinModificarLaReserva() throws Exception {
        mvc.perform(get("/api/v1/reservas/panel/17").requestAttr("actor", new Actor(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("idReserva").value(17))
                .andExpect(jsonPath("idUsuario").value(5))
                .andExpect(jsonPath("nombreUsuario").value("Antonella"))
                .andExpect(jsonPath("emailUsuario").value("antonella@example.com"))
                .andExpect(jsonPath("idCabana").value(3))
                .andExpect(jsonPath("nombreCabana").value("Familiar"))
                .andExpect(jsonPath("fechaIngreso").value("2026-12-01"))
                .andExpect(jsonPath("fechaSalida").value("2026-12-03"))
                .andExpect(jsonPath("cantidadHuespedes").value(2))
                .andExpect(jsonPath("tarifaNocheAplicada").value(100))
                .andExpect(jsonPath("montoTotal").value(200))
                .andExpect(jsonPath("montoAdelanto").value(100))
                .andExpect(jsonPath("saldoPendiente").value(100))
                .andExpect(jsonPath("estadoReserva").value("CONFIRMADA"))
                .andExpect(jsonPath("fechaCreacion").exists())
                .andExpect(jsonPath("expiraEn").exists());
        verify(repository, never()).save(any());
        verify(client).consultarUsuarios("test-key", null, List.of(5L));
        verify(client).consultarCabanas("test-key", List.of(3L));
    }

    @Test
    void idInvalidoDevuelve400SinConsultarServicios() throws Exception {
        for (String id : List.of("0", "-1", "abc", "9223372036854775808")) {
            mvc.perform(get("/api/v1/reservas/panel/" + id).requestAttr("actor", new Actor(1L, "ADMIN")))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(repository, client);
    }

    @Test
    void reservaInexistenteDevuelve404AntesDeConsultarUsuarios() throws Exception {
        mvc.perform(get("/api/v1/reservas/panel/999").requestAttr("actor", new Actor(1L, "ADMIN")))
                .andExpect(status().isNotFound());
        verifyNoInteractions(client);
    }

    @Test
    void conservaReservaHistoricaSiUsuarioOCabanaYaNoExisten() throws Exception {
        when(client.consultarUsuarios("test-key", null, List.of(5L))).thenReturn(List.of());
        when(client.consultarCabanas("test-key", List.of(3L))).thenReturn(List.of());
        mvc.perform(get("/api/v1/reservas/panel/17").requestAttr("actor", new Actor(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("idUsuario").value(5))
                .andExpect(jsonPath("idCabana").value(3))
                .andExpect(jsonPath("nombreUsuario").isEmpty())
                .andExpect(jsonPath("nombreCabana").isEmpty());
    }

    @Test
    void caidaDeDependenciaDevuelve503EnLugarDeDetalleIncompleto() throws Exception {
        when(client.consultarUsuarios("test-key", null, List.of(5L)))
                .thenThrow(mock(FeignException.class));
        mvc.perform(get("/api/v1/reservas/panel/17").requestAttr("actor", new Actor(1L, "ADMIN")))
                .andExpect(status().isServiceUnavailable());
    }
}
