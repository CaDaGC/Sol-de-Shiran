# Backend de Sol de Shirán

Servicios Java 21 con Spring Boot, Spring Cloud, Spring Data JPA y MySQL. Cada microservicio es un proyecto Maven independiente: no existe un POM agregador en esta carpeta.

## Servicios

| Módulo | Puerto local | Responsabilidad |
| --- | --- | --- |
| service-registry | 8762 | Registro y descubrimiento con Eureka. |
| config-server | 8089 | Configuración centralizada. |
| usuario-service | 8087 | Roles, cuentas, datos personales y fotografías de perfil. |
| auth-service | 8088 | Autenticación y emisión de JWT. |
| catalogo-service | 8086 | Cabañas, fotografías, propietarios y ocupación. |
| reservas-service | 8085 | Disponibilidad, reservas temporales, códigos RSS, bloqueos y estancias. |
| pagos-service | 8083 | Mercado Pago, pagos presenciales, conciliación y comprobantes PDF. |
| resenas-service | 8090 | Valoraciones de una a cinco estrellas y comentarios por estancia. |
| api-gateway | 8061 | Entrada de las API y controles de acceso. |

Verifica los puertos en el Compose de la raíz antes de combinar ejecuciones del IDE y Docker.

## Capas y comunicación

Los módulos separan entidades JPA, repositorios, DTO de entrada y salida, mapeadores, servicios de negocio, implementaciones, controladores y seguridad. Se utilizan clientes Feign y HTTP para la comunicación entre servicios. Los identificadores de usuario y cabaña se conservan como referencias entre dominios; cada servicio administra sus propias tablas.

Los tokens JWT identifican al usuario. Las credenciales internas autentican llamadas entre microservicios y no equivalen a una llamada a Mercado Pago. Las operaciones presenciales de saldo y daños se guardan en el sistema sin invocar la pasarela.

## Reglas principales

- El registro público crea una cuenta CLIENTE junto con sus datos personales. Las cuentas administrativas pueden no tener persona asociada.
- Los usuarios se desactivan de forma lógica para conservar referencias e historial.
- Las reservas reciben automáticamente un código con formato RSS00001 y numeración sucesiva.
- Una reserva temporal dispone de 15 minutos para pagar. El proceso de limpieza elimina reservas expiradas; las demás se conservan.
- La disponibilidad y los cruces se validan en el backend, independientemente del cliente que reserve.
- El adelanto se procesa con Mercado Pago. El saldo y los daños opcionales se registran como pagos presenciales diferenciados por tipo.
- Los comprobantes PDF corresponden al adelanto aprobado. Los clientes consultan los suyos; el personal puede consultarlos después del check-in.
- Se permite una reseña por estancia finalizada, con puntuación entre uno y cinco.

## Almacenamiento

`almacenamiento-compartido` contiene el adaptador local y el adaptador de Google Drive. Sus fuentes se compilan dentro de usuario-service, catalogo-service y pagos-service mediante Maven; no se ejecuta como servicio adicional.

Las API mantienen sus rutas al cambiar de proveedor. La base de datos guarda la referencia del archivo y los metadatos. Durante la transición se admite leer fotografías antiguas de cabañas almacenadas en MySQL y archivos locales anteriores. La migración opcional verifica el contenido antes de cambiar la referencia.

Consulta [la guía de Drive](../docs/almacenamiento-drive.md). Las credenciales deben permanecer fuera del repositorio y del contexto de construcción Docker.

## Ejecución

La opción integrada se ejecuta desde la raíz:

```bash
docker compose up -d --build
```

Orden de dependencias: MySQL, Eureka, Config Server, usuarios, autenticación, catálogo, reservas, pagos, reseñas y Gateway. El frontend se inicia después del Gateway.

Para ejecutar desde IntelliJ, importa el POM de cada módulo, selecciona JDK 21 y configura las variables del proceso. Spring Boot no carga automáticamente el archivo .env. Los nombres DNS de Compose, como mysql o config-server, deben adaptarse a localhost cuando el proceso corre fuera de Docker.

Para recibir notificaciones de Mercado Pago se necesita una URL HTTPS accesible externamente que llegue al webhook del Gateway. Docker no publica un túnel a Internet.

## Compilación y pruebas

Desde el módulo correspondiente:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

También puede utilizarse Maven instalado con `mvn test`. Las pruebas de persistencia utilizan H2 en varios módulos. Revisa la configuración de cada prueba de contexto: algunas pueden consultar Config Server o Eureka si no se deshabilitan explícitamente. Los cambios de almacenamiento incluyen pruebas con HTTP simulado y comprobación de permisos y transacciones.
