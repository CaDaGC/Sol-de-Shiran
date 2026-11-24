package pe.edu.upeu.soldeshiran.pagos_service.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import pe.edu.upeu.soldeshiran.pagos_service.service.impl.ComprobanteServiceImpl;
import pe.edu.upeu.soldeshiran.pagos_service.repository.ComprobantePagoRepository;
import pe.edu.upeu.soldeshiran.almacenamiento.AlmacenamientoService;

@Configuration
@ConditionalOnProperty(name = "archivos.migrar", havingValue = "true")
public class MigracionArchivosConfig {
    @Bean
    @org.springframework.core.annotation.Order(100)
    public ApplicationRunner migrarArchivos(ComprobanteServiceImpl servicio, ComprobantePagoRepository repository, AlmacenamientoService almacenamiento) {
        return argumentos -> {
            if (!almacenamiento.usaDrive()) throw new IllegalStateException("Para migrar los archivos activa archivos.proveedor=drive.");
            int pagina = 0, migrados = 0;
            org.springframework.data.domain.Page<?> lote;
            do {
                var registros = repository.findAll(PageRequest.of(pagina++, 50, Sort.by("id")));
                lote = registros;
                for (var registro : registros) if (servicio.migrar(registro.getId())) migrados++;
            } while (lote.hasNext());
            org.slf4j.LoggerFactory.getLogger(MigracionArchivosConfig.class).info("Migración de archivos finalizada: {} archivos verificados en Drive.", migrados);
        };
    }
}
