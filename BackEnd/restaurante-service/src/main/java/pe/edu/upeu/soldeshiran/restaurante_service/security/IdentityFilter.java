package pe.edu.upeu.soldeshiran.restaurante_service.security;

import feign.FeignException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.upeu.soldeshiran.restaurante_service.client.UsuariosClient;

@Component
@Order(1)
@RequiredArgsConstructor
public class IdentityFilter extends OncePerRequestFilter {

    private final TokenVerifier tokens;
    private final UsuariosClient usuarios;

    @Value("${seguridad.internal-key}")
    private String claveInterna;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(request.getServletPath().startsWith("/api/")
                || request.getServletPath().startsWith("/internal/"))
                || "OPTIONS".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        if (request.getServletPath().startsWith("/internal/")) {
            String recibida = request.getHeader("X-Internal-Key");
            if (claveInterna == null || claveInterna.isBlank() || recibida == null
                    || !java.security.MessageDigest.isEqual(
                        claveInterna.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        recibida.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                response.sendError(403, "Credencial interna inválida");
                return;
            }
            chain.doFilter(request, response);
            return;
        }
        if ("GET".equals(request.getMethod()) && request.getServletPath()
                .matches("/api/restaurante/imagenes/(locales|productos)/[0-9]+")) {
            chain.doFilter(request, response);
            return;
        }
        try {
            Long idUsuario = tokens.verificar(
                    request.getHeader(HttpHeaders.AUTHORIZATION)
            );

            var identidad = usuarios.identidad(
                    idUsuario,
                    claveInterna
            );

            if (identidad == null || !identidad.activo()) {
                response.sendError(
                        401,
                        "La cuenta no está disponible."
                );
                return;
            }

            request.setAttribute(
                    "actor",
                    new Actor(idUsuario, identidad.rol())
            );

        } catch (IllegalArgumentException | FeignException.NotFound exception) {
            response.sendError(
                    401,
                    "La sesión no es válida o ha vencido."
            );
            return;

        } catch (FeignException exception) {
            response.sendError(
                    503,
                    "No se pudo verificar la cuenta. Intenta nuevamente."
            );
            return;
        }

        chain.doFilter(request, response);
    }
}