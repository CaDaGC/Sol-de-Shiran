package pe.edu.upeu.soldeshiran.almacenamiento;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class DriveStorage {
    private final ObjectMapper mapper;
    private final Path credenciales;
    private final Map<CategoriaArchivo, String> carpetas;
    private final HttpClient cliente;
    private final URI api;
    private final URI subida;
    private final URI oauth;
    private String token;
    private Instant vence = Instant.EPOCH;

    public DriveStorage(ObjectMapper mapper, Path credenciales, Map<CategoriaArchivo, String> carpetas) {
        this(mapper, credenciales, carpetas, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                URI.create("https://www.googleapis.com/drive/v3/"), URI.create("https://www.googleapis.com/upload/drive/v3/"),
                URI.create("https://oauth2.googleapis.com/token"));
    }
    DriveStorage(ObjectMapper mapper, Path credenciales, Map<CategoriaArchivo, String> carpetas,
                 HttpClient cliente, URI api, URI subida, URI oauth) {
        this.mapper = mapper; this.credenciales = credenciales; this.carpetas = carpetas;
        this.cliente = cliente; this.api = api; this.subida = subida; this.oauth = oauth;
    }
    private synchronized String token() throws IOException, InterruptedException {
        if (token != null && Instant.now().isBefore(vence)) return token;
        if (!Files.isRegularFile(credenciales)) throw error("Falta autorizar Google Drive y configurar el archivo privado de credenciales.");
        JsonNode datos = mapper.readTree(Files.readAllBytes(credenciales));
        var formulario = new LinkedHashMap<String, String>();
        formulario.put("grant_type", "refresh_token");
        for (String campo : List.of("client_id", "client_secret", "refresh_token")) {
            String valor = datos.path(campo).asText();
            if (valor.isBlank()) throw error("El archivo privado de Google Drive está incompleto.");
            formulario.put(campo, valor);
        }
        String cuerpo = formulario.entrySet().stream().map(e -> codificar(e.getKey()) + "=" + codificar(e.getValue()))
                .collect(java.util.stream.Collectors.joining("&"));
        var respuesta = cliente.send(HttpRequest.newBuilder(oauth).timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build(), HttpResponse.BodyHandlers.ofByteArray());
        if (respuesta.statusCode() != 200) throw error("Google rechazó la autorización de Drive. Vuelve a autorizar la cuenta del almacenamiento.");
        var resultado = mapper.readTree(respuesta.body());
        token = resultado.path("access_token").asText();
        if (token.isBlank()) throw error("Google no devolvió una autorización válida para Drive.");
        vence = Instant.now().plusSeconds(Math.max(1, resultado.path("expires_in").asLong(3600) - 60));
        return token;
    }
    public String guardar(CategoriaArchivo categoria, String nombre, byte[] contenido, String tipo) throws IOException, InterruptedException {
        String carpeta = carpeta(categoria);
        String limite = "solshiran_" + UUID.randomUUID();
        byte[] metadatos = mapper.writeValueAsBytes(Map.of("name", nombre, "parents", List.of(carpeta),
                "appProperties", Map.of("aplicacion", "sol-shiran", "categoria", categoria.name())));
        var cuerpo = new ByteArrayOutputStream();
        cuerpo.write(("--" + limite + "\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        cuerpo.write(metadatos);
        cuerpo.write(("\r\n--" + limite + "\r\nContent-Type: " + tipo + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        cuerpo.write(contenido);
        cuerpo.write(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
        var respuesta = enviar(subida.resolve("files?uploadType=multipart&fields=id&supportsAllDrives=true"), "POST",
                "multipart/related; boundary=" + limite, cuerpo.toByteArray());
        String id = mapper.readTree(respuesta).path("id").asText();
        validarId(id);
        return "drive:" + id;
    }
    public byte[] leer(CategoriaArchivo categoria, String id) throws IOException, InterruptedException {
        validarId(id);
        var metadatos = mapper.readTree(enviar(api.resolve("files/" + id + "?fields=parents,trashed&supportsAllDrives=true"), "GET", null, null));
        boolean pertenece = false;
        for (var padre : metadatos.path("parents")) pertenece |= carpeta(categoria).equals(padre.asText());
        if (!pertenece || metadatos.path("trashed").asBoolean()) throw error("El archivo no está disponible en la carpeta de almacenamiento configurada.");
        return enviar(api.resolve("files/" + id + "?alt=media&supportsAllDrives=true"), "GET", null, null);
    }
    public void descartar(String id) throws IOException, InterruptedException {
        validarId(id);
        enviar(api.resolve("files/" + id + "?supportsAllDrives=true"), "PATCH", "application/json", "{\"trashed\":true}".getBytes(StandardCharsets.UTF_8));
    }
    private byte[] enviar(URI uri, String metodo, String tipo, byte[] cuerpo) throws IOException, InterruptedException {
        var solicitud = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(40)).header("Authorization", "Bearer " + token());
        if (tipo != null) solicitud.header("Content-Type", tipo);
        var respuesta = cliente.send(solicitud.method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(cuerpo)).build(), HttpResponse.BodyHandlers.ofByteArray());
        int estado = respuesta.statusCode();
        if (estado == 401) { synchronized (this) { token = null; vence = Instant.EPOCH; } throw error("La autorización de Drive venció. Reintenta la operación."); }
        if (estado == 403) throw error("Drive rechazó el almacenamiento. Revisa el acceso a la carpeta y el espacio disponible en tu cuenta.");
        if (estado == 404) throw error("No se encontró el archivo o la carpeta configurada en Google Drive.");
        if (estado < 200 || estado >= 300) throw error("Google Drive no está disponible. Reintenta la operación sin volver a pagar.");
        return respuesta.body();
    }
    private String carpeta(CategoriaArchivo categoria) { String id = carpetas.get(categoria); validarId(id); return id; }
    private static void validarId(String id) { if (id == null || !id.matches("[a-zA-Z0-9_-]{10,150}")) throw error("La referencia de Google Drive no es válida."); }
    private static String codificar(String valor) { return URLEncoder.encode(valor, StandardCharsets.UTF_8); }
    private static ResponseStatusException error(String mensaje) { return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, mensaje); }
}
