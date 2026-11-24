package pe.edu.upeu.soldeshiran.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import pe.edu.upeu.soldeshiran.api_gateway.security.JwtAuthenticationFilter;
import pe.edu.upeu.soldeshiran.api_gateway.security.JwtUtil;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class JwtRegistroTest {
  @Test
  void registroPublicoSoloAceptaPostEnLaRutaExacta() {
    var filtro = new JwtAuthenticationFilter(mock(JwtUtil.class));
    GatewayFilterChain chain = mock(GatewayFilterChain.class);
    when(chain.filter(any())).thenReturn(Mono.empty());
    var registro = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/register"));
    filtro.filter(registro, chain).block();
    verify(chain).filter(any());

    var otraRuta = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/auth/register/extra"));
    filtro.filter(otraRuta, chain).block();
    assertEquals(HttpStatus.UNAUTHORIZED, otraRuta.getResponse().getStatusCode());

    var otroMetodo = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/auth/register"));
    filtro.filter(otroMetodo, chain).block();
    assertEquals(HttpStatus.UNAUTHORIZED, otroMetodo.getResponse().getStatusCode());
    verifyNoMoreInteractions(chain);
  }
}
