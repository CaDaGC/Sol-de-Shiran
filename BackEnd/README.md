# Sol de Shirán — backend de seguridad

Java 21, Spring Boot y MySQL 8.4. El frontend lo desarrollará otra persona.
Docker Compose incluye únicamente MySQL. Los servicios Java se ejecutan desde IntelliJ con Java 21. El volumen mysql_data conserva los datos.

## Configuración y ejecución

En `.env`, configura tus credenciales de MySQL, ADMIN_EMAIL y ADMIN_PASSWORD antes del primer inicio.
El administrador inicial se crea solo si no existe: cambiar el archivo no cambia una contraseña ya guardada.
Los clientes crean su cuenta con nombre, correo y contraseña propios mediante la API de registro.
El registro público asigna CLIENTE; solo un administrador puede crear propietarios y personal.
La contraseña de la aplicación es independiente de la contraseña de Gmail.

Para enviar recuperaciones reales, completa:

```properties
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=tu-cuenta-remitente@gmail.com
SMTP_PASSWORD=tu-contraseña-de-aplicacion
SMTP_AUTH=true
SMTP_TLS=true
```

Se configura una cuenta remitente del sistema, no la contraseña de Gmail de cada cliente.
Google requiere verificación en dos pasos para crear una contraseña de aplicación:
https://support.google.com/mail/answer/185833?hl=es
No compartas ni subas `.env`. Si las credenciales SMTP están vacías, el envío no funcionará.

Desde la carpeta `BackEnd`:

```powershell
docker compose up -d mysql
```

Después de levantar MySQL, abre `BackEnd/pom.xml` como un proyecto Maven nuevo en IntelliJ,
selecciona SDK Java 21 y pulsa **Reload All Maven Projects** en la ventana Maven.
La configuración anterior de un único backend fue sustituida por cuatro configuraciones.
Ejecuta en este orden y espera el mensaje Started antes de continuar:

1. **01 - config-server** — puerto 8888.
2. **02 - discovery-server** — puerto 8761.
3. **03 - security-microservice** — puerto 8081.
4. **04 - api-gateway** — puerto 8080.

Los cuatro procesos Java se ejecutan en IntelliJ; solo MySQL usa Docker.
Cada configuración ejecuta `spring-boot:run` desde su módulo. Si no aparece, usa
la ventana Maven, módulo correspondiente, Plugins > spring-boot > spring-boot:run.
No ejecutes el objetivo desde el agregador raíz, que no tiene una clase principal.

El Config Server debe estar disponible antes de sus clientes; la importación es obligatoria
para evitar arrancar silenciosamente sin la configuración centralizada.
Su repositorio nativo `config-server/src/main/resources/config` contiene los puertos,
Eureka y rutas; no contiene claves de base de datos ni de Gmail. Seguridad carga `.env`
desde la raíz de BackEnd, tanto al ejecutarse desde su módulo como desde el proyecto.
Los servicios están enlazados a 127.0.0.1 para el desarrollo local.

El frontend debe llamar a http://localhost:8080/api a través del Gateway.
Prueba http://localhost:8080/api/salud después de que Eureka registre seguridad;
el descubrimiento puede tardar unos segundos en actualizarse.
La URL directa http://localhost:8081/api/salud permite diagnosticar seguridad.
Eureka: http://localhost:8761. Configuración pública: http://localhost:8888/security-microservice/default.
Gateway reenvía el Bearer token; los permisos se verifican en seguridad contra MySQL.
No se agregan microservicios de reservas, cabañas ni otros módulos de negocio todavía.

DBeaver: localhost, puerto 3307 (o MYSQL_PORT), base sol_shiran y credenciales
DB_USERNAME / DB_PASSWORD de `.env`. Flyway crea las tablas al iniciar seguridad.
Si antes ejecutaste todo con Docker, usa `docker compose up -d --remove-orphans mysql`
para retirar los contenedores anteriores conservando MySQL y su volumen.

## Contrato para quien implemente el frontend

Todas las peticiones JSON usan Content-Type: application/json.

