package pe.edu.upeu.soldeshiran.catalogo_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import pe.edu.upeu.soldeshiran.catalogo_service.model.Cabana;
import pe.edu.upeu.soldeshiran.catalogo_service.model.EstadoCabana;
import pe.edu.upeu.soldeshiran.catalogo_service.repository.CabanaRepository;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.cloud.config.enabled=false", "spring.config.import="})
class CabanaResumenTest {
    @Autowired CabanaRepository repository;

    @Test
    void resumenIncluyeCabanasEnMantenimientoDeReservasHistoricas() {
        Cabana cabana = new Cabana();
        cabana.setNombre("Familiar");
        cabana.setDescripcion("Cabaña familiar");
        cabana.setAforoMaximo(6);
        cabana.setNumHabitaciones(2);
        cabana.setNumBanos(1);
        cabana.setTarifaNoche(new BigDecimal("100.00"));
        cabana.setImagenPrincipal("familiar.jpg");
        cabana.setEstado(EstadoCabana.MANTENIMIENTO);
        repository.saveAndFlush(cabana);

        var resumen = repository.resumirPorIds(List.of(cabana.getIdCabana()));
        assertThat(resumen).hasSize(1);
        assertThat(resumen.getFirst().nombre()).isEqualTo("Familiar");
        assertThat(repository.findByEstadoOrderByNombreAsc(EstadoCabana.ACTIVA)).isEmpty();
    }
}
