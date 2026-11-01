package pe.edu.upeu.soldeshiran.api_gateway.security;

import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethod().name();

        if (isPublicRoute(path, method)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.isValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        if (isAdminRoute(path, method)) {
            String rol = jwtUtil.getRol(token);

            if (!"ADMIN".equals(rol)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }

        return chain.filter(exchange);
    }

    private boolean isPublicRoute(String path, String method) {
        return "OPTIONS".equals(method)
                || path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/usuarios/register")
                || ("GET".equals(method) && path.startsWith("/api/v1/cabanas"))
                || ("GET".equals(method) && path.startsWith("/api/v1/categorias"));
    }

    private boolean isAdminRoute(String path, String method) {
        return path.startsWith("/api/v1/usuarios")
                || (path.startsWith("/api/v1/reservas") && ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
