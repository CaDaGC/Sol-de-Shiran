package pe.edu.upeu.soldeshiran.almacenamiento;

import java.nio.file.*;
import java.io.IOException;
import java.util.Map;

public class LocalStorage {
    private final Map<CategoriaArchivo, Path> carpetas;
    public LocalStorage(Map<CategoriaArchivo, Path> carpetas) { this.carpetas = carpetas; }
    private Path ruta(CategoriaArchivo categoria, String nombre) {
        if (nombre == null || !nombre.matches("[a-zA-Z0-9._-]{1,120}") || nombre.equals(".") || nombre.equals(".."))
            throw new IllegalArgumentException("Referencia de archivo inválida.");
        return carpetas.get(categoria).toAbsolutePath().normalize().resolve(nombre);
    }
    public String guardar(CategoriaArchivo categoria, String nombre, byte[] contenido) throws IOException {
        var archivo = ruta(categoria, nombre);
        Files.createDirectories(archivo.getParent());
        Files.write(archivo, contenido, StandardOpenOption.CREATE_NEW);
        return nombre;
    }
    public byte[] leer(CategoriaArchivo categoria, String nombre) throws IOException { return Files.readAllBytes(ruta(categoria, nombre)); }
    public void descartar(CategoriaArchivo categoria, String nombre) throws IOException { Files.deleteIfExists(ruta(categoria, nombre)); }
}
