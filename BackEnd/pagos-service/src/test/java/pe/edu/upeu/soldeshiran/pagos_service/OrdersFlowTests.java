package pe.edu.upeu.soldeshiran.pagos_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.upeu.soldeshiran.pagos_service.service.impl.PagoServiceImpl;
import pe.edu.upeu.soldeshiran.pagos_service.repository.*;
import pe.edu.upeu.soldeshiran.pagos_service.client.ReservaInternaClient;
import pe.edu.upeu.soldeshiran.pagos_service.controller.WebhookController;
import pe.edu.upeu.soldeshiran.pagos_service.dto.*;
import pe.edu.upeu.soldeshiran.pagos_service.model.*;
import pe.edu.upeu.soldeshiran.pagos_service.service.PagoService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class OrdersFlowTests {
    TransaccionPagoRepository repo;
    ReservaInternaClient reservas;
    PagoServiceImpl service;
    MockRestServiceServer server;
    TransaccionPago tx;
    String claveCheckout;
    @BeforeEach void setup() {
        repo = mock(TransaccionPagoRepository.class);
        reservas = mock(ReservaInternaClient.class);
        RestClient.Builder builder = RestClient.builder();
        // El constructor fija timeouts antes del binding del servidor de prueba.
        service = new PagoServiceImpl(repo, reservas, builder, new ObjectMapper(), "test-token");
        server = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(service, "mercadoPago", builder.build());
        ReflectionTestUtils.setField(service, "internalToken", "internal");
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:4200");
        tx = new TransaccionPago(); tx.setIdTransaccion(7L); tx.setIdReserva(3L);
        tx.setMonto(new BigDecimal("100.00"));
        when(repo.findById(7L)).thenReturn(Optional.of(tx));
        when(repo.saveAndFlush(any())).thenAnswer(i -> { TransaccionPago t=i.getArgument(0); t.setIdTransaccion(7L); claveCheckout=t.getClaveIdempotenciaCheckout(); return t; });
        when(reservas.obtenerReserva(3L,"internal")).thenReturn(new ReservaPagoResponse(
                3L, 2L, new BigDecimal("100.00"), "PENDIENTE_PAGO", LocalDateTime.now().plusMinutes(10)));
    }
    @Test void creaCheckoutProEnOrdersSinPaymentsNiPreferences() {
        server.expect(requestTo("https://api.mercadopago.com/v1/orders"))
            .andExpect(request -> assertEquals(claveCheckout, request.getHeaders().getFirst("X-Idempotency-Key")))
            .andExpect(jsonPath("$.processing_mode").value("manual"))
            .andExpect(jsonPath("$.total_amount").value("100.00"))
            .andExpect(jsonPath("$.items[0].unit_price").value("100.00"))
            .andRespond(withSuccess("{\"id\":\"ORDTST123\",\"checkout_url\":\"https://www.mercadopago.com.pe/checkout/test\"}",MediaType.APPLICATION_JSON));
        var result=service.crearPreferencia(2L,3L,new CrearPreferenciaRequest("buyer@example.com"));
        assertEquals("ORDTST123", result.orderId());
        verify(repo).save(argThat(saved -> "ORDTST123".equals(saved.getIdExternoPasarela())));
        server.verify();
    }
    @Test void conflictoTransitorioReintentaLaMismaOrdenSinOtraTransaccion() {
        server.expect(requestTo("https://api.mercadopago.com/v1/orders"))
            .andExpect(request -> assertEquals(claveCheckout, request.getHeaders().getFirst("X-Idempotency-Key")))
            .andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT));
        server.expect(requestTo("https://api.mercadopago.com/v1/orders"))
            .andExpect(request -> assertEquals(claveCheckout, request.getHeaders().getFirst("X-Idempotency-Key")))
            .andExpect(jsonPath("$.total_amount").value("100.00"))
            .andRespond(withSuccess("{\"id\":\"ORDTST123\",\"checkout_url\":\"https://www.mercadopago.com.pe/checkout/test\"}",MediaType.APPLICATION_JSON));
        assertEquals("ORDTST123",service.crearPreferencia(2L,3L,new CrearPreferenciaRequest("buyer@example.com")).orderId());
        verify(repo,times(2)).saveAndFlush(any()); server.verify();
    }
    @Test void reintentoTras409PersistenteRecuperaCuerpoYClaveSinCrearOtraTransaccion() {
        final String[] original = { null };
        for (int i=0; i<2; i++) server.expect(requestTo("https://api.mercadopago.com/v1/orders"))
            .andExpect(request -> assertEquals(claveCheckout, request.getHeaders().getFirst("X-Idempotency-Key")))
            .andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT));
        server.expect(requestTo("https://api.mercadopago.com/v1/orders"))
            .andExpect(request -> assertEquals(claveCheckout, request.getHeaders().getFirst("X-Idempotency-Key")))
            .andExpect(request -> content().string(original[0]).match(request))
            .andRespond(withSuccess("{\"id\":\"ORDTST123\",\"checkout_url\":\"https://www.mercadopago.com.pe/checkout/test\"}",MediaType.APPLICATION_JSON));
        assertThrows(ResponseStatusException.class, () -> service.crearPreferencia(2L,3L,new CrearPreferenciaRequest("buyer@example.com")));
        // Captura el registro durable con el cuerpo original para simular una nueva solicitud HTTP.
        var guardados=org.mockito.ArgumentCaptor.forClass(TransaccionPago.class);
        verify(repo,times(2)).saveAndFlush(guardados.capture());
        TransaccionPago incierto=guardados.getValue();
        assertEquals(EstadoTransaccion.PENDIENTE,incierto.getEstadoTransaccion());
        assertNotNull(incierto.getSolicitudCheckout());
        original[0] = incierto.getSolicitudCheckout();
        when(repo.findFirstByIdReservaAndTipoPagoAndEstadoTransaccionOrderByIdTransaccionDesc(3L, TransaccionPago.ONLINE_ADELANTO, EstadoTransaccion.PENDIENTE)).thenReturn(Optional.of(incierto));
        service.crearPreferencia(2L,3L,new CrearPreferenciaRequest("changed@example.com"));
        verify(repo,times(2)).saveAndFlush(any()); server.verify();
    }
    @Test void clavesNoColisionanAunqueOtraBaseUseElMismoIdLocal() {
        List<String> claves = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            server.expect(requestTo("https://api.mercadopago.com/v1/orders"))
                .andExpect(request -> {
                    String clave = request.getHeaders().getFirst("X-Idempotency-Key");
                    assertEquals(UUID.fromString(clave).toString(), clave);
                    assertEquals(claveCheckout, clave);
                    claves.add(clave);
                })
                .andRespond(withSuccess("{\"id\":\"ORDTST123\",\"checkout_url\":\"https://www.mercadopago.com.pe/checkout/test\"}", MediaType.APPLICATION_JSON));
        }
        service.crearPreferencia(2L, 3L, new CrearPreferenciaRequest("buyer@example.com"));
        service.crearPreferencia(2L, 3L, new CrearPreferenciaRequest("buyer@example.com"));
        assertNotEquals(claves.get(0), claves.get(1));
        server.verify();
    }

    @Test void claveYaUtilizadaNoSeRotaNiSeReintentaAutomaticamente() {
        tx.setSolicitudCheckout("{\"type\":\"online\",\"total_amount\":\"100.00\"}");
        when(repo.findFirstByIdReservaAndTipoPagoAndEstadoTransaccionOrderByIdTransaccionDesc(3L, TransaccionPago.ONLINE_ADELANTO, EstadoTransaccion.PENDIENTE))
                .thenReturn(Optional.of(tx));
        server.expect(requestTo("https://api.mercadopago.com/v1/orders"))
            .andExpect(header("X-Idempotency-Key", "orden-7"))
            .andExpect(content().string(tx.getSolicitudCheckout()))
            .andRespond(withStatus(org.springframework.http.HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"errors\":[{\"code\":\"idempotency_key_already_used\"}]}"));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crearPreferencia(2L, 3L, new CrearPreferenciaRequest("changed@example.com")));
        assertEquals(409, error.getStatusCode().value());
        assertEquals(EstadoTransaccion.PENDIENTE, tx.getEstadoTransaccion());
        assertNull(tx.getClaveIdempotenciaCheckout());
        verify(repo, never()).saveAndFlush(any());
        server.verify();
    }

    String order(String status, String detail, String amount) {
        return "{\"id\":\"ORDTST123\",\"external_reference\":\"7\",\"currency\":\"PEN\",\"total_amount\":\""+amount+"\",\"total_paid_amount\":\"100.00\",\"status\":\""+status+"\",\"status_detail\":\""+detail+"\"}";
    }
    void expectOrder(String response) {
        tx.setIdExternoPasarela("ORDTST123");
        when(repo.findByIdExternoPasarela("ORDTST123")).thenReturn(Optional.of(tx));
        server.expect(requestTo("https://api.mercadopago.com/v1/orders/ORDTST123"))
            .andRespond(withSuccess(response,MediaType.APPLICATION_JSON));
    }
    @Test void confirmaSoloOrdenAcreditadaYReintentaConfirmacion() {
        expectOrder(order("processed","accredited","100.00"));
        service.procesarNotificacion("ORDTST123");
        assertEquals(EstadoTransaccion.APROBADA,tx.getEstadoTransaccion());
        verify(reservas).confirmarPago(3L,"internal",new ConfirmarPagoRequest("ORDTST123",new BigDecimal("100.00")));
        server.verify();
    }
    @Test void rechazaMontoAlteradoSinConfirmarReserva() {
        expectOrder(order("processed","accredited","1.00"));
        assertThrows(ResponseStatusException.class,()->service.procesarNotificacion("ORDTST123"));
        verify(reservas,never()).confirmarPago(any(),any(),any());
    }
    @Test void noConfirmaOrdenCreadaSinPago() {
        expectOrder(order("created","","100.00"));
        service.procesarNotificacion("ORDTST123");
        assertEquals(EstadoTransaccion.PENDIENTE,tx.getEstadoTransaccion());
        verify(reservas,never()).confirmarPago(any(),any(),any());
    }
    @Test void consultaImpideVerPagoDeOtroUsuario() {
        assertThrows(ResponseStatusException.class,()->service.consultarEstado(99L,7L));
        server.verify();
    }
    @Test void webhookValidaFirmaConIdAlfanumericoEnMinusculas() throws Exception {
        var pagos=mock(PagoService.class);
        var logs=mock(WebhookLogRepository.class);
        var webhook=new WebhookController(pagos,logs,new ObjectMapper());
        ReflectionTestUtils.setField(webhook,"webhookSecret","secret");
        Mac mac=Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("secret".getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        String hash=HexFormat.of().formatHex(mac.doFinal("id:ordtst123;request-id:req;ts:123;".getBytes(StandardCharsets.UTF_8)));
        webhook.recibir("ORDTST123","ts=123,v1="+hash,"req","{\"type\":\"order\",\"data\":{\"id\":\"ORDTST123\"}}");
        verify(pagos).procesarNotificacion("ORDTST123");
        assertThrows(ResponseStatusException.class,()->webhook.recibir("ORDTST123","ts=123,v1=incorrecta","req","{}"));
    }
    @Test void reutilizaCheckoutPendienteSinCrearOtraOrden() {
        when(repo.findFirstByIdReservaAndTipoPagoAndEstadoTransaccionOrderByIdTransaccionDesc(3L, TransaccionPago.ONLINE_ADELANTO, EstadoTransaccion.PENDIENTE))
                .thenReturn(Optional.of(tx));
        expectOrder(order("created","","100.00").replace("}",",\"checkout_url\":\"https://www.mercadopago.com.pe/checkout/test\"}"));
        var result=service.crearPreferencia(2L,3L,new CrearPreferenciaRequest("buyer@example.com"));
        assertEquals("ORDTST123",result.orderId());
        verify(repo,never()).saveAndFlush(any());
        server.verify();
    }
    @Test void noDuplicaPagoDeResultadoIncierto() {
        when(repo.findFirstByIdReservaAndTipoPagoAndEstadoTransaccionOrderByIdTransaccionDesc(3L, TransaccionPago.ONLINE_ADELANTO, EstadoTransaccion.PENDIENTE))
                .thenReturn(Optional.of(tx));
        assertThrows(ResponseStatusException.class,()->service.crearPreferencia(2L,3L,new CrearPreferenciaRequest("buyer@example.com")));
        verify(repo,never()).saveAndFlush(any());
        server.verify();
    }
    @Test void reenvioReintentaConfirmacionSiReservasFallo() {
        tx.setEstadoTransaccion(EstadoTransaccion.APROBADA);
        expectOrder(order("processed","accredited","100.00"));
        service.procesarNotificacion("ORDTST123");
        verify(reservas).confirmarPago(3L,"internal",new ConfirmarPagoRequest("ORDTST123",new BigDecimal("100.00")));
        server.verify();
    }}


