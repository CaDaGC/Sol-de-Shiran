# Gestión y permisos — Sol de Shirán

## Iniciar después de estos cambios

1. Conservar el contenedor y el volumen MySQL existentes. No borrar la base de datos.
2. Recargar los proyectos Maven en IntelliJ y usar JDK 21.
3. Reiniciar service-registry (8762), config-server (8089), usuario-service (8087), auth-service (8088), catalogo-service (8086), reservas-service (8085) y api-gateway (8061).
4. Config Server usa http://localhost:8762/eureka/ por defecto. Para otra dirección, establece EUREKA_CLIENT_SERVICEURL_DEFAULTZONE.
5. Reiniciar Angular desde FrontEnd con `npm start`. Entrar en http://localhost:4200/login y luego abrir **Gestión y cuenta**, o http://localhost:4200/gestion.

Hibernate añade las columnas y tablas con ddl-auto=update. Al iniciar usuarios se crean los roles faltantes sin borrar los existentes. Las cabañas antiguas sin dueño quedan bajo administración de ADMIN hasta que se les asigne uno. Las reservas antiguas reciben version=0 y se conserva el adelanto ya reconocido en reservas confirmadas; no se inventan movimientos en el libro de pagos.

## Administrador inicial

Si ya hay una cuenta ADMIN, usarla. No hay una contraseña universal.
Si no la hay, en la configuración de ejecución de **UsuarioServiceApplication** establecer:

```text
ADMIN_EMAIL=admin@solshiran.local
ADMIN_PASSWORD=UnaClaveLargaQueTuElijas2026!
```

Reiniciar usuario-service. El inicializador crea la cuenta únicamente si el correo no existe. No cambia la contraseña ni eleva los permisos de cuentas existentes. La contraseña debe tener al menos 12 caracteres. Si el correo ya pertenece a un cliente, usar otro correo. Retirar las variables tras el primer inicio si se desea.

Ahora `/usuarios/admin` requiere ADMIN incluso entrando directamente a 8087. Para crear otros administradores usar un token de administrador. Se eliminó el acceso público directo que antes permitía esa operación.

## Roles y alcance

| Acción | ADMIN | DUENO | TRABAJADOR | USUARIO |
|---|---|---|---|---|
| Catálogo y disponibilidad | Público | Público | Público | Público |
| Reserva personal y consulta propia | Sí | Sí | Sí | Sí |
| Crear/editar/desactivar cabañas | Todas | Solo propias | No | No |
| Asignar trabajadores a cabañas | Sí | Propias, trabajadores a su cargo | No | No |
| Consultar reservas operativas | Todas | Sus cabañas | Cabañas asignadas | Solo propias |
| Registrar pago, check-in y check-out | Todas | Sus cabañas | Cabañas asignadas | No |
| Cancelar reservas | Sí | Sus cabañas o propias | No | Propias antes del ingreso |
| Bloquear fechas y consultar ingresos | Todas | Sus cabañas | No | No |
| Crear administradores/dueños y cambiar roles | Sí | No | No | No |
| Crear trabajadores | Sí | A su cargo | No | No |
| Desactivar cuentas | Otras cuentas | Trabajadores a su cargo | No | No |
| Perfil y contraseña | Propios | Propios | Propios | Propios |

`DUENO` es el identificador técnico del rol Dueño. Se aceptan los alias DUEÑO/PROPIETARIO, EMPLEADO y CLIENTE para las cuentas anteriores. Las cuentas nuevas se guardan como ADMIN, DUENO, TRABAJADOR o USUARIO. El registro público siempre asigna USUARIO, aunque se envíe otro rol.

Eliminar cabañas/cuentas es desactivación lógica. Cancelar una reserva no borra sus pagos ni ejecuta reembolsos automáticos. La política implementada para cancelación personal es antes de la fecha de ingreso; ADMIN puede gestionar reservas pendientes/confirmadas y DUENO las de sus cabañas. No se cancela una reserva ocupada o finalizada.

## Postman

Usar Gateway `http://localhost:8061/api/v1`. Body raw JSON y Content-Type application/json. Todas las operaciones de gestión requieren Authorization > Bearer Token. No enviar X-User-Id: el servicio obtiene la identidad del token firmado.

### 1. Login

`POST /auth/login` — No Auth

```json
{"email":"admin@solshiran.local","password":"TU_CONTRASEÑA"}
```

Copiar `token` a Authorization > Bearer Token.

### 2. Crear cabaña — ADMIN o DUENO

`POST /cabanas` devuelve 201.

```json
{
  "nombre": "Cabaña Familiar",
  "descripcion": "Cabaña con terraza y vista al jardín.",
  "aforoMaximo": 8,
  "numHabitaciones": 3,
  "numBanos": 2,
  "tarifaNoche": 250.00,
  "imagenPrincipal": "/images/cabana-placeholder.svg",
  "imagenesSecundarias": [],
  "duenoId": null
}
```

La imagen debe existir dentro de los assets publicados por Angular o ser una URL HTTP/HTTPS válida; el endpoint no sube archivos. Para ADMIN, duenoId=null significa administración del centro; también puede especificar el ID de un DUENO activo. Para DUENO se fuerza su propio ID.

