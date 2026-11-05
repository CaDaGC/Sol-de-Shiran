package pe.edu.upeu.soldeshiran.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import pe.edu.upeu.soldeshiran.api_gateway.security.JwtAuthenticationFilter;
import pe.edu.upeu.soldeshiran.api_gateway.security.JwtUtil;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtPanelReservasTest {
    @Test
    void clienteConsultaSusReservasSinSuplantarElIdDelToken() {
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.isValid("token")).thenReturn(true);
        when(jwt.getRol("token")).thenReturn("CLIENTE");
        when(jwt.getUserId("token")).thenReturn(5L);
        GatewayFilterChain chain = exchange -> {
            assertThat(exchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("5");
            return Mono.empty();
        };
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/reservas/mis-reservas")
                .header("Authorization", "Bearer token").header("X-User-Id", "999"));
        new JwtAuthenticationFilter(jwt).filter(exchange, chain).block();
        for (String rol : List.of("GUARDIAN", "ADMIN")) {
            when(jwt.getRol("token")).thenReturn(rol);
            var denegado = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/reservas/mis-reservas")
                    .header("Authorization", "Bearer token"));
            new JwtAuthenticationFilter(jwt).filter(denegado, chain).block();
            assertThat(denegado.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Test
    void listadoYResumenesRequierenGuardian() {
        for (String path : List.of("/api/v1/reservas", "/api/v1/reservas/1",
                "/api/v1/cabanas/resumen", "/api/v1/usuarios/resumen",
                "/api/v1/pagos/reservas/1/restante")) {
            JwtUtil jwt = mock(JwtUtil.class);
            when(jwt.isValid("token")).thenReturn(true);
            when(jwt.getRol("token")).thenReturn("CLIENTE");
            when(jwt.getUserId("token")).thenReturn(1L);
            var filtro = new JwtAuthenticationFilter(jwt);
            GatewayFilterChain chain = mock(GatewayFilterChain.class);
            when(chain.filter(any())).thenReturn(Mono.empty());
            var anonimo = MockServerWebExchange.from(MockServerHttpRequest.get(path));
            filtro.filter(anonimo, chain).block();
            assertThat(anonimo.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            var cliente = MockServerWebExchange.from(MockServerHttpRequest.get(path).header("Authorization", "Bearer token"));
            filtro.filter(cliente, chain).block();
            assertThat(cliente.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verifyNoInteractions(chain);
            when(jwt.getRol("token")).thenReturn("ADMIN");
            var admin = MockServerWebExchange.from(MockServerHttpRequest.get(path).header("Authorization", "Bearer token"));
            filtro.filter(admin, chain).block();
            assertThat(admin.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            verifyNoInteractions(chain);
            when(jwt.getRol("token")).thenReturn("GUARDIAN");
            var guardian = MockServerWebExchange.from(MockServerHttpRequest.get(path).header("Authorization", "Bearer token"));
            filtro.filter(guardian, chain).block();
            verify(chain).filter(any());
        }
    }

    @Test
    void registrarSaldoRequiereGuardianYNoConfiaEnElRolEnviadoPorElCliente() {
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.isValid("token")).thenReturn(true);
        when(jwt.getUserId("token")).thenReturn(5L);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenAnswer(invocation -> {
            org.springframework.web.server.ServerWebExchange exchange = invocation.getArgument(0);
            assertThat(exchange.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("GUARDIAN");
            return Mono.empty();
        });
        for (String rol : List.of("CLIENTE", "ADMIN", "GUARDIAN")) {
            when(jwt.getRol("token")).thenReturn(rol);
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/pagos/reservas/1/restante")
                    .header("Authorization", "Bearer token")
                    .header("X-User-Role", rol.equals("GUARDIAN") ? "ADMIN" : "GUARDIAN"));
            new JwtAuthenticationFilter(jwt).filter(exchange, chain).block();
            if (!rol.equals("GUARDIAN")) {
                assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            } else {
                assertThat(exchange.getResponse().getStatusCode()).isNull();
            }
        }
        verify(chain).filter(any());
    }

    @Test
    void guardianNoObtienePermisosDeAdministracionDeUsuarios() {
        JwtUtil jwt = mock(JwtUtil.class);
        when(jwt.isValid("token")).thenReturn(true);
        when(jwt.getRol("token")).thenReturn("GUARDIAN");
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/usuarios")
                .header("Authorization", "Bearer token"));
        new JwtAuthenticationFilter(jwt).filter(exchange, chain).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(chain);
    }

    @Test
    void catalogoYDisponibilidadSiguenSiendoPublicos() {
        JwtUtil jwt = mock(JwtUtil.class);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        for (String path : List.of("/api/v1/cabanas", "/api/v1/cabanas/1",
                "/api/v1/reservas/cabanas/1/disponibilidad", "/api/v1/reservas/cabanas/1/fechas-ocupadas")) {
            new JwtAuthenticationFilter(jwt).filter(
                    MockServerWebExchange.from(MockServerHttpRequest.get(path)), chain).block();
        }
        verify(chain, times(4)).filter(any());
        verifyNoInteractions(jwt);
    }
}
