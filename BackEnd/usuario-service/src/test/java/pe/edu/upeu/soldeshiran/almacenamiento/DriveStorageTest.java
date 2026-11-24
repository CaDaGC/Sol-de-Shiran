package pe.edu.upeu.soldeshiran.almacenamiento;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DriveStorageTest {
    @TempDir Path temporal;
    HttpServer servidor;
    DriveStorage drive;
    AlmacenamientoService almacenamiento;
    final byte[] imagen = new byte[]{1, 2, 3, 4};
    String carpeta = "carpeta_perfiles_123";
    int estadoSubida = 200;
    boolean contenidoDistinto;
    String cuerpoSubida;
    final AtomicInteger autorizaciones = new AtomicInteger();
    final AtomicInteger descargas = new AtomicInteger();

    @BeforeEach void preparar() throws Exception {
        Path credenciales = temporal.resolve("credenciales.json");
        Files.writeString(credenciales, "{\"client_id\":\"cliente-prueba\",\"client_secret\":\"secreto-prueba\",\"refresh_token\":\"renovacion-prueba\"}");
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/token", e -> {
            autorizaciones.incrementAndGet();
            e.getRequestBody().readAllBytes();
            byte[] cuerpo = "{\"access_token\":\"token-de-prueba\",\"expires_in\":3600}".getBytes();
            e.sendResponseHeaders(200, cuerpo.length); e.getResponseBody().write(cuerpo); e.close();
        });
        servidor.createContext("/upload/", e -> {
            assertEquals("Bearer token-de-prueba", e.getRequestHeaders().getFirst("Authorization"));
            cuerpoSubida = new String(e.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
            byte[] cuerpo = "{\"id\":\"archivo_drive_123\"}".getBytes();
            e.sendResponseHeaders(estadoSubida, cuerpo.length); e.getResponseBody().write(cuerpo); e.close();
        });
        servidor.createContext("/drive/files/", e -> {
            byte[] cuerpo;
            if (e.getRequestURI().getQuery().contains("alt=media")) { descargas.incrementAndGet(); cuerpo = contenidoDistinto ? new byte[]{9} : imagen; }
            else cuerpo = ("{\"parents\":[\"" + carpeta + "\"],\"trashed\":false}").getBytes();
            e.sendResponseHeaders(200, cuerpo.length); e.getResponseBody().write(cuerpo); e.close();
        });
        servidor.start();
        String url = "http://127.0.0.1:" + servidor.getAddress().getPort();
        drive = new DriveStorage(new ObjectMapper(), credenciales, Map.of(CategoriaArchivo.PERFIL, "carpeta_perfiles_123"),
                HttpClient.newHttpClient(), URI.create(url + "/drive/"), URI.create(url + "/upload/"), URI.create(url + "/token"));
        almacenamiento = new AlmacenamientoService("drive", new LocalStorage(Map.of(CategoriaArchivo.PERFIL, temporal.resolve("fotos"))), drive);
    }
    @AfterEach void cerrar() { servidor.stop(0); }

    @Test void subeALaCarpetaYLeeSinCrearUnaCopiaLocal() {
        String referencia = almacenamiento.guardar(CategoriaArchivo.PERFIL, "foto.png", imagen, "image/png");
        assertEquals("drive:archivo_drive_123", referencia);
        assertTrue(cuerpoSubida.contains("carpeta_perfiles_123"));
        assertTrue(cuerpoSubida.contains("Content-Type: image/png"));
        assertTrue(cuerpoSubida.contains("\r\n"));
        assertArrayEquals(imagen, almacenamiento.leer(CategoriaArchivo.PERFIL, referencia));
        assertArrayEquals(imagen, almacenamiento.leer(CategoriaArchivo.PERFIL, referencia));
        assertEquals(1, autorizaciones.get());
        assertFalse(Files.exists(temporal.resolve("fotos")));
    }
    @Test void noEntregaArchivosDeOtraCarpeta() {
        carpeta = "otra_carpeta_123";
        assertThrows(ResponseStatusException.class, () -> almacenamiento.leer(CategoriaArchivo.PERFIL, "drive:archivo_drive_123"));
        assertEquals(0, descargas.get());
    }
    @Test void unErrorDeDriveNoGuardaEnLocalNiOcultaElMotivo() {
        estadoSubida = 403;
        var error = assertThrows(ResponseStatusException.class, () -> almacenamiento.guardar(CategoriaArchivo.PERFIL, "foto.png", imagen, "image/png"));
        assertTrue(error.getReason().contains("espacio"));
        assertFalse(Files.exists(temporal.resolve("fotos")));
    }
    @Test void migracionVerificaLaCopiaAntesDeConfirmar() {
        assertEquals("drive:archivo_drive_123", almacenamiento.migrar(CategoriaArchivo.PERFIL, "foto.png", imagen, "image/png"));
        assertEquals(1, descargas.get());
    }
    @Test void evitaRutasFueraDeLasCarpetasLocales() {
        var local = new AlmacenamientoService("local", new LocalStorage(Map.of(CategoriaArchivo.PERFIL, temporal)), drive);
        assertThrows(ResponseStatusException.class, () -> local.leer(CategoriaArchivo.PERFIL, "../credenciales.json"));
    }
}
