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
  @Autowired PersonaRepository personas;
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
    personas.deleteAll();
    admin = crear("ADMIN", "admin@test.local");
    cliente = crear("CLIENTE", "user@test.local");
    dueno = crear("DUENO", "dueno@test.local");
  }

  long crear(String rol, String email) {
    Usuario u = new Usuario();
    u.setNombre(email.split("@")[0]);
    u.setEmail(email);
    u.setPassword("hash-no-publico");
    u.setRol(roles.findByNombre(rol).orElseThrow());
    return usuarios.save(u).getId();
  }

  @Test
  void adminCambiaEstadoSinBorrarNiAlterarIdentidad() throws Exception {
    long total = usuarios.count();
    var original = usuarios.findById(cliente).orElseThrow();
    String correo = original.getEmail();
    String hash = original.getPassword();
    for (boolean activo : new boolean[] {false, true}) {
      mvc.perform(put("/api/v1/usuarios/" + cliente + "/estado")
          .header("Authorization", token(admin))
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"activo\":" + activo + "}"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.activo").value(activo))
          .andExpect(jsonPath("$.password").doesNotExist());
      var guardado = usuarios.findById(cliente).orElseThrow();
      Assertions.assertEquals(total, usuarios.count());
      Assertions.assertEquals(correo, guardado.getEmail());
      Assertions.assertEquals(hash, guardado.getPassword());
      Assertions.assertEquals(activo, guardado.getActivo());
    }
  }

  @Test
  void cambioEstadoExigeAdminYEstadoExplicito() throws Exception {
    mvc.perform(put("/api/v1/usuarios/" + dueno + "/estado")
        .header("Authorization", token(cliente)).contentType(MediaType.APPLICATION_JSON)
        .content("{\"activo\":false}")).andExpect(status().isForbidden());
    mvc.perform(put("/api/v1/usuarios/" + admin + "/estado")
        .header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
        .content("{\"activo\":false}")).andExpect(status().isBadRequest());
    mvc.perform(put("/api/v1/usuarios/" + cliente + "/estado")
        .header("Authorization", token(admin)).contentType(MediaType.APPLICATION_JSON)
        .content("{}")).andExpect(status().isBadRequest());
  }

  @Test
  void consultaPublicaSugiereSoloNombresDisponiblesSinDatosDeCuenta() throws Exception {
    crear("CLIENTE", "admin_1@test.local");
    var respuesta = mvc.perform(get("/api/v1/usuarios/nombre-disponible").param("nombre", " ADMIN "))
        .andExpect(status().isOk()).andExpect(jsonPath("$.disponible").value(false))
        .andExpect(jsonPath("$.id").doesNotExist()).andExpect(jsonPath("$.email").doesNotExist())
        .andReturn().getResponse().getContentAsString();
    var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(respuesta);
    Assertions.assertEquals(2, json.get("sugerencias").size());
    for (var sugerencia : json.get("sugerencias")) {
      Assertions.assertFalse(usuarios.existsByNombreIgnoreCase(sugerencia.asText()));
      Assertions.assertTrue(sugerencia.asText().startsWith("admin_"));
      Assertions.assertTrue(sugerencia.asText().length() <= 80);
    }
    mvc.perform(get("/api/v1/usuarios/nombre-disponible").param("nombre", "nombrelibre"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.disponible").value(true))
        .andExpect(jsonPath("$.sugerencias.length()").value(0));
  }

  @Test
  void nombreDeCuentaInactivaSigueReservadoYSeValidanLimites() throws Exception {
    var cuenta = usuarios.findById(cliente).orElseThrow();
    cuenta.setActivo(false); usuarios.save(cuenta);
    mvc.perform(get("/api/v1/usuarios/nombre-disponible").param("nombre", cuenta.getNombre()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.disponible").value(false));
    for (String invalido : new String[] {"   ", "a".repeat(81)})
      mvc.perform(get("/api/v1/usuarios/nombre-disponible").param("nombre", invalido))
          .andExpect(status().isBadRequest());
  }

  @Test
  void noDuplicaNombreAlCrearOEditarYPermiteConservarElPropio() throws Exception {
    mvc.perform(post("/api/v1/usuarios").header("Authorization", token(admin))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nombreUsuario\":\"ADMIN\",\"email\":\"otro@test.local\",\"password\":\"Password123!\",\"rol\":\"ADMIN\"}"))
        .andExpect(status().isConflict());
    mvc.perform(put("/api/v1/usuarios/" + cliente).header("Authorization", token(admin))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nombreUsuario\":\"ADMIN\",\"email\":\"cliente@test.local\"}"))
        .andExpect(status().isConflict());
    mvc.perform(put("/api/v1/usuarios/" + admin).header("Authorization", token(admin))
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"nombreUsuario\":\"ADMIN\",\"email\":\"admin@test.local\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.nombreUsuario").value("ADMIN"));
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
                        + " nuevo\",\"email\":\"nuevo@test.local\",\"password\":\"Password123!\",\"rol\":\"DUENO\","
                        + "\"persona\":{\"nombres\":\"Ana\",\"apellidos\":\"Perez\",\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"12345678\",\"telefono\":\"999999999\",\"direccion\":\"Lima\"}}"))
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
                    "{\"nombre\":\"Registro\",\"email\":\"registro@test.local\",\"password\":\"Password123!\",\"rol\":\"ADMIN\",\"persona\":{\"nombres\":\"Ana\",\"apellidos\":\"Perez\",\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"12345678\",\"telefono\":\"999999999\",\"direccion\":\"Lima\"}}"))
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
                .content("{\"nombre\":\"Prueba\",\"email\":\"user@test.local\",\"rol\":\"ADMIN\",\"persona\":{\"nombres\":\"Ana\",\"apellidos\":\"Perez\",\"tipoDocumento\":\"DNI\",\"numeroDocumento\":\"12345678\",\"telefono\":\"999999999\",\"direccion\":\"Lima\"}}"))
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
  void adminDesactivaUsuarioConservandoLaCuentaYElTokenDejaDeServir() throws Exception {
    mvc.perform(delete("/api/v1/usuarios/" + cliente).header("Authorization", token(admin)))
        .andExpect(status().isNoContent());
    Assertions.assertTrue(usuarios.existsById(cliente));
    Assertions.assertFalse(usuarios.findById(cliente).orElseThrow().getActivo());
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
  void adminDesactivaDuenoSinEliminarSuCuentaNiTrabajadores() throws Exception {
    Usuario trabajador = usuarios.findById(crear("TRABAJADOR", "trabajador@test.local"))
        .orElseThrow();
    usuarios.save(trabajador);
    mvc.perform(delete("/api/v1/usuarios/" + dueno).header("Authorization", token(admin)))
        .andExpect(status().isNoContent());
    Assertions.assertTrue(usuarios.existsById(dueno));
    Assertions.assertFalse(usuarios.findById(dueno).orElseThrow().getActivo());
    Assertions.assertTrue(usuarios.existsById(trabajador.getId()));
  }

  @Test
  void duenoNoPuedeEliminarTrabajadorPropio() throws Exception {
    Usuario trabajador = usuarios.findById(crear("TRABAJADOR", "propio@test.local"))
        .orElseThrow();
    usuarios.save(trabajador);
    mvc.perform(delete("/api/v1/usuarios/" + trabajador.getId())
        .header("Authorization", token(dueno)))
        .andExpect(status().isForbidden());
    Assertions.assertTrue(usuarios.existsById(trabajador.getId()));
  }

  @Test
  void registroPublicoExigePersonaPeroAdminPuedeCrearCuentaSinPersona() throws Exception {
    String cuerpo = """
        {"nombreUsuario":"nuevo-alias","email":"alias@test.local","password":"Password123!","rol":"ROLE_ADMIN"}
        """;
    mvc.perform(post("/api/v1/usuarios/register").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
        .andExpect(status().isBadRequest());
    Assertions.assertFalse(usuarios.existsByEmail("alias@test.local"));
    Assertions.assertEquals(0, personas.count());
    mvc.perform(post("/api/v1/usuarios/admin").header("Authorization",token(admin))
        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.nombreUsuario").value("nuevo-alias"))
        .andExpect(jsonPath("$.persona").isEmpty())
        .andExpect(jsonPath("$.rol").value("ADMIN"));
  }

  @Test
  void documentoRepetidoRevierteLaCuentaYElAliasEsUnico() throws Exception {
    String cuerpo = """
        {"nombreUsuario":"cliente-uno","email":"uno@test.local","password":"Password123!",
         "persona":{"nombres":"Ana","apellidos":"Pérez","numeroDocumento":"98765432","telefono":"999888777","direccion":"Lima"}}
        """;
    mvc.perform(post("/api/v1/usuarios/register").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.persona.nombres").value("Ana"));
    mvc.perform(post("/api/v1/usuarios/register").contentType(MediaType.APPLICATION_JSON)
        .content(cuerpo.replace("cliente-uno","cliente-dos").replace("uno@test","dos@test")))
        .andExpect(status().isConflict());
    Assertions.assertEquals(1, personas.count());
    Assertions.assertFalse(usuarios.existsByEmail("dos@test.local"));
  }

  @Test
  void desactivarConservaPersonaYDocumentoDeLaCuenta() throws Exception {
    String cuerpo = """
        {"nombreUsuario":"con-persona","email":"persona@test.local","password":"Password123!",
         "persona":{"nombres":"Ana","apellidos":"Pérez","numeroDocumento":"99999999","telefono":"999888777","direccion":"Lima"}}
        """;
    mvc.perform(post("/api/v1/usuarios/register").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
        .andExpect(status().isCreated());
    long id=usuarios.findByEmail("persona@test.local").orElseThrow().getId();
    long cantidad=personas.count();
    mvc.perform(delete("/api/v1/usuarios/"+id).header("Authorization",token(admin))).andExpect(status().isNoContent());
    Assertions.assertTrue(usuarios.existsById(id));
    Assertions.assertEquals(cantidad,personas.count());
    mvc.perform(get("/api/v1/usuarios/"+id).header("Authorization",token(admin)))
        .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false))
        .andExpect(jsonPath("$.persona.numeroDocumento").value("99999999"));
  }

  @Test
  void personalNoPuedeCambiarSuCorreoPeroAdminSiPuede() throws Exception {
    for (String rol : new String[] {"TRABAJADOR", "RESTAURANTE"}) {
      String original = rol.toLowerCase() + "-correo@test.local";
      String nuevo = rol.toLowerCase() + "-nuevo@test.local";
      long id = crear(rol, original);
      String cuerpo = "{\"nombreUsuario\":\"alias-" + rol + "\",\"email\":\"" + nuevo + "\"}";
      mvc.perform(put("/api/v1/usuarios/" + id).header("Authorization", token(id))
          .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isForbidden());
      Assertions.assertEquals(original, usuarios.findById(id).orElseThrow().getEmail());
      mvc.perform(put("/api/v1/usuarios/" + id).header("Authorization", token(id))
          .contentType(MediaType.APPLICATION_JSON).content(cuerpo.replace(nuevo, original))).andExpect(status().isOk());
      mvc.perform(put("/api/v1/usuarios/" + id).header("Authorization", token(admin))
          .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isOk());
      Assertions.assertEquals(nuevo, usuarios.findById(id).orElseThrow().getEmail());
    }
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
            "test-only-jwt-key-not-for-production-1234567890"
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
