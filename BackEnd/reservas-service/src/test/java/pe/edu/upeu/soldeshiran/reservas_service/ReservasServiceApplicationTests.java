package pe.edu.upeu.soldeshiran.reservas_service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient;
import pe.edu.upeu.soldeshiran.reservas_service.controller.GestionReservaController;
import pe.edu.upeu.soldeshiran.reservas_service.model.EstadoReserva;
import pe.edu.upeu.soldeshiran.reservas_service.model.Reserva;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;
import pe.edu.upeu.soldeshiran.reservas_service.security.Actor;

@ActiveProfiles("test")
@SpringBootTest(properties = "reservas.exportar-danos-cada-ms=3600000")
class ReservasServiceApplicationTests {

    @Autowired private ObjectMapper mapper;
    @Autowired private ReservaRepository reservas;
    @Autowired private pe.edu.upeu.soldeshiran.reservas_service.service.ReservaExpirationScheduler limpieza;
    @Autowired private GestionReservaController gestion;
    @MockitoBean private CatalogoClient catalogo;
    @MockitoBean private pe.edu.upeu.soldeshiran.reservas_service.client.PagoDanioClient pagos;
    @Autowired private pe.edu.upeu.soldeshiran.reservas_service.service.PagoDanioScheduler exportador;

    @Test
    void eliminaSoloExpiradasYPendientesImpagasVencidas() {
        var ahora = LocalDateTime.now();
        Long expirada = guardarReserva(EstadoReserva.EXPIRADA, ahora.plusMinutes(1), BigDecimal.ZERO);
        Long vencida = guardarReserva(EstadoReserva.PENDIENTE_PAGO, ahora, BigDecimal.ZERO);
        Long vigente = guardarReserva(EstadoReserva.PENDIENTE_PAGO, ahora.plusMinutes(15), BigDecimal.ZERO);
        Long conPago = guardarReserva(EstadoReserva.PENDIENTE_PAGO, ahora.minusMinutes(1), BigDecimal.ONE);
        Long confirmada = guardarReserva(EstadoReserva.CONFIRMADA, ahora.minusMinutes(1), BigDecimal.TEN);
        try {
            limpieza.eliminarReservasVencidas();
            assertFalse(reservas.existsById(expirada));
            assertFalse(reservas.existsById(vencida));
            assertTrue(reservas.existsById(vigente));
            assertTrue(reservas.existsById(conPago));
            assertTrue(reservas.existsById(confirmada));
        } finally {
            reservas.deleteAllById(java.util.List.of(expirada, vencida, vigente, conPago, confirmada));
        }
    }

    private Long guardarReserva(EstadoReserva estado, LocalDateTime vencimiento, BigDecimal pagado) {
        var r = new Reserva();
        r.setIdCabana(12L);
        r.setIdUsuario(5L);
        r.setEstadoReserva(estado);
        r.setCantidadHuespedes(2);
        r.setFechaIngreso(LocalDate.now());
        r.setFechaSalida(LocalDate.now().plusDays(1));
        r.setExpiraEn(vencimiento);
        r.setTarifaNocheAplicada(BigDecimal.TEN);
        r.setMontoTotal(BigDecimal.TEN);
        r.setMontoAdelanto(BigDecimal.ONE);
        r.setSaldoPendiente(BigDecimal.TEN.subtract(pagado));
        r.setTotalPagado(pagado);
        return reservas.saveAndFlush(r).getIdReserva();
    }

    @Test
    void checkoutValidaElCargoYRevierteElRegistroCuandoFallaCatalogo() throws Exception {
        Reserva reserva = new Reserva();
        reserva.setIdCabana(12L);
        reserva.setIdUsuario(5L);
        reserva.setEstadoReserva(EstadoReserva.OCUPADA);
        reserva.setCantidadHuespedes(2);
        reserva.setFechaIngreso(LocalDate.now().minusDays(1));
        reserva.setFechaSalida(LocalDate.now());
        reserva.setExpiraEn(LocalDateTime.now().minusDays(1));
        reserva.setTarifaNocheAplicada(new BigDecimal("200"));
        reserva.setMontoTotal(new BigDecimal("200"));
        reserva.setMontoAdelanto(new BigDecimal("100"));
        reserva.setSaldoPendiente(BigDecimal.ZERO);
        Long id = reservas.saveAndFlush(reserva).getIdReserva();

        try {
            var mvc = MockMvcBuilders.standaloneSetup(gestion)
                    .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();
            var salida = post("/api/v1/reservas/{id}/check-out", id)
                    .requestAttr("actor", new Actor(1L, "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON);
            String cuerpo = """
                    {"danos":{"descripcion":"Toalla dañada","monto":25.50,"medioPago":"YAPE"}}
                    """;

            mvc.perform(salida.content(cuerpo.replace("25.50", "25.555")))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(catalogo);

            doThrow(mock(FeignException.class)).doNothing()
                    .when(catalogo).liberar(12L, id, "test-internal-token");
            mvc.perform(salida.content(cuerpo)).andExpect(status().isServiceUnavailable());
            var ocupada = reservas.findById(id).orElseThrow();
            assertEquals(EstadoReserva.OCUPADA, ocupada.getEstadoReserva());
            assertNull(ocupada.getCheckout());
            exportador.exportar();
            verifyNoInteractions(pagos);

            var respuesta = mvc.perform(salida.content(cuerpo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estadoReserva").value("FINALIZADA"))
                    .andExpect(jsonPath("$.checkout.montoDanos").value(25.50))
                    .andReturn().getResponse().getContentAsString();
            String fechaOriginal = JsonPath.read(respuesta, "$.checkout.fecha");
            var finalizada = reservas.findById(id).orElseThrow();
            assertEquals(EstadoReserva.FINALIZADA, finalizada.getEstadoReserva());
            assertEquals(new BigDecimal("25.50"), finalizada.getCheckout().montoDanos());

            mvc.perform(salida.content(cuerpo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.checkout.fecha").value(fechaOriginal));
            verify(catalogo, times(2)).liberar(12L, id, "test-internal-token");
            var cobro = new pe.edu.upeu.soldeshiran.reservas_service.client.PagoDanioClient.Cobro(
                    1L, "Toalla dañada", new BigDecimal("25.50"), "YAPE", finalizada.getCheckout().fecha());
            doThrow(mock(FeignException.class)).doNothing()
                    .when(pagos).registrar(id, "test-internal-token", cobro);
            exportador.exportar();
            assertNotEquals(Boolean.TRUE, reservas.findById(id).orElseThrow().getCheckoutPagoDanosExportado());
            exportador.exportar();
            assertEquals(Boolean.TRUE, reservas.findById(id).orElseThrow().getCheckoutPagoDanosExportado());
            exportador.exportar();
            verify(pagos, times(2)).registrar(id, "test-internal-token", cobro);
        } finally {
            reservas.deleteById(id);
        }
    }
}
