package pe.edu.upeu.soldeshiran.reservas_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.upeu.soldeshiran.reservas_service.model.*;
import pe.edu.upeu.soldeshiran.reservas_service.repository.*;

@SpringBootTest(
    properties = {
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false",
      "servicios.interno.token=test-internal-token",
      "spring.datasource.url=jdbc:h2:mem:pagos;MODE=MySQL;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@AutoConfigureMockMvc
class PagosHttpTest {
  @org.springframework.test.context.bean.override.mockito.MockitoBean
  private pe.edu.upeu.soldeshiran.reservas_service.client.CatalogoClient catalogo;
  static final com.sun.net.httpserver.HttpServer identity = stub();

  static com.sun.net.httpserver.HttpServer stub() {
    try {
      var s =
          com.sun.net.httpserver.HttpServer.create(
              new java.net.InetSocketAddress("127.0.0.1", 0), 0);
      s.createContext(
          "/internal/usuarios/",
          e -> {
            String id = e.getRequestURI().getPath().substring("/internal/usuarios/".length());
            if (id.equals("2/perfil")) {
              e.sendResponseHeaders(404, -1);
              e.close();
              return;
            }
            byte[] b =
                ("{\"activo\":true,\"rol\":\"" + (id.equals("1") ? "ADMIN" : "USUARIO") + "\"}")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            e.getResponseHeaders().set("Content-Type", "application/json");
            e.sendResponseHeaders(200, b.length);
            e.getResponseBody().write(b);
            e.close();
          });
      s.start();
      return s;
    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    }
  }

  @DynamicPropertySource
  static void config(DynamicPropertyRegistry p) {
    p.add("usuarios.url", () -> "http://127.0.0.1:" + identity.getAddress().getPort());
  }

  @Autowired MockMvc mvc;
  @Autowired ReservaRepository reservas;
  @Autowired PagoPresencialRepository pagos;
  long id;

  @BeforeEach
  void setup() {
    pagos.deleteAll();
    reservas.deleteAll();
    Reserva r = new Reserva();
    r.setIdCabana(1L);
    r.setIdUsuario(2L);
    r.setFechaIngreso(LocalDate.now());
    r.setFechaSalida(LocalDate.now().plusDays(2));
    r.setCantidadHuespedes(2);
    r.setTarifaNocheAplicada(new BigDecimal("100"));
    r.setMontoTotal(new BigDecimal("200"));
    r.setMontoAdelanto(new BigDecimal("100"));
    r.setSaldoPendiente(new BigDecimal("100"));
    r.setExpiraEn(LocalDateTime.now().plusMinutes(15));
    id = reservas.save(r).getIdReserva();
  }

  @AfterAll
  static void cerrar() {
    identity.stop(0);
  }

  String pago(String monto, String clave) {
    return "{\"monto\":" + monto + ",\"medio\":\"EFECTIVO\",\"claveOperacion\":\"" + clave + "\"}";
  }

  @Test
  void anonimoNoCobra() throws Exception {
    mvc.perform(
            post("/api/v1/reservas/" + id + "/pagos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pago("100", "a")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void clienteNoCobraSuPropiaReserva() throws Exception {
    mvc.perform(
            post("/api/v1/reservas/" + id + "/pagos")
                .header("Authorization", token(2))
                .contentType(MediaType.APPLICATION_JSON)
                .content(pago("100", "a")))
        .andExpect(status().isForbidden());
  }

  @Test
  void pagoConfirmadoYRepetidoTieneUnSoloRegistro() throws Exception {
    mvc.perform(
            post("/api/v1/reservas/" + id + "/pagos")
                .header("Authorization", token(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(pago("100", "misma")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estadoReserva").value("CONFIRMADA"));
    mvc.perform(
            post("/api/v1/reservas/" + id + "/pagos")
                .header("Authorization", token(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(pago("100", "misma")))
        .andExpect(status().isOk());
    org.junit.jupiter.api.Assertions.assertEquals(1, pagos.count());
  }

  @Test
  void sobrepagoNoEsPersistido() throws Exception {
    mvc.perform(
            post("/api/v1/reservas/" + id + "/pagos")
                .header("Authorization", token(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(pago("201", "a")))
        .andExpect(status().isBadRequest());
    org.junit.jupiter.api.Assertions.assertEquals(0, pagos.count());
  }

  @Test
  void checkinConSaldoEsConflicto() throws Exception {
    mvc.perform(post("/api/v1/reservas/" + id + "/check-in").header("Authorization", token(1)))
        .andExpect(status().isConflict());
  }

  @Test
  void puedePagarTotalYRegistrarCheckin() throws Exception {
    mvc.perform(
            post("/api/v1/reservas/" + id + "/pagos")
                .header("Authorization", token(1))
                .contentType(MediaType.APPLICATION_JSON)
                .content(pago("200", "a")))
        .andExpect(status().isOk());
    mvc.perform(post("/api/v1/reservas/" + id + "/check-in").header("Authorization", token(1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.estadoReserva").value("OCUPADA"));
  }

  @Test
  void usuarioSoloListaSusReservas() throws Exception {
    mvc.perform(get("/api/v1/reservas").header("Authorization", token(3)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/v1/reservas").header("Authorization", token(2)))
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void detallePropioConservaFormatoDeFechas() throws Exception {
    mvc.perform(get("/api/v1/reservas/" + id).header("Authorization", token(2)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fechaIngreso").value(LocalDate.now().toString()));
  }

  @Test
  void adminConsultaReservaAunqueSeHayaEliminadoElHuesped() throws Exception {
    mvc.perform(get("/api/v1/reservas/" + id).header("Authorization", token(1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idUsuario").value(2))
        .andExpect(jsonPath("$.huesped").doesNotExist());
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

  @Test
  void adelantoEnLineaBloqueaCobroPresencialYActualizaSaldoAlConfirmar() throws Exception {
    String interno = "/api/v1/reservas/interno/" + id;
    mvc.perform(post(interno + "/iniciar-adelanto").param("idUsuario", "2")
            .header("X-Internal-Service-Token", "test-internal-token"))
        .andExpect(status().isNoContent());
    mvc.perform(post("/api/v1/reservas/" + id + "/pagos").header("Authorization", token(1))
            .contentType(MediaType.APPLICATION_JSON).content(pago("100", "otro-canal")))
        .andExpect(status().isConflict());
    mvc.perform(post(interno + "/confirmar-pago")
            .header("X-Internal-Service-Token", "test-internal-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"idTransaccionPasarela":"ORDTST123","montoAprobado":100}
                """))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/reservas/" + id).header("Authorization", token(1)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPagado").value(100))
        .andExpect(jsonPath("$.saldoPendiente").value(100))
        .andExpect(jsonPath("$.estadoReserva").value("CONFIRMADA"));
  }
}
