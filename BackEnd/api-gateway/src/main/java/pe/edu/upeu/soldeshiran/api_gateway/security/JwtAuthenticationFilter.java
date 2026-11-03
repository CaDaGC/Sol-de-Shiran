package pe.edu.upeu.soldeshiran.api_gateway.security;

import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
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

        try {
            if (isAdminRoute(path, method)
                    && !"ADMIN".equals(jwtUtil.getRol(token))) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }

            Long userId = jwtUtil.getUserId(token);

            ServerHttpRequest request = exchange.getRequest()
                    .mutate()
                    .headers(headers -> {
                        // No se permite que el cliente suplante el ID del usuario.
                        headers.remove("X-User-Id");
                        headers.set("X-User-Id", userId.toString());
                    })
                    .build();

            return chain.filter(exchange.mutate().request(request).build());
        } catch (Exception exception) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublicRoute(String path, String method) {
        if ("OPTIONS".equals(method)) {
            return true;
        }

        if (path.startsWith("/api/v1/auth/login")
                || path.startsWith("/api/v1/usuarios/register")) {
            return true;
        }

        if ("POST".equals(method) && path.equals("/api/v1/pagos/webhook")) {
            return true;
        }

        if (!"GET".equals(method)) {
            return false;
        }

        if (path.startsWith("/api/v1/cabanas")) {
            return true;
        }

        return path.startsWith("/api/v1/reservas/cabanas/")
                && (
                path.endsWith("/disponibilidad")
                        || path.endsWith("/fechas-ocupadas")
        );
    }

    private boolean isAdminRoute(String path, String method) {
        return path.startsWith("/api/v1/usuarios")
                || (path.startsWith("/api/v1/reservas")
                && ("PUT".equals(method) || "DELETE".equals(method)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}