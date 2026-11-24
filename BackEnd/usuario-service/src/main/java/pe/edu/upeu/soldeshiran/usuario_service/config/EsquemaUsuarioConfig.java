package pe.edu.upeu.soldeshiran.usuario_service.config;

import java.nio.file.*;
import java.sql.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration
public class EsquemaUsuarioConfig {
  @Bean
  static BeanPostProcessor adaptarEsquemaUsuarios(Environment environment) {
    return new BeanPostProcessor() {
      @Override public Object postProcessAfterInitialization(Object bean, String nombre) {
        if (bean instanceof DataSource ds) {
          try (var c = ds.getConnection()) {
            if (!c.getMetaData().getDatabaseProductName().equals("MySQL")) return bean;
            renombrarTabla(c, "roles", "rol");
            renombrarTabla(c, "usuarios", "usuario");
            if (existeTabla(c, "rol")) renombrarColumna(c, "rol", "id", "id_rol");
            if (existeTabla(c, "usuario")) {
              renombrarColumna(c, "usuario", "id", "id_usuario");
              renombrarColumna(c, "usuario", "nombre", "nombre_usuario");
              renombrarColumna(c, "usuario", "rol_id", "id_rol");
              renombrarColumna(c, "usuario", "activo", "estado");
              migrarFotos(c, environment);
              if (existeColumna(c, "usuario", "dueno_id"))
                try (var s=c.createStatement()) { s.execute("ALTER TABLE usuario DROP COLUMN dueno_id"); }
              try (var s = c.createStatement()) {

                var duplicados = new java.util.ArrayList<Long>();
                try (var r = s.executeQuery("""
                    SELECT u.id_usuario FROM usuario u
                    WHERE u.nombre_usuario IS NULL OR TRIM(u.nombre_usuario) = ''
                       OR CHAR_LENGTH(u.nombre_usuario) > 80
                       OR EXISTS (SELECT 1 FROM usuario v WHERE v.id_usuario < u.id_usuario
                                  AND v.nombre_usuario = u.nombre_usuario)
                    """)) {
                  while (r.next()) duplicados.add(r.getLong(1));
                }
                for (Long id : duplicados) {
                  String alias = "usuario-" + id;
                  int intento = 0;
                  while (aliasExiste(c, alias, id)) alias = "usuario-" + id + "-" + (++intento);
                  try (var p = c.prepareStatement("UPDATE usuario SET nombre_usuario=? WHERE id_usuario=?")) {
                    p.setString(1, alias); p.setLong(2, id); p.executeUpdate();
                  }
                }
              }
            }
          } catch (Exception e) {
            throw new IllegalStateException("No se pudo adaptar el esquema de Usuarios conservando sus datos", e);
          }
        }
        return bean;
      }
    };
  }

  private static boolean aliasExiste(Connection c, String alias, Long id) throws SQLException {
    try (var p = c.prepareStatement("SELECT COUNT(*) FROM usuario WHERE nombre_usuario=? AND id_usuario<>?")) {
      p.setString(1, alias); p.setLong(2, id);
      try (var r=p.executeQuery()) { r.next(); return r.getLong(1)>0; }
    }
  }
  private static boolean existeTabla(Connection c, String tabla) throws SQLException {
    try (var r=c.getMetaData().getTables(c.getCatalog(), null, tabla, new String[]{"TABLE"})) { return r.next(); }
  }
  private static boolean existeColumna(Connection c, String tabla, String columna) throws SQLException {
    try (var r=c.getMetaData().getColumns(c.getCatalog(), null, tabla, columna)) { return r.next(); }
  }
  private static void renombrarTabla(Connection c, String anterior, String nueva) throws SQLException {
    if (!existeTabla(c, anterior)) return;
    if (existeTabla(c, nueva)) throw new SQLException("Existen ambas tablas: " + anterior + " y " + nueva);
    try (var s=c.createStatement()) { s.execute("RENAME TABLE " + anterior + " TO " + nueva); }
  }
  private static void renombrarColumna(Connection c, String tabla, String anterior, String nueva) throws SQLException {
    if (existeColumna(c, tabla, anterior) && !existeColumna(c, tabla, nueva))
      try (var s=c.createStatement()) { s.execute("ALTER TABLE " + tabla + " RENAME COLUMN " + anterior + " TO " + nueva); }
  }
  private static void migrarFotos(Connection c, Environment env) throws Exception {
    if (!existeColumna(c, "usuario", "foto_perfil")) {
      if (existeColumna(c,"usuario","foto_perfil_url"))
        renombrarColumna(c,"usuario","foto_perfil_url","foto_perfil");
      return;
    }
    String tipo;
    try (var r=c.getMetaData().getColumns(c.getCatalog(), null, "usuario", "foto_perfil")) {
      r.next(); tipo=r.getString("TYPE_NAME");
    }
    if (!tipo.toUpperCase().contains("BLOB")) return;
    Path carpeta=Paths.get(env.getProperty("usuarios.fotos.directorio", "uploads/perfiles")).toAbsolutePath().normalize();
    Files.createDirectories(carpeta);
    try (var s=c.createStatement(); var r=s.executeQuery("SELECT id_usuario,foto_perfil FROM usuario WHERE foto_perfil IS NOT NULL")) {
      while (r.next()) {
        byte[] bytes=r.getBytes(2);
        String extension=bytes.length>0 && bytes[0]==(byte)0x89 ? ".png" : ".jpg";
        Files.write(carpeta.resolve("migrada-"+r.getLong(1)+extension),bytes);
      }
    }
    try (var s=c.createStatement()) {

      if (!existeColumna(c,"usuario","foto_perfil_url"))
        s.execute("ALTER TABLE usuario ADD COLUMN foto_perfil_url VARCHAR(500) NULL");
      try(var r=s.executeQuery("SELECT id_usuario,foto_perfil FROM usuario WHERE foto_perfil IS NOT NULL")) {
        while(r.next()) {
          byte[] bytes=r.getBytes(2);
          String extension=bytes.length>0 && bytes[0]==(byte)0x89 ? ".png" : ".jpg";
          Path archivo=carpeta.resolve("migrada-"+r.getLong(1)+extension);
          if (!java.util.Arrays.equals(bytes,Files.readAllBytes(archivo))) throw new SQLException("Foto migrada diferente");
          try(var p=c.prepareStatement("UPDATE usuario SET foto_perfil_url=? WHERE id_usuario=?")) {
            p.setString(1,"/api/v1/usuarios/fotos/"+archivo.getFileName()); p.setLong(2,r.getLong(1)); p.executeUpdate();
          }
        }
      }
      s.execute("ALTER TABLE usuario DROP COLUMN foto_perfil");
      s.execute("ALTER TABLE usuario RENAME COLUMN foto_perfil_url TO foto_perfil");
      if (existeColumna(c,"usuario","tipo_foto_perfil"))
        s.execute("ALTER TABLE usuario DROP COLUMN tipo_foto_perfil");
    }
  }
}
