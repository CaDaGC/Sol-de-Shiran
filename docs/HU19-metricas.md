# HU19 · Métricas de ventas y reservas

La ruta `/admin/metricas` muestra ingresos diarios y frecuencia de reservas por cabaña. Requiere una cuenta ADMIN. Angular consulta en paralelo, mediante el API Gateway, a ms-pagos, ms-reservas y al catálogo para obtener los nombres de las cabañas.

## Definición de las métricas

- **Ingresos:** suma de `transaccion_pago.monto` con estado `APROBADA`. Se agrupa por `fecha_transaccion`, que representa el registro de la transacción local. Incluye adelantos, saldo y cobros por daños. Los registros históricos sin fecha quedan fuera del período.
- **Frecuencia:** número de reservas con estado `CONFIRMADA`, `OCUPADA` o `FINALIZADA`, agrupadas por cabaña y filtradas por `fecha_ingreso`. Excluye reservas pendientes, vencidas y canceladas.
- El rango de consulta incluye ambos extremos y admite como máximo 367 días. Los días sin ingresos aparecen con valor cero en el gráfico.

El script `BackEnd/metricas-hu19-indices.sql` añade índices para filtrar por estado y fecha en MySQL. Ejecútalo una sola vez cuando integres la HU en la base compartida; no cambia registros ni estados.

## Consultas de prueba

Con un JWT de ADMIN y el Gateway activo:

```http
GET http://localhost:8061/api/v1/pagos/metricas/ingresos?desde=2026-11-01&hasta=2026-11-30
Authorization: Bearer <token-admin>
```

```http
GET http://localhost:8061/api/v1/reservas/metricas/frecuencia?desde=2026-11-01&hasta=2026-11-30
Authorization: Bearer <token-admin>
```

Comprobar con Postman que una cuenta CLIENTE recibe 403, que `desde > hasta` recibe 400, que pagos pendientes no suman y que reservas canceladas no aparecen en el ranking. Ejecutar las consultas con datos de prueba representativos y medir el tiempo de respuesta; la meta de la HU es menos de 3 segundos. Una prueba unitaria no acredita ese tiempo en el entorno real.

Pruebas automáticas aisladas: `MetricasIngresosControllerTest` y `MetricasReservasControllerTest` verifican filtros, sumas y acceso ADMIN en H2.
