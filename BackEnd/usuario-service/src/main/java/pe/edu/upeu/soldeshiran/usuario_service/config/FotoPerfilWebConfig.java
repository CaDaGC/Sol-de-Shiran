package pe.edu.upeu.soldeshiran.usuario_service.config;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class FotoPerfilWebConfig implements WebMvcConfigurer {
  @Value("${usuarios.fotos.directorio:uploads/perfiles}")
  private String directorio;
  @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/api/v1/usuarios/fotos/**")
        .addResourceLocations(Paths.get(directorio).toAbsolutePath().normalize().toUri().toString())
        .setCachePeriod(0);
  }
}
