package pe.edu.upeu.soldeshiran.almacenamiento;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import java.nio.file.Path;
import java.util.Map;

@Configuration
public class AlmacenamientoConfig {
    @Bean
    public AlmacenamientoService almacenamientoService(Environment entorno, org.springframework.beans.factory.ObjectProvider<ObjectMapper> mappers) {
        var carpetas = Map.of(
                CategoriaArchivo.PERFIL, entorno.getProperty("drive.carpeta.perfiles", ""),
                CategoriaArchivo.CABANA, entorno.getProperty("drive.carpeta.cabanas", ""),
                CategoriaArchivo.COMPROBANTE, entorno.getProperty("drive.carpeta.comprobantes", ""));
        var locales = Map.of(
                CategoriaArchivo.PERFIL, Path.of(entorno.getProperty("usuarios.fotos.directorio", "uploads/perfiles")),
                CategoriaArchivo.CABANA, Path.of(entorno.getProperty("catalogo.fotos.directorio", "uploads/cabanas")),
                CategoriaArchivo.COMPROBANTE, Path.of(entorno.getProperty("comprobantes.directorio", "uploads/comprobantes")));
        var drive = new DriveStorage(mappers.getIfAvailable(ObjectMapper::new), Path.of(entorno.getProperty("drive.credenciales", "privado/google-drive.json")), carpetas);
        return new AlmacenamientoService(entorno.getProperty("archivos.proveedor", "local"), new LocalStorage(locales), drive);
    }
}