- `GET /cabanas/gestion`: listado autorizado, incluye desactivadas, dueño y trabajadores.
- `PUT /cabanas/{id}`: mismo JSON para editar.
- `DELETE /cabanas/{id}`: 204, desactiva sin borrar historial.
- `PUT /cabanas/{id}/activar` con `{}`: reactiva.
- `PUT /cabanas/{id}/trabajadores` con `[7,8]`: reemplaza asignaciones. Deben ser TRABAJADOR activos del mismo dueño; `[]` retira todas.

### 3. Crear cuentas

`POST /usuarios/register` — público, crea USUARIO.
`POST /usuarios` — ADMIN elige rol; DUENO solo crea TRABAJADOR a su cargo.

```json
{"nombre":"Dueño de prueba","email":"dueno@example.com","password":"UnaClave2026!","rol":"DUENO"}
```

Para trabajador creado por ADMIN, incluir `"rol":"TRABAJADOR","duenoId":ID_DEL_DUENO`. Para trabajadores del centro usar duenoId=null y cabañas también sin dueño. Después asignarlo con `/cabanas/{id}/trabajadores`.
`GET /usuarios`, `PUT /usuarios/{id}`, `DELETE /usuarios/{id}` respetan el alcance. `GET /usuarios/me` devuelve el perfil propio. Para cambiar contraseña propia: `PUT /usuarios/{id}/password` con `{"passwordActual":"...","password":"NuevaClave2026!"}`.

### 4. Reservas y pagos presenciales

- `GET /reservas`: ADMIN todas; DUENO/TRABAJADOR según alcance más sus propias reservas; USUARIO solo propias.
- Filtros opcionales: `?idCabana=1&desde=2026-12-01&hasta=2026-12-31`.
- `GET /reservas/{id}` y `GET /reservas/{id}/pagos`: detalle autorizado.
- `POST /reservas/{id}/pagos`:

```json
{"monto":125.00,"medio":"EFECTIVO","referencia":null,"claveOperacion":"cobro-reserva-1-adelanto-001"}
```

Medios: EFECTIVO, TARJETA, YAPE y PLIN. Referencia obligatoria salvo efectivo. Registrar solo pagos realmente recibidos. El endpoint es un registro presencial, no cobra tarjetas ni verifica transferencias con un proveedor externo.

Mantener la misma claveOperacion al reintentar el mismo cobro. La misma clave con los mismos datos no duplica el registro; si cambia monto/medio/referencia, responde conflicto. Una nueva clave representa un cobro distinto.
No se aceptan sobrepagos ni pagos de reservas vencidas/canceladas/finalizadas. El adelanto suficiente confirma la reserva; el saldo restante debe quedar en cero antes de check-in.

- `POST /reservas/{id}/check-in` con `{}`: reserva confirmada, saldo cero y fecha actual dentro de la estancia. Cambia a OCUPADA.
- `POST /reservas/{id}/check-out` con `{}`: solo OCUPADA, cambia a FINALIZADA.
- `DELETE /reservas/{id}`: cancelación según permisos y estado.
- `GET /reservas/reportes`: número de reservas e ingresos registrados para ADMIN/DUENO según alcance.
- `POST /reservas/bloqueos`: `{"idCabana":1,"desde":"2026-12-10","hasta":"2026-12-12","motivo":"Mantenimiento"}`. Hasta es inclusive. No puede cruzarse con reservas vigentes.
- `GET /reservas/bloqueos?idCabana=1` y `DELETE /reservas/bloqueos/{id}`: solo ADMIN/DUENO correspondiente.

### Casos que deben dar denegación

- Sin token en gestión: 401.
- USUARIO creando cabaña o registrando pago: 403.
- DUENO editando/desactivando cabaña de otro: 403.
- TRABAJADOR editando cabaña o cancelando reservas: 403.
- TRABAJADOR no asignado registrando pago: 403.
- Registro público con rol ADMIN: se crea USUARIO.
- Cuenta desactivada con token anterior: 401.
- Cabeceras X-User-Id/X-Role sin token no conceden acceso.

## Seguridad interna

Los microservicios verifican firma y vencimiento del JWT y consultan la cuenta activa y su rol actual. Las rutas `/internal/**` requieren X-Internal-Key y no se publican como rutas del Gateway. Configurar el mismo `SEGURIDAD_INTERNAL_KEY` en usuario-service, auth-service, catalogo-service y reservas-service; configurar el mismo `JWT_SECRET` en auth-service, api-gateway, usuario-service, catalogo-service y reservas-service. Se conserva el valor de desarrollo para la ejecución local actual; cambiar ambos secretos antes de desplegar. No exponer los puertos internos a Internet.

Por defecto las consultas internas usan usuarios en localhost:8087 y catálogo en localhost:8086; pueden cambiarse con USUARIOS_URL y CATALOGO_URL. Las respuestas públicas de usuarios no incluyen el hash de contraseña.

## Verificación

Se añadieron pruebas de reglas y pruebas HTTP con bases en memoria aisladas (H2 solo en scope test; la aplicación sigue usando MySQL). No se usan ni borran datos reales del usuario.
En este entorno se ejecuta Mockito con el agente Byte Buddy explícito porque la conexión dinámica a la JVM está bloqueada. Los resultados están en los directorios target/surefire-reports de usuarios, catálogo y reservas.
La compilación de Angular comprueba también las plantillas de la pantalla de gestión. No reemplaza la prueba manual de tu MySQL ni un cobro real.
