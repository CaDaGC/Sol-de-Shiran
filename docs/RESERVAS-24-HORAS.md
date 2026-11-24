# Reservas de exactamente 24 horas

El cliente elige fecha y hora de ingreso en hora local de Perú (America/Lima).
Web y móvil calculan salida = ingreso + 24 horas; no existe selector de duración.
Se cobra una tarifa por noche y se conserva el adelanto de 50 %.
Adultos + niños deben estar entre 1 y el aforo de la cabaña. Backend valida nuevamente.

## Contrato
GET /api/v1/reservas/cabanas/{id}/disponibilidad:
fechaIngreso=2026-12-01&horaIngreso=14:00&fechaSalida=2026-12-02&horaSalida=14:00&cantidadHuespedes=2

POST /api/v1/reservas (autenticado como cliente):
```json
{"idCabana":1,"fechaIngreso":"2026-12-01","horaIngreso":"14:00","fechaSalida":"2026-12-02","horaSalida":"14:00","cantidadHuespedes":2}
```
400: duración distinta de 24 horas, ingreso pasado o aforo excedido.
409: intervalo reservado por otro cliente, incluso si la cotización anterior estaba libre.

GET /api/v1/reservas/cabanas/{id}/fechas-ocupadas?desde=2026-12-01&hasta=2026-12-31
conserva fechasOcupadas para clientes antiguos y agrega intervalos [{inicio, fin}].
Los clientes nuevos usan intervalos para no bloquear todo un día parcialmente ocupado.
No expone identidad del cliente que reservó.

## Consistencia
El intervalo es [ingreso, salida): un ingreso a la misma hora de la salida anterior está permitido.
Bloquean reservas CONFIRMADA/OCUPADA y PENDIENTE_PAGO vigente, de cualquier usuario.
Un bloqueo manual comprende días completos hasta las 00:00 del día siguiente al último día.
Las interfaces refrescan los horarios cada 15 segundos; creación vuelve a verificar con bloqueo
transaccional en base de datos y aislamiento READ_COMMITTED para serializar altas simultáneas.
El cobro restante y check-in se permiten únicamente dentro del intervalo exacto.

## Datos y puesta en marcha
Se agregan hora_ingreso y hora_salida; reservas previas conservan fechas y se interpretan a las 00:00.
No se cambia la duración de reservas ya registradas. No se borran reservas para migrar.
La tabla cerrojo_reservas y su fila única se preparan al arrancar el servicio con ddl-auto=update.
Reiniciar reservas-service, actualizar frontend e instalar APK 1.4 (código 5).
El APK necesita el mismo gateway/backend actualizado; no implica un despliegue en nube.

## Validación
Pruebas del servicio con H2, incluyendo dos altas concurrentes por clientes distintos.
Pruebas Angular de intervalos y fechas; pruebas Kotlin de reglas, aforo y límites de calendario.
Estas pruebas no sustituyen un recorrido con datos reales en el celular.
