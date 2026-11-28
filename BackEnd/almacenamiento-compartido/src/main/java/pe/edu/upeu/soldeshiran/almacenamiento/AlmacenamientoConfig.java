package pe.edu.upeu.soldeshiran.almacenamiento;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AlmacenamientoConfig {

    @Bean
    public AlmacenamientoService almacenamientoService(
            Environment entorno,
            ObjectProvider<ObjectMapper> mappers) {

        var carpetas = Map.of(
                CategoriaArchivo.PERFIL,
                entorno.getProperty("drive.carpeta.perfiles", ""),
                CategoriaArchivo.CABANA,
                entorno.getProperty("drive.carpeta.cabanas", ""),
                CategoriaArchivo.COMPROBANTE,
                entorno.getProperty("drive.carpeta.comprobantes", ""),
                CategoriaArchivo.RESTAURANTE,
                entorno.getProperty("drive.carpeta.restaurantes", ""),
                CategoriaArchivo.PRODUCTO,
                entorno.getProperty("drive.carpeta.productos", ""),
                CategoriaArchivo.COMANDA,
                entorno.getProperty("drive.carpeta.comandas", "")
        );

        var locales = Map.of(
                CategoriaArchivo.PERFIL,
                Path.of(entorno.getProperty(
                        "usuarios.fotos.directorio",
                        "uploads/perfiles"
                )),
                CategoriaArchivo.CABANA,
                Path.of(entorno.getProperty(
                        "catalogo.fotos.directorio",
                        "uploads/cabanas"
                )),
                CategoriaArchivo.COMPROBANTE,
                Path.of(entorno.getProperty(
                        "comprobantes.directorio",
                        "uploads/comprobantes"
                )),
                CategoriaArchivo.RESTAURANTE,
                Path.of(entorno.getProperty("restaurantes.fotos.directorio", "uploads/restaurantes")),
                CategoriaArchivo.PRODUCTO,
                Path.of(entorno.getProperty("productos.fotos.directorio", "uploads/productos")),
                CategoriaArchivo.COMANDA,
                Path.of(entorno.getProperty(
                        "comandas.directorio",
                        "uploads/comandas"
                ))
        );

        var drive = new DriveStorage(
                mappers.getIfAvailable(ObjectMapper::new),
                Path.of(entorno.getProperty(
                        "drive.credenciales",
                        "privado/google-drive.json"
                )),
                carpetas
        );

        return new AlmacenamientoService(
                entorno.getProperty("archivos.proveedor", "local"),
                new LocalStorage(locales),
                drive
        );
    }
}