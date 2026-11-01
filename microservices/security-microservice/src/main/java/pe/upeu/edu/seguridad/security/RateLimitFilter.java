package pe.upeu.edu.seguridad.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private record Ventana(long inicio, int intentos) {}

    private final Map<String, Ventana> intentos = new HashMap<>();

    // Solo getRemoteAddr: no se confia en un X-Forwarded-For enviado por el cliente.
    synchronized boolean permitir(String address, long now) {
        intentos.entrySet().removeIf(e -> now - e.getValue().inicio() > 60000);
        var v = intentos.get(address);
        if (v == null) {
            if (intentos.size() >= 10000) return false;
            intentos.put(address, new Ventana(now, 1));
            return true;
        }
        if (v.intentos() >= 30) return false;
        intentos.put(address, new Ventana(v.inicio(), v.intentos() + 1));
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if ("POST".equals(request.getMethod())
                && request.getRequestURI().startsWith("/api/auth/")
                && !permitir(request.getRemoteAddr(), System.currentTimeMillis())) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"Demasiados intentos. Espera un minuto.\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
