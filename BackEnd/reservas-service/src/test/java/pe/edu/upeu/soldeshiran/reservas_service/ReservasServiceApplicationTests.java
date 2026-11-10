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
@SpringBootTest
class ReservasServiceApplicationTests {

    @Autowired private ObjectMapper mapper;
    @Autowired private ReservaRepository reservas;
    @Autowired private GestionReservaController gestion;
    @MockitoBean private CatalogoClient catalogo;

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
        } finally {
            reservas.deleteById(id);
        }
    }
}
