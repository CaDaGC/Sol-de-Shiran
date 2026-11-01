package pe.upeu.edu.seguridad.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import pe.upeu.edu.seguridad.entity.Usuario;
import pe.upeu.edu.seguridad.enums.Rol;

import java.sql.*;
import java.time.Instant;
import java.util.*;

@Repository
public class SeguridadRepository {
    private final JdbcTemplate jdbc;

    public SeguridadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<Usuario> USER =
            (rs, n) ->
                    new Usuario(
                            rs.getLong("id"),
                            rs.getString("nombre"),
                            rs.getString("email"),
                            rs.getString("password_hash"),
                            Rol.valueOf(rs.getString("rol")),
                            rs.getBoolean("activo"),
                            rs.getInt("intentos"),
                            rs.getTimestamp("bloqueado_hasta") == null
                                    ? null
                                    : rs.getTimestamp("bloqueado_hasta").toInstant());

    public Optional<Usuario> porCorreo(String email, boolean lock) {
        return jdbc
                .query(
                        "SELECT * FROM usuarios WHERE email=?" + (lock ? " FOR UPDATE" : ""),
                        USER,
                        email)
                .stream()
                .findFirst();
    }

    public Optional<Usuario> porId(long id) {
        return jdbc.query("SELECT * FROM usuarios WHERE id=?", USER, id).stream().findFirst();
    }

    public long crear(String nombre, String email, String password, Rol rol) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps =
                            connection.prepareStatement(
                                    "INSERT INTO usuarios(nombre,email,password_hash,rol) VALUES"
                                            + " (?,?,?,?)",
                                    new String[] {"id"});
                    ps.setString(1, nombre);
                    ps.setString(2, email);
                    ps.setString(3, password);
                    ps.setString(4, rol.name());
                    return ps;
                },
                keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }

    public void fallo(Usuario u, Instant now) {
        int count =
                u.bloqueadoHasta() != null && !u.bloqueadoHasta().isAfter(now)
                        ? 1
                        : u.intentos() + 1;
        jdbc.update(
                "UPDATE usuarios SET intentos=?,bloqueado_hasta=? WHERE id=?",
                count,
                count >= 5 ? Timestamp.from(now.plusSeconds(900)) : null,
                u.id());
    }

    public void limpiarIntentos(long id) {
        jdbc.update("UPDATE usuarios SET intentos=0,bloqueado_hasta=NULL WHERE id=?", id);
    }

    public void sesion(String hash, long id, Instant vence) {
        jdbc.update("DELETE FROM sesiones WHERE vence_en<CURRENT_TIMESTAMP");
        jdbc.update(
                "INSERT INTO sesiones(token_hash,usuario_id,vence_en) VALUES (?,?,?)",
                hash,
                id,
                Timestamp.from(vence));
    }

    public Optional<Usuario> autenticar(String hash) {
        return jdbc
                .query(
                        "SELECT u.* FROM usuarios u JOIN sesiones s ON s.usuario_id=u.id WHERE"
                            + " s.token_hash=? AND s.vence_en>CURRENT_TIMESTAMP AND u.activo=TRUE",
                        USER,
                        hash)
                .stream()
                .findFirst();
    }

    public void cerrar(String hash) {
        jdbc.update("DELETE FROM sesiones WHERE token_hash=?", hash);
    }

    public void revocar(long id) {
        jdbc.update("DELETE FROM sesiones WHERE usuario_id=?", id);
        jdbc.update("DELETE FROM recuperaciones WHERE usuario_id=?", id);
    }

    public void password(long id, String hash) {
        jdbc.update(
                "UPDATE usuarios SET password_hash=?,intentos=0,bloqueado_hasta=NULL WHERE id=?",
                hash,
                id);
        revocar(id);
    }

    public void recuperacion(String hash, long id, Instant vence) {
        jdbc.update(
                "DELETE FROM recuperaciones WHERE vence_en<CURRENT_TIMESTAMP OR usuario_id=?", id);
        jdbc.update(
                "INSERT INTO recuperaciones(token_hash,usuario_id,vence_en) VALUES (?,?,?)",
                hash,
                id,
                Timestamp.from(vence));
    }

    public Optional<Long> consumirRecuperacion(String hash) {
        var ids =
                jdbc.query(
                        "SELECT usuario_id FROM recuperaciones WHERE token_hash=? AND"
                                + " vence_en>CURRENT_TIMESTAMP FOR UPDATE",
                        (rs, n) -> rs.getLong(1),
                        hash);
        if (ids.isEmpty()) return Optional.empty();
        jdbc.update("DELETE FROM recuperaciones WHERE token_hash=?", hash);
        return Optional.of(ids.getFirst());
    }

    public List<Usuario> listar(int pagina) {
        return jdbc.query(
                "SELECT * FROM usuarios ORDER BY id LIMIT 50 OFFSET ?", USER, pagina * 50);
    }

    public List<Long> bloquearAdmins() {
        return jdbc.query(
                "SELECT id FROM usuarios WHERE rol='ADMIN_GENERAL' AND activo=TRUE ORDER BY id FOR"
                        + " UPDATE",
                (rs, n) -> rs.getLong(1));
    }

    public void cambiarAcceso(long id, Rol rol, boolean activo) {
        jdbc.update("UPDATE usuarios SET rol=?,activo=? WHERE id=?", rol.name(), activo, id);
        revocar(id);
    }

    public void auditar(Long actor, String evento, Long objetivo) {
        jdbc.update(
                "INSERT INTO auditoria(actor_id,evento,objetivo_id) VALUES (?,?,?)",
                actor,
                evento,
                objetivo);
    }

    public void editar(long id, String nombre, String email, Rol rol, boolean activo) {
        jdbc.update(
                "UPDATE usuarios SET nombre=?,email=?,rol=?,activo=? WHERE id=?",
                nombre,
                email,
                rol.name(),
                activo,
                id);
        revocar(id);
    }

    public void eliminar(long id) {
        revocar(id);
        // Conserva los eventos históricos sin una referencia a la cuenta eliminada.
        jdbc.update("UPDATE auditoria SET actor_id=NULL WHERE actor_id=?", id);
        jdbc.update("UPDATE auditoria SET objetivo_id=NULL WHERE objetivo_id=?", id);
        jdbc.update("DELETE FROM usuarios WHERE id=?", id);
    }
}
