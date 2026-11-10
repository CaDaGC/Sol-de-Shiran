package pe.edu.upeu.soldeshiran.resenas_service.security;

import feign.FeignException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.edu.upeu.soldeshiran.resenas_service.client.UsuariosClient;

@Component
@RequiredArgsConstructor
public class IdentityFilter extends OncePerRequestFilter {
    public static final String CLIENTE_ID = "clienteResenaId";
    private final TokenVerifier tokens;
    private final UsuariosClient usuarios;

    @Value("${seguridad.internal-key:sol-shiran-interno-desarrollo-cambiar}")
    private String clave;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/api/v1/resenas/")
                || "OPTIONS".equals(request.getMethod())
                || ("GET".equals(request.getMethod()) && path.matches("/api/v1/resenas/cabanas/[0-9]+/?"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            Long id = tokens.verificar(request.getHeader(HttpHeaders.AUTHORIZATION));
            var identidad = usuarios.identidad(id, clave);
            if (identidad == null || !identidad.activo() || !"CLIENTE".equals(identidad.rol())) {
                response.sendError(403, "Solo los clientes activos pueden publicar reseñas.");
                return;
            }
            request.setAttribute(CLIENTE_ID, id);
        } catch (IllegalArgumentException | FeignException.NotFound e) {
            response.sendError(401, "Sesión inválida.");
            return;
        } catch (FeignException e) {
            response.sendError(503, "No se pudo verificar la cuenta.");
            return;
        }
        chain.doFilter(request, response);
    }
}
