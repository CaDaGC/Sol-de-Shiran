-- V1 creó esta restricción con el nombre automático de MySQL.
-- No se modifica V1 porque ya está aplicada en la base de datos.
ALTER TABLE usuarios DROP CHECK usuarios_chk_1;

UPDATE usuarios
SET rol = 'EMPLEADO'
WHERE rol IN ('RECEPCION', 'LIMPIEZA', 'RESTAURANTE');

ALTER TABLE usuarios
ADD CONSTRAINT ck_usuarios_rol
CHECK (rol IN ('ADMIN_GENERAL', 'PROPIETARIO', 'EMPLEADO', 'CLIENTE'));
