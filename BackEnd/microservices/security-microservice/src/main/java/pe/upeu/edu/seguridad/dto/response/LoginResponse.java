package pe.upeu.edu.seguridad.dto.response;

import java.time.Instant;

public record LoginResponse(String token, Instant venceEn, UsuarioResponse usuario) {}
