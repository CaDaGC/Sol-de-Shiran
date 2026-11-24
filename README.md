# Sol de Shirán

Sistema de reservas del Complejo Turístico Sol de Shirán. Integra un catálogo de cabañas, disponibilidad por fechas, reservas, pagos de adelantos con Mercado Pago, registro presencial de saldos y daños, check-in, check-out, reseñas y gestión de cuentas.

## Organización

- [BackEnd](BackEnd/README.md): microservicios Java y Spring Cloud.
- [FrontEnd](FrontEnd/README.md): aplicación web Angular.
- `compose.yml`: infraestructura y aplicaciones para ejecución conjunta.
- `docker/`: construcción de contenedores y autorización del almacenamiento.
- [Almacenamiento en Drive](docs/almacenamiento-drive.md): autorización, configuración y migración de archivos.

## Arquitectura

La aplicación web consume las API a través de un API Gateway. Los microservicios se registran en Eureka y obtienen su configuración desde Config Server. Cada servicio de negocio administra su propia base de datos MySQL. La autenticación usa JWT y los permisos se validan en el backend.

El código del backend se organiza por responsabilidad: modelos, repositorios, DTO, mapeadores, servicios, implementaciones, controladores y seguridad, según el módulo. El frontend separa páginas, componentes compartidos, servicios HTTP, modelos y guardas de navegación.

## Roles

| Rol | Funciones |
| --- | --- |
| ADMIN | Administración de cabañas y usuarios, reservas, bloqueos de fechas y métricas administrativas. |
| CLIENTE | Catálogo, reservas propias, adelantos, comprobantes, reseñas después de la estancia y perfil. |
| TRABAJADOR | Consulta general de reservas, registro presencial de pago restante, check-in, check-out y daños opcionales, además de su perfil. |
| DUENO | Rol conservado para el acceso a reportes por propiedad; la navegación web de reportes del dueño no está completada. |

## Inicio con Docker

Requiere Docker Desktop iniciado con contenedores Linux. Para desarrollo fuera de Docker se utilizan Java 21, Maven y Node.js 24.

1. Copia `.env.example` como `.env` en la raíz y completa sus valores privados. MySQL utiliza `DB_USERNAME=root` y la contraseña indicada en `DB_PASSWORD`.
2. Configura `JWT_SECRET`, `INTERNAL_KEY` e `INTERNAL_SERVICE_TOKEN`. Mantén las claves internas coherentes entre servicios. Configura las variables de Mercado Pago para utilizar la pasarela.
3. Para crear el administrador inicial en una base nueva, define `ADMIN_EMAIL` y `ADMIN_PASSWORD` antes del primer arranque. El inicializador no reemplaza cuentas existentes.
4. Desde la raíz ejecuta:

```bash
docker compose up -d --build
```

| Recurso | Dirección local |
| --- | --- |
| Aplicación web | http://localhost:4200 |
| API Gateway | http://localhost:8061 |
| Eureka | http://localhost:8762 |
| Config Server | http://localhost:8089 |
| MySQL | localhost:3307 |

Compose crea MySQL y conserva sus datos en un volumen. El puerto 3307 debe estar libre. Dentro de Docker los servicios acceden a MySQL mediante mysql:3306. El arranque espera las comprobaciones de salud de las dependencias.

## Archivos y configuración privada

Se admite almacenamiento local o Google Drive para fotos y comprobantes. El valor predeterminado es local. Para Drive se necesita `ARCHIVOS_PROVEEDOR=drive`, una autorización OAuth guardada en `privado/google-drive.json` y los identificadores de las carpetas. Los PDF mantienen sus permisos de consulta; no se publican enlaces abiertos de Drive.

Al reemplazar o retirar una foto de perfil, el archivo anterior se envía a la papelera después de confirmar el cambio en la cuenta. La papelera debe gestionarse periódicamente. Los comprobantes se conservan como historial.

No se versionan `.env`, autorizaciones OAuth, respaldos ni archivos subidos. Descargar el repositorio no copia la base de datos ni la autorización privada del almacenamiento. La activación y migración de Drive se describen en la guía enlazada.

## Operación

```bash
docker compose ps
docker compose logs -f reservas-service pagos-service
docker compose stop
docker compose up -d
docker compose down
```

Después de cambios de código utiliza `docker compose up -d --build`. Evita `docker compose down -v` si necesitas conservar la base de datos: elimina su volumen. No ejecutes simultáneamente servicios del IDE y contenedores sobre los mismos puertos.
