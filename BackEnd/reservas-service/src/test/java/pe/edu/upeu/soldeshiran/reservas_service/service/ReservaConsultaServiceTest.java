package pe.edu.upeu.soldeshiran.reservas_service.service;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.reservas_service.client.PanelReservasClient;
import pe.edu.upeu.soldeshiran.reservas_service.client.PanelReservasClient.UsuarioResumen;
import pe.edu.upeu.soldeshiran.reservas_service.client.PanelReservasClient.CabanaResumen;
import pe.edu.upeu.soldeshiran.reservas_service.model.Reserva;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.config.import="})
class ReservaConsultaServiceTest {
    @Autowired ReservaRepository repository;
    PanelReservasClient client;
    ReservaConsultaService service;

    @BeforeEach
    void preparar() {
        client = mock(PanelReservasClient.class);
        service = new ReservaConsultaService(repository, client);
        when(client.consultarUsuarios(eq("Bearer admin"), isNull(), anyList())).thenReturn(List.of(
                new UsuarioResumen(1L, "Antonella", "antonella@gmail.com"),
                new UsuarioResumen(2L, "Carlos", "carlos@gmail.com")));
        when(client.consultarCabanas(eq("Bearer admin"), anyList())).thenReturn(List.of(
                new CabanaResumen(1L, "Familiar"), new CabanaResumen(2L, "Sol")));
        guardar(1L, 1L);
        guardar(1L, 2L);
        guardar(2L, 1L);
        repository.flush();
    }

    @Test
    void paginaEnBaseDeDatosConOrdenEstableYConsultasPorLote() {
        var primera = service.consultar("Bearer admin", null, null, 0, 2);
        var segunda = service.consultar("Bearer admin", null, null, 1, 2);
        assertThat(primera.totalElements()).isEqualTo(3);
        assertThat(primera.totalPages()).isEqualTo(2);
        assertThat(primera.content()).hasSize(2);
        assertThat(segunda.content()).hasSize(1);
        assertThat(primera.content().getFirst().idReserva())
                .isGreaterThan(segunda.content().getFirst().idReserva());
        assertThat(primera.content().getFirst().nombreUsuario()).isEqualTo("Carlos");
        assertThat(primera.content().getFirst().nombreCabana()).isEqualTo("Familiar");
        verify(client, times(2)).consultarUsuarios(eq("Bearer admin"), isNull(), anyList());
        verify(client, times(2)).consultarCabanas(eq("Bearer admin"), anyList());
    }

    @Test
    void combinaNombreOCorreoConCabanaAntesDePaginar() {
        when(client.consultarUsuarios("Bearer admin", "Antonella", null))
                .thenReturn(List.of(new UsuarioResumen(1L, "Antonella", "antonella@gmail.com")));
        var resultado = service.consultar("Bearer admin", "  Antonella  ", 2L, 0, 1);
        assertThat(resultado.totalElements()).isEqualTo(1);
        assertThat(resultado.content().getFirst().idUsuario()).isEqualTo(1L);
        assertThat(resultado.content().getFirst().idCabana()).isEqualTo(2L);
        verify(client, never()).consultarUsuarios(anyString(), isNull(), anyList());
    }

    @Test
    void filtraSoloCabanaYTrataBusquedaVaciaComoSinFiltro() {
        var resultado = service.consultar("Bearer admin", "   ", 1L, 0, 20);
        assertThat(resultado.totalElements()).isEqualTo(2);
        assertThat(resultado.content()).allMatch(r -> r.idCabana().equals(1L));
    }

    @Test
    void sinCoincidenciasNoConsultaCabanas() {
        when(client.consultarUsuarios("Bearer admin", "nadie", null)).thenReturn(List.of());
        var resultado = service.consultar("Bearer admin", "nadie", null, 0, 20);
        assertThat(resultado.totalElements()).isZero();
        assertThat(resultado.content()).isEmpty();
        verify(client, never()).consultarCabanas(anyString(), anyList());
    }

    @Test
    void paginaFueraDeRangoConservaElTotalSinConsultarResumenes() {
        var resultado = service.consultar("Bearer admin", null, null, 9, 2);
        assertThat(resultado.content()).isEmpty();
        assertThat(resultado.totalElements()).isEqualTo(3);
        verifyNoInteractions(client);
    }

    @Test
    void caidaDeUsuariosNoSePresentaComoListaVacia() {
        when(client.consultarUsuarios(anyString(), isNull(), anyList())).thenThrow(mock(FeignException.class));
        assertThatThrownBy(() -> service.consultar("Bearer admin", null, null, 0, 20))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    private void guardar(Long usuario, Long cabana) {
        Reserva r = new Reserva();
        r.setIdUsuario(usuario);
        r.setIdCabana(cabana);
        r.setFechaIngreso(LocalDate.of(2026, 12, 1));
        r.setFechaSalida(LocalDate.of(2026, 12, 2));
        r.setCantidadHuespedes(2);
        r.setTarifaNocheAplicada(new BigDecimal("100.00"));
        r.setMontoTotal(new BigDecimal("100.00"));
        r.setMontoAdelanto(new BigDecimal("50.00"));
        r.setSaldoPendiente(new BigDecimal("50.00"));
        r.setExpiraEn(LocalDateTime.of(2026, 12, 1, 12, 0));
        repository.save(r);
    }
}
