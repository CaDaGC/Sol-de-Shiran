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

    @Test void aplicaHorarioFijoSinConfiarEnHorasDelClienteYRespetaElAforo() {
        var cot=servicio.consultarDisponibilidad(8888L,entrada,entrada.plusDays(1),8,LocalTime.NOON,LocalTime.NOON);
        assertEquals(LocalTime.of(11,0),cot.horaIngreso());
        assertEquals(LocalTime.of(13,0),cot.horaSalida());
        assertEquals(new BigDecimal("250.00"),cot.montoTotal());
        assertThrows(ResponseStatusException.class,()->servicio.crearReservaTemporal(100L,new CrearReservaRequest(8888L,entrada,entrada.plusDays(1),9)));
    }
    @Test void otroClienteVeElBloqueoEnCalendarioYNoPuedeCrearLaMismaEstancia() {
        var primera=servicio.crearReservaTemporal(99L,new CrearReservaRequest(8888L,entrada,entrada.plusDays(1),2));
        assertEquals(LocalTime.of(11,0),primera.horaIngreso());
        assertEquals(LocalTime.of(13,0),primera.horaSalida());
        var calendario=servicio.consultarFechasOcupadas(8888L,entrada.minusDays(1),entrada.plusDays(4));
        assertTrue(calendario.fechasOcupadas().contains(entrada));
        assertEquals(entrada.atTime(11,0),calendario.intervalos().getFirst().inicio());
        assertFalse(servicio.consultarDisponibilidad(8888L,entrada,entrada.plusDays(1),2).disponible());
        var conflicto=assertThrows(ResponseStatusException.class,()->servicio.crearReservaTemporal(100L,new CrearReservaRequest(8888L,entrada,entrada.plusDays(1),2)));
        assertEquals(409,conflicto.getStatusCode().value());

        assertFalse(servicio.consultarDisponibilidad(8888L,entrada.plusDays(1),entrada.plusDays(2),2).disponible());
        var siguiente=servicio.crearReservaTemporal(100L,new CrearReservaRequest(8888L,entrada.plusDays(2),entrada.plusDays(3),2));
        assertNotEquals(primera.idReserva(),siguiente.idReserva());
    }
    @Test void permiteCotizarYCrearParaHoyAunqueHayaPasadoElHorarioDeIngreso() {
        var hoy = LocalDate.now(ZoneId.of("America/Lima"));
        var cotizacion = servicio.consultarDisponibilidad(8888L,hoy,hoy.plusDays(2),2);
        assertTrue(cotizacion.disponible());
        assertEquals(2,cotizacion.noches());
        assertEquals(new BigDecimal("500.00"),cotizacion.montoTotal());
        var creada = servicio.crearReservaTemporal(100L,new CrearReservaRequest(8888L,hoy,hoy.plusDays(2),2));
        assertEquals(LocalTime.of(11,0),creada.horaIngreso());
        assertEquals(LocalTime.of(13,0),creada.horaSalida());
        assertEquals(new BigDecimal("500.00"),creada.montoTotal());
        assertFalse(servicio.consultarDisponibilidad(8888L,hoy,hoy.plusDays(1),2).disponible());
    }
}
