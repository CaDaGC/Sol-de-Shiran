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

        if (path.contains("/interno/") || path.endsWith("/interno") || path.startsWith("/internal/")) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
        }
        if (isPublicRoute(path, method) && !isCatalogo(path, method)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null && isCatalogo(path, method)) {
            return chain.filter(exchange);
        }
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
            String rol = normalizar(jwtUtil.getRol(token));
            if (!permitido(rol, path, method)) {
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
                        headers.set("X-User-Role", rol);
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

        if ("POST".equals(method)) {
            return path.equals("/api/v1/auth/login")
                    || path.equals("/api/v1/usuarios/register")
                    || path.equals("/api/v1/pagos/webhook");
        }
        return false;
    }

    private boolean isCatalogo(String path, String method) {
        return "GET".equals(method)
                && (path.equals("/api/v1/cabanas")
                || path.matches("/api/v1/cabanas/[0-9]+")
                || path.matches("/api/v1/reservas/cabanas/[0-9]+/(disponibilidad|fechas-ocupadas)"));
    }

    private String normalizar(String rol) {
        if (rol == null) return "";
        return switch (rol.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "USUARIO" -> "CLIENTE";
            case "GUARDIAN", "EMPLEADO" -> "TRABAJADOR";
            default -> rol.trim().toUpperCase(java.util.Locale.ROOT);
        };
    }

    private boolean permitido(String rol, String path, String method) {
        if (path.equals("/api/v1/reservas/mis-reservas")) return "CLIENTE".equals(rol);
        if ("ADMIN".equals(rol)) return true;
        boolean cuenta = path.equals("/api/v1/usuarios/me")
                || path.matches("/api/v1/usuarios/[0-9]+(/password)?")
                    && ("GET".equals(method) || "PUT".equals(method));
        if ("TRABAJADOR".equals(rol)) {
            return cuenta || path.equals("/api/v1/reservas/panel")
                    || path.startsWith("/api/v1/reservas/panel/")
                    || path.matches("/api/v1/reservas/[0-9]+(/pagos|/check-in|/check-out)")
                    || path.matches("/api/v1/pagos/reservas/[0-9]+/restante");
        }
        if ("CLIENTE".equals(rol)) {
            return cuenta || isCatalogo(path, method)
                    || path.equals("/api/v1/reservas")
                    || path.matches("/api/v1/reservas/[0-9]+(/pagos)?")
                    || path.matches("/api/v1/pagos/reservas/[0-9]+/(brick|orden|preferencia)")
                    || path.matches("/api/v1/pagos/transacciones/[0-9]+");
        }
        return false;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
