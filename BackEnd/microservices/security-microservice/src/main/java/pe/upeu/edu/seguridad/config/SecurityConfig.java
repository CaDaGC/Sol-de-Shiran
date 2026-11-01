package pe.upeu.edu.seguridad.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import pe.upeu.edu.seguridad.security.RateLimitFilter;
import pe.upeu.edu.seguridad.security.TokenAuthenticationFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity
@org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain security(
            HttpSecurity http,
            TokenAuthenticationFilter bearer,
            RateLimitFilter limiter,
            UrlBasedCorsConfigurationSource corsSource)
            throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsSource))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        a ->
                                a.requestMatchers(
                                                org.springframework.http.HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/api/salud",
                                                "/api/auth/login",
                                                "/api/auth/registro",
                                                "/api/auth/recuperacion",
                                                "/api/auth/restablecer",
                                                "/error")
                                        .permitAll()
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN_GENERAL")
                                        .requestMatchers("/api/propietario/**")
                                        .hasAnyRole("ADMIN_GENERAL", "PROPIETARIO")
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        e ->
                                e.authenticationEntryPoint(
                                                (req, res, ex) -> {
                                                    res.setStatus(401);
                                                    res.setContentType(
                                                            "application/json;charset=UTF-8");
                                                    res.getWriter()
                                                            .write(
                                                                    "{\"message\":\"Inicia sesión"
                                                                        + " para continuar.\"}");
                                                })
                                        .accessDeniedHandler(
                                                (req, res, ex) -> {
                                                    res.setStatus(403);
                                                    res.setContentType(
                                                            "application/json;charset=UTF-8");
                                                    res.getWriter()
                                                            .write(
                                                                    "{\"message\":\"No tienes"
                                                                            + " permiso para esta"
                                                                            + " acción.\"}");
                                                }))
                .headers(
                        h ->
                                h.contentSecurityPolicy(
                                        c ->
                                                c.policyDirectives(
                                                        "default-src 'none'; frame-ancestors"
                                                                + " 'none'")))
                .addFilterBefore(bearer, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(limiter, TokenAuthenticationFilter.class)
                .build();
    }

    @Bean
    UrlBasedCorsConfigurationSource cors(@Value("${app.cors-origin}") String origin) {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // Cada filtro se ejecuta solo dentro de Spring Security, no duplicado por Tomcat.
    @Bean
    FilterRegistrationBean<TokenAuthenticationFilter> bearerRegistration(
            TokenAuthenticationFilter filter) {
        var r = new FilterRegistrationBean<>(filter);
        r.setEnabled(false);
        return r;
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter> limiterRegistration(RateLimitFilter filter) {
        var r = new FilterRegistrationBean<>(filter);
        r.setEnabled(false);
        return r;
    }
}
