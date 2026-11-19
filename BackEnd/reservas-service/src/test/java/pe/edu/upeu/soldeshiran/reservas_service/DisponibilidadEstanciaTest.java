package pe.edu.upeu.soldeshiran.reservas_service;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.reservas_service.client.*;
import pe.edu.upeu.soldeshiran.reservas_service.dto.CrearReservaRequest;
import pe.edu.upeu.soldeshiran.reservas_service.model.*;
import pe.edu.upeu.soldeshiran.reservas_service.repository.ReservaRepository;
import pe.edu.upeu.soldeshiran.reservas_service.service.ReservaService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DisponibilidadEstanciaTest {
    @Autowired ReservaService servicio;
    @Autowired ReservaRepository repo;
    @MockitoBean CatalogoClient catalogo;
    @MockitoBean PagoDanioClient pagos;
    LocalDate entrada;
    @BeforeEach void preparar() {
        entrada = LocalDate.now().plusDays(20);
        when(catalogo.obtenerDetalle(8888L)).thenReturn(new CabanaDetalleClientResponse(8888L,"Cabaña",8,new BigDecimal("250")));
    }
    void reserva(EstadoReserva estado, LocalDateTime vence) {
        Reserva r=new Reserva(); r.setIdCabana(8888L); r.setIdUsuario(99L);
        r.setFechaIngreso(entrada); r.setFechaSalida(entrada.plusDays(3)); r.setCantidadHuespedes(2);
        r.setTarifaNocheAplicada(new BigDecimal("250")); r.setMontoTotal(new BigDecimal("750"));
        r.setMontoAdelanto(new BigDecimal("375")); r.setSaldoPendiente(new BigDecimal("375"));
        r.setEstadoReserva(estado); r.setExpiraEn(vence); repo.saveAndFlush(r);
    }
    @Test void confirmadaBloqueaAunqueElTiempoDePagoHayaVencido() {
        reserva(EstadoReserva.CONFIRMADA,LocalDateTime.now().minusDays(1));
        assertFalse(servicio.consultarDisponibilidad(8888L,entrada.plusDays(1),entrada.plusDays(2),2).disponible());
        assertThrows(ResponseStatusException.class,()->servicio.crearReservaTemporal(100L,
            new CrearReservaRequest(8888L,entrada.plusDays(1),entrada.plusDays(2),2)));
        assertEquals(List.of(entrada,entrada.plusDays(1),entrada.plusDays(2)),
            servicio.consultarFechasOcupadas(8888L,entrada,entrada.plusDays(4)).fechasOcupadas());
    }
    @Test void ocupadaBloqueaYPermiteReservarDesdeSuSalida() {
        reserva(EstadoReserva.OCUPADA,LocalDateTime.now().minusDays(1));
        assertFalse(servicio.consultarDisponibilidad(8888L,entrada,entrada.plusDays(1),2).disponible());
        assertTrue(servicio.consultarDisponibilidad(8888L,entrada.plusDays(3),entrada.plusDays(4),2).disponible());
    }
    @Test void temporalVigenteBloqueaYTemporalVencidaLibera() {
        reserva(EstadoReserva.PENDIENTE_PAGO,LocalDateTime.now().plusMinutes(15));
        assertFalse(servicio.consultarDisponibilidad(8888L,entrada,entrada.plusDays(1),2).disponible());
        repo.findAll().stream().filter(r->r.getIdCabana().equals(8888L)).forEach(r->r.setExpiraEn(LocalDateTime.now().minusMinutes(1)));
        repo.flush();
        assertTrue(servicio.consultarDisponibilidad(8888L,entrada,entrada.plusDays(1),2).disponible());
    }

    @Test void soloAceptaExactamente24HorasYRespetaElAforo() {
        assertThrows(ResponseStatusException.class,()->servicio.consultarDisponibilidad(8888L,entrada,entrada.plusDays(2),2,LocalTime.NOON,LocalTime.NOON));
        assertThrows(ResponseStatusException.class,()->servicio.consultarDisponibilidad(8888L,entrada,entrada.plusDays(1),2,LocalTime.NOON,LocalTime.of(11,59)));
        assertThrows(ResponseStatusException.class,()->servicio.crearReservaTemporal(100L,new CrearReservaRequest(8888L,entrada,entrada.plusDays(1),9,LocalTime.NOON,LocalTime.NOON)));
        var cot=servicio.consultarDisponibilidad(8888L,entrada,entrada.plusDays(1),8,LocalTime.NOON,LocalTime.NOON);
        assertEquals(new BigDecimal("250.00"),cot.montoTotal());
    }
    @Test void reservaDeOtroClienteBloqueaHorasPeroPermiteIngresoAlTerminar() {
        var primera=servicio.crearReservaTemporal(99L,new CrearReservaRequest(8888L,entrada,entrada.plusDays(1),2,LocalTime.of(14,0),LocalTime.of(14,0)));
        assertFalse(servicio.consultarDisponibilidad(8888L,entrada.plusDays(1),entrada.plusDays(2),2,LocalTime.of(13,59),LocalTime.of(13,59)).disponible());
        var conflicto=assertThrows(ResponseStatusException.class,()->servicio.crearReservaTemporal(100L,new CrearReservaRequest(8888L,entrada,entrada.plusDays(1),2,LocalTime.of(15,0),LocalTime.of(15,0))));
        assertEquals(409,conflicto.getStatusCode().value());
        var siguiente=servicio.crearReservaTemporal(100L,new CrearReservaRequest(8888L,entrada.plusDays(1),entrada.plusDays(2),8,LocalTime.of(14,0),LocalTime.of(14,0)));
        assertNotEquals(primera.idReserva(),siguiente.idReserva());
        assertEquals(LocalTime.of(14,0),siguiente.horaIngreso());
        assertEquals(2,servicio.consultarFechasOcupadas(8888L,entrada,entrada.plusDays(3)).intervalos().size());
    }
}
