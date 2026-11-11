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

## Prueba funcional paso a paso

1. Inicia el contenedor MySQL, service-registry, config-server, usuario-service, auth-service, catalogo-service, reservas-service, pagos-service y api-gateway. Inicia Angular con `npm start` desde `FrontEnd`.
2. Importa `docs/postman/HU19.postman_collection.json` en Postman. Define `adminToken` y `clienteToken` con JWT vigentes obtenidos mediante `POST /api/v1/auth/login`. No guardes correos ni contraseñas en la colección.
3. Ajusta `desde` y `hasta` al período que quieras comprobar. Ejecuta las cinco solicitudes de la colección en orden. Las primeras tres deben responder 200; la cuarta, 403; la quinta, 400. Para la quinta, `desde` debe ser anterior a `hasta` antes de invertirlos.
4. Usa datos de prueba con al menos una transacción `APROBADA` y una reserva `CONFIRMADA`, `OCUPADA` o `FINALIZADA` cuya `fecha_ingreso` caiga en el período. Contrasta `totalIngresos` con la suma de `ingresosPorDia` y `totalReservas` con la suma de `ranking[].reservas`. Los pagos pendientes y las reservas canceladas no deben aumentar esos totales.
5. Entra al frontend con una cuenta ADMIN y abre **Métricas** en el menú, o visita `/admin/metricas`. Prueba los rangos rápidos de 7, 30 y 90 días y un rango personalizado. Al seleccionar una barra de ingresos o una cabaña del ranking debe aparecer su detalle. Un período sin datos debe mostrar cero ingresos y un ranking vacío, sin fallo de carga.
6. Mide en Postman el tiempo de respuesta de cada endpoint con los datos reales de prueba y registra el resultado. La meta de HU19 es menos de 3 segundos. Si la primera llamada arranca un servicio o descarga dependencias, repite la medición con los servicios ya listos.

La colección solo consulta información: no inserta pagos, reservas ni usuarios. Si el panel no carga pero las dos métricas responden 200, verifica también `GET /api/v1/cabanas/gestion`, porque Angular combina las tres respuestas para mostrar los nombres de las cabañas.
