CREATE TABLE usuarios (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 nombre VARCHAR(100) NOT NULL,
 email VARCHAR(254) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 rol VARCHAR(30) NOT NULL CHECK (rol IN ('ADMIN_GENERAL','PROPIETARIO','CLIENTE','RECEPCION','LIMPIEZA','RESTAURANTE')),
 activo BOOLEAN NOT NULL DEFAULT TRUE,
 intentos INTEGER NOT NULL DEFAULT 0,
 bloqueado_hasta TIMESTAMP(6) NULL,
 creado_en TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE sesiones (
 token_hash CHAR(64) PRIMARY KEY,
 usuario_id BIGINT NOT NULL,
 vence_en TIMESTAMP(6) NOT NULL,
 FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
CREATE INDEX ix_sesiones_usuario ON sesiones(usuario_id);
CREATE INDEX ix_sesiones_vencimiento ON sesiones(vence_en);
CREATE TABLE recuperaciones (
 token_hash CHAR(64) PRIMARY KEY,
 usuario_id BIGINT NOT NULL,
 vence_en TIMESTAMP(6) NOT NULL,
 FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);
CREATE INDEX ix_recuperaciones_usuario ON recuperaciones(usuario_id);
CREATE TABLE auditoria (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 actor_id BIGINT,
 evento VARCHAR(50) NOT NULL,
 objetivo_id BIGINT,
 creado_en TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (actor_id) REFERENCES usuarios(id),
 FOREIGN KEY (objetivo_id) REFERENCES usuarios(id)
);

