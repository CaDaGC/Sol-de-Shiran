# Ejecutar Sol de Shirán con Docker

Desde la raíz, con Docker Desktop abierto:

```powershell
docker compose up --build -d
```

El frontend queda en http://localhost:4200, Gateway en http://localhost:8061, Eureka en http://localhost:8762 y Config Server en http://localhost:8089. Los puertos de los demás servicios se conservan para las pruebas locales.

La configuración espera a que cada aplicación responda saludable antes de arrancar la siguiente:

1. mysql
2. service-registry
3. config-server
4. usuario-service
5. auth-service
6. catalogo-service
7. reservas-service
8. pagos-service
9. resenas-service
10. api-gateway
11. frontend

Se usan `depends_on` con `service_healthy` y endpoints de readiness. Los servicios de datos incluyen la conexión a MySQL en su comprobación. El orden controla el arranque inicial; los servicios también necesitan que sus dependencias sigan disponibles durante el uso.

## Configuración privada

En esta instalación se preparó `.env` reutilizando los ajustes locales. Está ignorado por Git y por el contexto de construcción Docker. Para otro equipo, copiar `.env.example` a `.env` y completar sus propios valores. No publicar `.env` ni el archivo privado de Config Server.

MySQL forma parte de Compose, publica `127.0.0.1:3307` y guarda sus datos en el volumen `sol-de-shiran_mysql-data`. Dentro de Docker los microservicios usan `mysql:3306`; `MYSQL_PORT` solo cambia el puerto publicado en Windows. `DB_USERNAME` debe ser `root` y la contraseña se toma de `DB_PASSWORD`, en el archivo privado `.env`.

La instancia nueva comienza vacía y los microservicios crean sus tablas con Hibernate. Para crear un administrador inicial, completar `ADMIN_EMAIL` y `ADMIN_PASSWORD` en `.env` antes de iniciar. Usar una contraseña de al menos 12 caracteres. El inicializador no modifica una cuenta que ya existe; estas variables permiten acceder después a los endpoints administrativos de Postman.

El contenedor anterior `mysql_sol_shiran` y su volumen se conservan detenidos como respaldo. Además, se generó una copia SQL completa y su checksum en `backups/mysql/`, excluidos de Git y de las imágenes. No iniciar simultáneamente el contenedor anterior y el nuevo: ambos publican 3307.

Las fotos de perfiles y comprobantes se montan desde `uploads/perfiles` y `uploads/comprobantes`. Las imágenes de cabañas continúan en su almacenamiento actual en MySQL. No se incluyen datos privados dentro de las imágenes.

Para pagos reales o de prueba, configurar en `.env` `MP_ACCESS_TOKEN`, `MP_WEBHOOK_SECRET` y `MP_WEBHOOK_URL` con los valores propios de Mercado Pago. La URL de notificación debe ser accesible externamente; Docker no crea un túnel público. El retorno del navegador sigue siendo http://localhost:4200.

## Operación

```powershell
docker compose ps
docker compose logs -f reservas-service pagos-service
docker compose restart pagos-service
docker compose down
```

`down` detiene y retira los contenedores, incluido MySQL, pero conserva el volumen de datos y los directorios montados. No usar `docker compose down -v` si se quieren conservar los datos, porque elimina el volumen de MySQL. Después de modificar código, ejecutar de nuevo `docker compose up --build -d`. Tras cambiar `.env`, usar `docker compose up -d --force-recreate`.

No ejecutar simultáneamente las aplicaciones de IntelliJ/Angular y Compose sobre los mismos puertos. La primera construcción descarga las imágenes base y las dependencias de Maven y npm. Los builds posteriores reutilizan la caché.

## Volver a la base anterior

Para recuperar temporalmente el entorno anterior, detener primero esta aplicación con `docker compose down` y después iniciar `docker start mysql_sol_shiran`. Su volumen original permanece intacto. La aplicación dockerizada apunta a `mysql:3306`, por lo que ese contenedor anterior se utiliza con la configuración local de IntelliJ o mediante el puerto 3307.
