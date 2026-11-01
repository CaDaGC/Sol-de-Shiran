package pe.upeu.edu.seguridad.entity;

import pe.upeu.edu.seguridad.enums.Rol;

import java.time.Instant;

public record Usuario(
        long id,
        String nombre,
        String email,
        String passwordHash,
        Rol rol,
        boolean activo,
        int intentos,
        Instant bloqueadoHasta) {}