| Método y ruta | Datos |
| --- | --- |
| POST /api/auth/registro | `{"nombre":"Cliente","email":"cliente@gmail.com","password":"Mi-clave-segura-123"}` |
| POST /api/auth/login | `{"email":"cliente@gmail.com","password":"Mi-clave-segura-123"}` |
| POST /api/auth/recuperacion | `{"email":"cliente@gmail.com"}` |
| POST /api/auth/restablecer | `{"codigo":"codigo-recibido-por-correo","password":"Nueva-clave-segura-456"}` |
| GET /api/auth/me | Cabecera Authorization: Bearer TOKEN |
| POST /api/auth/logout | Cabecera Authorization: Bearer TOKEN |
| POST /api/auth/password | Autenticación y `{"actual":"clave-actual","nueva":"Nueva-clave-segura-456"}` |

Login devuelve token, venceEn y usuario. Configura FRONTEND_ORIGIN con la URL del futuro frontend.
La recuperación responde genéricamente para evitar revelar cuentas registradas.
El código contiene 32 caracteres hexadecimales aleatorios; se guarda solo su hash,
vence en 15 minutos y sirve una sola vez. Solicitar uno nuevo invalida el anterior.
Restablecer la contraseña revoca las sesiones anteriores.

Si se olvida el correo registrado, este flujo no puede identificar a la persona.
Debe definirse un medio alternativo verificado (teléfono o correo secundario), o una
verificación manual por el administrador. No existe recuperación automática del correo en esta versión.
Registrar un correo no verifica por sí mismo que le pertenezca al usuario; la verificación
inicial del correo y el inicio de sesión con Google quedan fuera de esta versión.

## Organización y validación

Consulta ESTRUCTURA.md. Ejecuta `./mvnw.cmd verify` con Java 21 para las pruebas unitarias y HTTP.
Las pruebas reales contra MySQL se ejecutan aparte con el perfil mysql-it y un servidor disponible.


## Roles y creación desde Postman

| Rol del sistema | Valor JSON |
| --- | --- |
| Administrador | ADMIN_GENERAL |
| Dueño de la cabaña | PROPIETARIO |
| Empleado | EMPLEADO |
| Cliente | CLIENTE |

El registro público POST /api/auth/registro siempre asigna CLIENTE.
Para crear cuentas de otros roles, inicia sesión con el administrador y usa
POST http://localhost:8080/api/admin/usuarios, Authorization > Bearer Token,
con el token recibido al iniciar sesión. Body > raw > JSON:

```json
{
  "nombre": "Empleado de prueba",
  "email": "empleado@example.com",
  "password": "Mi-clave-segura-123",
  "rol": "EMPLEADO"
}
```

Cambia rol por PROPIETARIO, ADMIN_GENERAL o CLIENTE según corresponda.
El acceso sigue siendo POST /api/auth/login con email y password para todos los roles.
Reinicia security-microservice después de compilar: Flyway aplicará V2 automáticamente.
La migración conserva las cuentas y convierte RECEPCION, LIMPIEZA y RESTAURANTE a EMPLEADO.
Los permisos de reservas, cabañas y tareas de empleados se implementarán con esos módulos.

## Editar y eliminar usuarios desde Postman

Solo ADMIN_GENERAL. Primero inicia sesión y copia el token.
En ambas peticiones usa Authorization > Bearer Token.

PUT http://localhost:8080/api/admin/usuarios/ID

Body > raw > JSON (los cuatro campos son obligatorios):

```json
{
  "nombre": "Pedro Actualizado",
  "email": "pedro@gmail.com",
  "rol": "EMPLEADO",
  "activo": true
}
```

Respuesta: 200 con los datos actualizados. No cambia la contraseña.
Las sesiones y códigos de recuperación anteriores quedan revocados.
Si editas tu propia cuenta, debes iniciar sesión otra vez.
Correo duplicado: 409; usuario inexistente: 404; datos inválidos: 400.
El administrador no puede cambiar su propio rol ni desactivarse.

DELETE http://localhost:8080/api/admin/usuarios/ID

Sin Body. Respuesta: 204 No Content. Elimina la cuenta, sus sesiones y recuperaciones.
Conserva los eventos históricos de auditoría quitando las referencias a la cuenta borrada;
registra el ID eliminado en un evento nuevo. No puedes eliminar tu propia cuenta ni dejar
el sistema sin administradores activos. Usa PATCH /{id}/acceso con activo=false si solo
quieres desactivar una cuenta. El administrador inicial configurado en .env se vuelve a
crear al arrancar si no existe; evita eliminar esa cuenta de arranque.

Reinicia 03 - security-microservice para cargar estos endpoints.
