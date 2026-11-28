package pe.edu.upeu.soldeshiran.restaurante_service.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TiempoConfig {
    @Bean
    public Clock relojRestaurante() {
        return Clock.system(ZoneId.of("America/Lima"));
    }
}
