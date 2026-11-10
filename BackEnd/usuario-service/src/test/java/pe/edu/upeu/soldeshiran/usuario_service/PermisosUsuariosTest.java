package pe.edu.upeu.soldeshiran.usuario_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.soldeshiran.usuario_service.model.*;
import pe.edu.upeu.soldeshiran.usuario_service.repository.*;

@SpringBootTest(
    properties = {
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false",
      "spring.datasource.url=jdbc:h2:mem:usuarios;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "admin.email=",
      "admin.password="
    })
@AutoConfigureMockMvc
class PermisosUsuariosTest {
  private static final java.util.concurrent.atomic.AtomicBoolean asignado =
      new java.util.concurrent.atomic.AtomicBoolean();
  private static final com.sun.net.httpserver.HttpServer catalogo = iniciarCatalogo();

  private static com.sun.net.httpserver.HttpServer iniciarCatalogo() {
    try {
      var server = com.sun.net.httpserver.HttpServer.create(
          new java.net.InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/internal/cabanas/asignaciones/", exchange -> {
        byte[] body = Boolean.toString(asignado.get())
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (var output = exchange.getResponseBody()) {
          output.write(body);
        }
      });
      server.start();
      return server;
    } catch (java.io.IOException e) {
      throw new java.io.UncheckedIOException(e);
    }
  }

  @org.springframework.test.context.DynamicPropertySource
  static void configurarCatalogo(org.springframework.test.context.DynamicPropertyRegistry config) {
    config.add("catalogo.url", () -> "http://127.0.0.1:" + catalogo.getAddress().getPort());
  }

  @AfterAll
  static void cerrarCatalogo() {
    catalogo.stop(0);
  }

  @Autowired MockMvc mvc;
  @Autowired UsuarioRepository usuarios;
  @Autowired RolRepository roles;
  @Autowired pe.edu.upeu.soldeshiran.usuario_service.security.RoleInitializer inicializador;
  long admin, cliente, dueno;

  @Test
  void migraRolesAnterioresSinCambiarLasCuentas() {
    for (String[] cambio : new String[][] {{"USUARIO", "CLIENTE"}, {"GUARDIAN", "TRABAJADOR"}}) {
      Rol anterior = new Rol();
      anterior.setNombre(cambio[0]);
      roles.save(anterior);
      long id = crear(cambio[0], cambio[0] + "@test.local");
      inicializador.run();
      Assertions.assertTrue(roles.findByNombre(cambio[0]).isEmpty());
      Assertions.assertEquals(cambio[1], usuarios.findById(id).orElseThrow().getRol().getNombre());
    }
  }

  @BeforeEach
  void preparar() {
    usuarios.deleteAll();
    admin = crear("ADMIN", "admin@test.local");
    cliente = crear("CLIENTE", "user@test.local");
    dueno = crear("DUENO", "dueno@test.local");
  }

  long crear(String rol, String email) {
    Usuario u = new Usuario();
    u.setNombre("Prueba");
    u.setEmail(email);
    u.setPassword("hash-no-publico");
    u.setRol(roles.findByNombre(rol).orElseThrow());
    return usuarios.save(u).getId();
  }

  @Test
  void puertoDirectoNoPermiteCrearAdminAnonimo() throws Exception {
    mvc.perform(
            post("/api/v1/usuarios/admin").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void clienteNoPuedeCrearAdmin() throws Exception {
    mvc.perform(
            post("/api/v1/usuarios/admin")
                .header("Authorization", token(cliente))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void clienteNoPuedeListarUsuarios() throws Exception {
    mvc.perform(get("/api/v1/usuarios").header("Authorization", token(cliente)))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminPuedeCrearDuenoSinExponerPassword() throws Exception {
    mvc.perform(
            post("/api/v1/usuarios")
                .header("Authorization", token(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"DueÃ±o"
                        + " nuevo\",\"email\":\"nuevo@test.local\",\"password\":\"Password123!\",\"rol\":\"DUENO\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rol").value("DUENO"))
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void registroPublicoIgnoraRolPrivilegiado() throws Exception {
    mvc.perform(
            post("/api/v1/usuarios/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"Registro\",\"email\":\"registro@test.local\",\"password\":\"Password123!\",\"rol\":\"ADMIN\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.rol").value("CLIENTE"));
  }

  @Test
  void duenoNoPuedeCrearOtroDueno() throws Exception {
    mvc.perform(
            post("/api/v1/usuarios")
                .header("Authorization", token(dueno))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"Otro\",\"email\":\"otro@test.local\",\"password\":\"Password123!\",\"rol\":\"DUENO\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void tokenDeCuentaDesactivadaSeRechaza() throws Exception {
    Usuario u = usuarios.findById(cliente).orElseThrow();
    u.setActivo(false);
    usuarios.save(u);
    mvc.perform(get("/api/v1/usuarios/me").header("Authorization", token(cliente)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void cabeceraSuplantadaNoDaAcceso() throws Exception {
    mvc.perform(get("/api/v1/usuarios").header("X-User-Id", admin).header("X-Role", "ADMIN"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void hashInternoNoEsPublico() throws Exception {
    mvc.perform(get("/internal/usuarios/autenticacion").param("email", "admin@test.local"))
        .andExpect(status().isForbidden());
  }

  @Test
  void clienteNoPuedeCambiarSuRol() throws Exception {
    mvc.perform(
            put("/api/v1/usuarios/" + cliente)
                .header("Authorization", token(cliente))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Prueba\",\"email\":\"user@test.local\",\"rol\":\"ADMIN\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminNoPuedeDesactivarse() throws Exception {
    mvc.perform(delete("/api/v1/usuarios/" + admin).header("Authorization", token(admin)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void correoDuplicadoDa409() throws Exception {
    mvc.perform(
            post("/api/v1/usuarios/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"nombre\":\"Prueba\",\"email\":\"user@test.local\",\"password\":\"Password123!\"}"))
        .andExpect(status().isConflict());
  }

  @Test
  void adminEliminaUsuarioDefinitivamenteYElTokenDejaDeServir() throws Exception {
    mvc.perform(delete("/api/v1/usuarios/" + cliente).header("Authorization", token(admin)))
        .andExpect(status().isNoContent());
    Assertions.assertFalse(usuarios.existsById(cliente));
    mvc.perform(get("/api/v1/usuarios/me").header("Authorization", token(cliente)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void duenoNoPuedeEliminarUsuariosNiTrabajadoresAjenos() throws Exception {
    long ajeno = crear("TRABAJADOR", "ajeno@test.local");
    for (long id : new long[] {cliente, ajeno}) {
      mvc.perform(delete("/api/v1/usuarios/" + id).header("Authorization", token(dueno)))
          .andExpect(status().isForbidden());
      Assertions.assertTrue(usuarios.existsById(id));
    }
  }

  @Test
  void adminPuedeEliminarDuenoYDesvinculaTrabajadores() throws Exception {
    Usuario trabajador = usuarios.findById(crear("TRABAJADOR", "trabajador@test.local"))
        .orElseThrow();
    trabajador.setDuenoId(dueno);
    usuarios.save(trabajador);
    mvc.perform(delete("/api/v1/usuarios/" + dueno).header("Authorization", token(admin)))
        .andExpect(status().isNoContent());
    Assertions.assertFalse(usuarios.existsById(dueno));
    Assertions.assertNull(usuarios.findById(trabajador.getId()).orElseThrow().getDuenoId());
  }

  @Test
  void duenoNoPuedeEliminarTrabajadorPropio() throws Exception {
    Usuario trabajador = usuarios.findById(crear("TRABAJADOR", "propio@test.local"))
        .orElseThrow();
    trabajador.setDuenoId(dueno);
    usuarios.save(trabajador);
    mvc.perform(delete("/api/v1/usuarios/" + trabajador.getId())
        .header("Authorization", token(dueno)))
        .andExpect(status().isForbidden());
    Assertions.assertTrue(usuarios.existsById(trabajador.getId()));
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
