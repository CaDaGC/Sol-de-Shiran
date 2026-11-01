package pe.upeu.edu.seguridad.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import pe.upeu.edu.seguridad.service.AuthService;

import java.io.IOException;
import java.util.List;

@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private final AuthService auth;

    public TokenAuthenticationFilter(AuthService auth) {
        this.auth = auth;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            auth.autenticar(header.substring(7))
                    .ifPresent(
                            user -> {
                                var authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                user,
                                                null,
                                                List.of(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_" + user.rol().name())));
                                SecurityContextHolder.getContext()
                                        .setAuthentication(authentication);
                            });
        }
        chain.doFilter(request, response);
    }
}
