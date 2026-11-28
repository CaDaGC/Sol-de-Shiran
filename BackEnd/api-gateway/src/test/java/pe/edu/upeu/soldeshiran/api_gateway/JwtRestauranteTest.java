package pe.edu.upeu.soldeshiran.api_gateway;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import pe.edu.upeu.soldeshiran.api_gateway.security.*;
import reactor.core.publisher.Mono;

class JwtRestauranteTest {
    @Test
    void encargadoAccedeSoloAlModuloRestauranteYSuCuenta() {
        var jwt = mock(JwtUtil.class);
        when(jwt.isValid("token")).thenReturn(true);
        when(jwt.getRol("token")).thenReturn("RESTAURANTE");
        when(jwt.getUserId("token")).thenReturn(7L);
        var filtro = new JwtAuthenticationFilter(jwt);
        for (String ruta : List.of("/api/pedidos/restaurante/1", "/api/restaurante/mi-local/7", "/api/v1/usuarios/me",
                "/api/restaurante/locales/1/carta-operativa")) {
            var cadena = mock(GatewayFilterChain.class);
            when(cadena.filter(any())).thenReturn(Mono.empty());
            var e = MockServerWebExchange.from(MockServerHttpRequest.get(ruta).header("Authorization", "Bearer token"));
            filtro.filter(e, cadena).block();
            verify(cadena).filter(any());
        }
        for (String ruta : List.of("/api/restaurante/admin/locales", "/api/pedidos/usuario/42", "/api/v1/reservas/panel")) {
            var cadena = mock(GatewayFilterChain.class);
            var e = MockServerWebExchange.from(MockServerHttpRequest.get(ruta).header("Authorization", "Bearer token"));
            filtro.filter(e, cadena).block();
            assertThat(e.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verifyNoInteractions(cadena);
        }
    }

    @Test
    void permitePatchOperativoPeroNoCreacionDePedidosAlEncargado() {
        var jwt = mock(JwtUtil.class);
        when(jwt.isValid("token")).thenReturn(true);
        when(jwt.getRol("token")).thenReturn("RESTAURANTE");
        when(jwt.getUserId("token")).thenReturn(7L);
        var filtro = new JwtAuthenticationFilter(jwt);
        for (String ruta : List.of("/api/pedidos/restaurante/1/2/estado", "/api/restaurante/locales/1/productos/3/disponibilidad")) {
            var cadena = mock(GatewayFilterChain.class);
            when(cadena.filter(any())).thenReturn(Mono.empty());
            filtro.filter(MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.PATCH, ruta)
                    .header("Authorization", "Bearer token")), cadena).block();
            verify(cadena).filter(any());
        }
        var e = MockServerWebExchange.from(MockServerHttpRequest.post("/api/pedidos").header("Authorization", "Bearer token"));
        filtro.filter(e, mock(GatewayFilterChain.class)).block();
        assertThat(e.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void imagenEsPublicaYEndpointInternoEstaBloqueado() {
        var jwt = mock(JwtUtil.class);
        var cadena = mock(GatewayFilterChain.class);
        when(cadena.filter(any())).thenReturn(Mono.empty());
        var filtro = new JwtAuthenticationFilter(jwt);
        filtro.filter(MockServerWebExchange.from(MockServerHttpRequest.get("/api/restaurante/imagenes/productos/3")), cadena).block();
        verify(cadena).filter(any());
        verifyNoInteractions(jwt);
        var interno = MockServerWebExchange.from(MockServerHttpRequest.get("/internal/restaurante/reservas/1/pendientes"));
        filtro.filter(interno, cadena).block();
        assertThat(interno.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
