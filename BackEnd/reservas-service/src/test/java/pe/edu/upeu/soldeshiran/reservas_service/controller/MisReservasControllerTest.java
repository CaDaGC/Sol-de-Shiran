package pe.edu.upeu.soldeshiran.reservas_service.controller;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient;
import pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient.CabanaResumenCliente;
import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;
import pe.edu.upeu.soldeshiran.reservas_service.model.Reserva;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;
import pe.edu.upeu.soldeshiran.reservas_service.service.MisReservasService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.config.import="})
class MisReservasControllerTest {
    @Autowired ReservaRepository repository;
    CatalogoClient catalogo;
    MockMvc mvc;

    @BeforeEach
    void preparar() {
        catalogo = mock(CatalogoClient.class);
        when(catalogo.consultarResumen(anyList(), org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of(new CabanaResumenCliente(1L, "Familiar", "/cabana.jpg")));
        var servicio = new MisReservasService(repository, catalogo);
        org.springframework.test.util.ReflectionTestUtils.setField(servicio, "internalKey", "test-key");
        mvc = MockMvcBuilders.standaloneSetup(new MisReservasController(servicio)).build();
        guardar(5L, EstadoReserva.PENDIENTE_PAGO, 10, false);
        guardar(5L, EstadoReserva.CONFIRMADA, 10, false);
        guardar(5L, EstadoReserva.FINALIZADA, -10, false);
        guardar(5L, EstadoReserva.PENDIENTE_PAGO, 10, true);
        guardar(5L, EstadoReserva.CONFIRMADA, -10, false);
        guardar(9L, EstadoReserva.CONFIRMADA, 10, false);
        repository.flush();
    }

    @Test
    void devuelveSoloReservasDelPropietarioAunqueSeEnvieOtroId() throws Exception {
        mvc.perform(get("/api/v1/reservas/mis-reservas?idUsuario=9").requestAttr("actor", new pe.edu.upeu.soldeshiran.reservas_service.security.Actor(5L, "USUARIO")))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(5))
                .andExpect(jsonPath("content[*].idUsuario", everyItem(is(5))))
                .andExpect(jsonPath("content[*].nombreCabana", everyItem(is("Familiar"))));
        verify(catalogo).consultarResumen(org.mockito.ArgumentMatchers.eq(List.of(1L)), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void pendientesExcluyeVencidasYPermiteContinuarPago() throws Exception {
        mvc.perform(get("/api/v1/reservas/mis-reservas?grupo=PENDIENTES").requestAttr("actor", new pe.edu.upeu.soldeshiran.reservas_service.security.Actor(5L, "USUARIO")))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1))
                .andExpect(jsonPath("content[0].pagoDisponible").value(true));
    }

    @Test
    void confirmadasExcluyeEstanciasPasadas() throws Exception {
        mvc.perform(get("/api/v1/reservas/mis-reservas?grupo=CONFIRMADAS").requestAttr("actor", new pe.edu.upeu.soldeshiran.reservas_service.security.Actor(5L, "USUARIO")))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1))
                .andExpect(jsonPath("content[0].pagoDisponible").value(false));
    }

    @Test
    void historialIncluyeFinalizadasEstanciasPasadasYPendientesVencidas() throws Exception {
        mvc.perform(get("/api/v1/reservas/mis-reservas?grupo=PASADAS").requestAttr("actor", new pe.edu.upeu.soldeshiran.reservas_service.security.Actor(5L, "USUARIO")))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(3))
                .andExpect(jsonPath("content[*].pagoDisponible", everyItem(is(false))))
                .andExpect(jsonPath("content[*].estadoReserva", hasItem("EXPIRADA")));
    }

    @Test
    void paginaFueraDeRangoConservaTotalesYNoConsultaCatalogo() throws Exception {
        mvc.perform(get("/api/v1/reservas/mis-reservas?page=9&size=2").requestAttr("actor", new pe.edu.upeu.soldeshiran.reservas_service.security.Actor(5L, "USUARIO")))
                .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(5))
                .andExpect(jsonPath("content").isEmpty());
        verifyNoInteractions(catalogo);
    }

    @Test
    void validaIdentidadGrupoYPaginacion() throws Exception {
        mvc.perform(get("/api/v1/reservas/mis-reservas").header("X-User-Id", "5"))
                .andExpect(status().isUnauthorized());
        for (String query : List.of("page=-1", "size=0", "size=101", "page=2147483647&size=100", "grupo=INVALIDO")) {
            mvc.perform(get("/api/v1/reservas/mis-reservas?" + query).requestAttr("actor", new pe.edu.upeu.soldeshiran.reservas_service.security.Actor(5L, "USUARIO")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void caidaDelCatalogoNoSePresentaComoHistorialVacio() throws Exception {
        when(catalogo.consultarResumen(anyList(), org.mockito.ArgumentMatchers.anyString())).thenThrow(mock(FeignException.class));
        mvc.perform(get("/api/v1/reservas/mis-reservas").requestAttr("actor", new pe.edu.upeu.soldeshiran.reservas_service.security.Actor(5L, "USUARIO")))
                .andExpect(status().isServiceUnavailable());
    }

    private void guardar(Long usuario, EstadoReserva estado, int dias, boolean vencida) {
        var ahora = LocalDateTime.now(ZoneId.of("America/Lima"));
        var r = new Reserva();
        r.setIdUsuario(usuario); r.setIdCabana(1L);
        r.setFechaIngreso(ahora.toLocalDate().plusDays(dias)); r.setFechaSalida(r.getFechaIngreso().plusDays(2));
        r.setCantidadHuespedes(2); r.setTarifaNocheAplicada(new BigDecimal("100.00"));
        r.setMontoTotal(new BigDecimal("200.00")); r.setMontoAdelanto(new BigDecimal("100.00"));
        r.setSaldoPendiente(new BigDecimal("100.00")); r.setEstadoReserva(estado);
        r.setExpiraEn(vencida ? ahora.minusMinutes(1) : ahora.plusMinutes(15));
        repository.save(r);
    }
}
