-- Índices de lectura para HU19. Ejecutar una sola vez en MySQL.
CREATE INDEX idx_pagos_metricas_estado_fecha
    ON db_pagos.transaccion_pago (estado_transaccion, fecha_transaccion);

CREATE INDEX idx_reservas_metricas_estado_ingreso_cabana
    ON db_reservas.reserva (estado_reserva, fecha_ingreso, id_cabana);
