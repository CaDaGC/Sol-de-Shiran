package pe.edu.upeu.soldeshiran.catalogo_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.soldeshiran.catalogo_service.model.*;
import pe.edu.upeu.soldeshiran.catalogo_service.repository.CabanaRepository;

@SpringBootTest(
    properties = {
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false",
      "servicios.interno.token=test-internal-token",
      "spring.datasource.url=jdbc:h2:mem:catalogo;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
class CatalogoHttpTest {
  static final com.sun.net.httpserver.HttpServer identity = stub();

  static com.sun.net.httpserver.HttpServer stub() {
    try {
      var server =
          com.sun.net.httpserver.HttpServer.create(
              new java.net.InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext(
          "/internal/usuarios/",
          e -> {
            String id = e.getRequestURI().getPath().substring("/internal/usuarios/".length());
            String rol =
                switch (id) {
                  case "1" -> "ADMIN";
                  case "10", "11" -> "DUENO";
                  case "20" -> "TRABAJADOR";
                  default -> "USUARIO";
                };
            byte[] b =
                ("{\"activo\":true,\"rol\":\"" + rol + "\",\"duenoId\":10}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type", "application/json");
            e.sendResponseHeaders(200, b.length);
            e.getResponseBody().write(b);
            e.close();
          });
      server.start();
      return server;
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  @DynamicPropertySource
  static void config(DynamicPropertyRegistry r) {
    r.add("usuarios.url", () -> "http://127.0.0.1:" + identity.getAddress().getPort());
  }

  @Autowired MockMvc mvc;
  @Autowired CabanaRepository cabanas;
  long id;
  String body =
      "{\"nombre\":\"CabaÃ±a nueva\",\"descripcion\":\"Vista al"
          + " jardÃ­n\",\"aforoMaximo\":4,\"numHabitaciones\":2,\"numBanos\":1,\"tarifaNoche\":150.00,\"imagenPrincipal\":\"/assets/cabana.jpg\",\"imagenesSecundarias\":[]}";

  @BeforeEach
  void setup() {
    cabanas.deleteAll();
    Cabana c = new Cabana();
    c.setNombre("CabaÃ±a existente");
    c.setDescripcion("DescripciÃ³n");
    c.setAforoMaximo(4);
    c.setNumHabitaciones(2);
    c.setNumBanos(1);
    c.setTarifaNoche(new BigDecimal("150"));
    c.setImagenPrincipal("/assets/cabana.jpg");
    c.setDuenoId(10L);
    id = cabanas.save(c).getIdCabana();
  }

  @AfterAll
  static void cerrar() {
    identity.stop(0);
  }

  @Test
  void catalogoPublicoFunciona() throws Exception {
    mvc.perform(get("/api/v1/cabanas"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nombre").value("CabaÃ±a existente"));
  }

  @Test
  void anonimonoPuedeCrearNiSuplantarCabeceras() throws Exception {
    mvc.perform(
            post("/api/v1/cabanas")
                .header("X-User-Id", "1")
                .header("X-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void usuarioNoPuedeCrear() throws Exception {
    mvc.perform(
            post("/api/v1/cabanas")
                .header("Authorization", token(30))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminPuedeCrearYConsultarGestion() throws Exception {
    mvc.perform(
            post("/api/v1/cabanas")
                .header("Authorization", token(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/v1/cabanas/gestion").header("Authorization", token(1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void duenoNoPuedeEditarCabanasAjenas() throws Exception {
    mvc.perform(
            put("/api/v1/cabanas/" + id)
                .header("Authorization", token(11))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void duenoPuedeEditarSuCabana() throws Exception {
    mvc.perform(
            put("/api/v1/cabanas/" + id)
                .header("Authorization", token(10))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("CabaÃ±a nueva"));
  }

  @Test
  void desactivarOcultaSinBorrar() throws Exception {
    mvc.perform(delete("/api/v1/cabanas/" + id).header("Authorization", token(10)))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/cabanas")).andExpect(jsonPath("$.length()").value(0));
    org.junit.jupiter.api.Assertions.assertTrue(cabanas.existsById(id));
  }

  @Test
  void gestionNoEsPublica() throws Exception {
    mvc.perform(get("/api/v1/cabanas/gestion")).andExpect(status().isUnauthorized());
  }

  @Test
  void noSeAceptanDatosIncompletos() throws Exception {
    mvc.perform(
            post("/api/v1/cabanas")
                .header("Authorization", token(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void trabajadorNoPuedeEditar() throws Exception {
    mvc.perform(
            put("/api/v1/cabanas/" + id)
                .header("Authorization", token(20))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void consultaInternaDetectaPropiedadYAsignacionDeTrabajador() throws Exception {
    String url = "/internal/cabanas/asignaciones/";
    mvc.perform(put("/api/v1/cabanas/" + id + "/trabajadores")
            .header("Authorization", token(10))
            .contentType(MediaType.APPLICATION_JSON).content("[20]"))
        .andExpect(status().isOk());
    mvc.perform(get(url + "20")).andExpect(status().isForbidden());
    for (long cuenta : new long[] {10, 20}) {
      mvc.perform(get(url + cuenta)
              .header("X-Internal-Key", "sol-shiran-interno-desarrollo-cambiar"))
          .andExpect(status().isOk())
          .andExpect(content().string("true"));
    }
    mvc.perform(put("/api/v1/cabanas/" + id + "/trabajadores")
            .header("Authorization", token(10))
            .contentType(MediaType.APPLICATION_JSON).content("[]"))
        .andExpect(status().isOk());
    mvc.perform(get(url + "20")
            .header("X-Internal-Key", "sol-shiran-interno-desarrollo-cambiar"))
        .andExpect(status().isOk())
        .andExpect(content().string("false"));
  }

  private String token(long id) throws Exception {
    var encoder = java.util.Base64.getUrlEncoder().withoutPadding();
    String header =
        encoder.encodeToString(
            "{\"alg\":\"HS256\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String body =
        encoder.encodeToString(
            ("{\"userId\":"
                    + id
                    + ",\"exp\":"
                    + (java.time.Instant.now().getEpochSecond() + 3600)
                    + "}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    var mac = javax.crypto.Mac.getInstance("HmacSHA256");
    mac.init(
        new javax.crypto.spec.SecretKeySpec(
            "ESTA_ES_UNA_CLAVE_SECRETA_MUY_LARGA_PARA_FIRMAR_TOKENS_123456"
                .getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "HmacSHA256"));
    return "Bearer "
        + header
        + "."
        + body
        + "."
        + encoder.encodeToString(
            mac.doFinal(
                (header + "." + body).getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
  }
}
