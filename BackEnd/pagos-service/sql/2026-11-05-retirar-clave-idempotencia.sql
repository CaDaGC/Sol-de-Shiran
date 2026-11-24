-- Ejecutar una sola vez en la base local db_pagos después de actualizar ms-pagos.
-- Conserva los valores antiguos para poder recuperarlos si fuese necesario.
CREATE TABLE IF NOT EXISTS db_pagos.transaccion_pago_idempotencia_backup_20261105 (
    id_transaccion BIGINT NOT NULL PRIMARY KEY,
    clave_idempotencia VARCHAR(80) NOT NULL
);

INSERT INTO db_pagos.transaccion_pago_idempotencia_backup_20261105
    (id_transaccion, clave_idempotencia)
SELECT id_transaccion, clave_idempotencia
FROM db_pagos.transaccion_pago
WHERE clave_idempotencia IS NOT NULL
ON DUPLICATE KEY UPDATE clave_idempotencia = VALUES(clave_idempotencia);

ALTER TABLE db_pagos.transaccion_pago DROP COLUMN clave_idempotencia;
