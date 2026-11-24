# Almacenamiento en Google Drive

La integración está preparada, pero no está activa hasta autorizar una cuenta de Google. La conexión de Google Drive al chat no autoriza al backend. No se ha migrado ni eliminado ningún archivo existente.

## Carpetas

- Raíz: https://drive.google.com/drive/folders/1wWseqcwo0AZvpixLeP9GBVrVxa9ak7QH
- Fotos de perfil: https://drive.google.com/drive/folders/1DJowW2-fEZ5gdKanuj4P9KufWwRQIO7_
- Fotos de cabañas: https://drive.google.com/drive/folders/13KmUpK4yKJ0NMMjTOCJdyL4SQwcEs8XR
- Comprobantes: https://drive.google.com/drive/folders/1bS3PGBQ635RVoXGkplo8ZfP_FKNeErFj

Se conservan los permisos privados de Drive. Las fotos se sirven desde los endpoints actuales del sistema; los comprobantes mantienen la autorización por usuario y rol. No se generan enlaces públicos de Drive ni se incluyen tokens en la web.

## Configuración desde cero

1. Abre https://console.cloud.google.com/ con la cuenta que administra esas carpetas.
2. Crea o selecciona un proyecto, por ejemplo Sol de Shiran.
3. En APIs y servicios, Biblioteca, habilita Google Drive API.
4. En Google Auth Platform configura la información de la aplicación y tu correo. Si el proyecto es externo y está en pruebas, añade tu propia cuenta como usuario de prueba.
5. En Clientes crea un cliente OAuth 2.0 de tipo Aplicación de escritorio y descarga su JSON. Guárdalo fuera de Git, por ejemplo en Documentos. No uses una clave API ni una cuenta de servicio para sustituir esta autorización de Drive personal.
6. Desde una terminal en la raíz del repositorio ejecuta:

```powershell
node docker/autorizar-drive.mjs "C:\ruta\cliente-oauth.json"
```

7. Abre el enlace que muestra el comando. Revisa el consentimiento de Google y autoriza con la cuenta propietaria de las carpetas. Este paso lo realiza el propietario. El programa usa un retorno temporal en 127.0.0.1, comprueba el estado y usa PKCE. No imprime el refresh token ni el secreto del cliente.
8. Se creará privado/google-drive.json. La carpeta privado está excluida de Git. El JSON original de OAuth también debe mantenerse fuera de Git.

Si configuras una aplicación externa en estado Pruebas, Google limita normalmente su refresh token a 7 días para este permiso. Para uso continuo revisa el estado de publicación adecuado de la aplicación antes de depender de ella como almacenamiento permanente. Referencia: https://developers.google.com/identity/protocols/oauth2#expiration

El asistente de autorización solicita el permiso de Drive para acceder a estas carpetas existentes. Ese permiso de Google es más amplio que una sola carpeta: revisa la pantalla antes de aceptar. El backend restringe los archivos a las tres carpetas configuradas. Una integración más limitada con drive.file requiere seleccionar las carpetas mediante Google Picker; no basta con pegar sus enlaces.

## Activar las nuevas subidas

En el archivo local .env agrega o actualiza:

```dotenv
ARCHIVOS_PROVEEDOR=drive
DRIVE_CREDENCIALES=/run/privado/google-drive.json
DRIVE_CARPETA_PERFILES=1DJowW2-fEZ5gdKanuj4P9KufWwRQIO7_
DRIVE_CARPETA_CABANAS=13KmUpK4yKJ0NMMjTOCJdyL4SQwcEs8XR
DRIVE_CARPETA_COMPROBANTES=1bS3PGBQ635RVoXGkplo8ZfP_FKNeErFj
ARCHIVOS_MIGRAR=false
```

Después ejecuta:

```powershell
docker compose up -d --build
```

Las nuevas fotos y comprobantes se guardarán en Drive. En MySQL quedan las referencias y los datos del sistema. Si Drive falla, se devuelve un error; no se guarda una copia local de manera silenciosa. Las rutas de imágenes y de descarga PDF no cambian, por lo que no hay que modificar Angular ni Android.

Cuando se ejecuta desde IntelliJ, configura las mismas variables y usa DRIVE_CREDENCIALES con la ruta absoluta del archivo privado en Windows, no la ruta interna del contenedor.

## Migrar archivos anteriores

Con Drive autorizado y las nuevas subidas comprobadas, cambia temporalmente ARCHIVOS_MIGRAR=true en .env y ejecuta:

```powershell
docker compose up -d --build usuario-service catalogo-service pagos-service
docker compose logs --tail=100 usuario-service catalogo-service pagos-service
```

La migración trabaja por lotes, conserva las URLs existentes, descarga la copia de Drive y compara los bytes antes de guardar la nueva referencia. Es reanudable: omite los registros que ya apuntan a Drive. Las fotos antiguas de cabañas dejan de ocupar su columna binaria después de verificar y confirmar la nueva referencia. No elimina las carpetas locales de perfiles ni comprobantes: quedan como respaldo hasta comprobar el funcionamiento real. No borres volúmenes ni ejecutes down -v.

Al terminar devuelve ARCHIVOS_MIGRAR=false y recrea esos servicios. No dejes activada la migración en el arranque habitual.

## Funcionamiento y límites

- usuario-service conserva el alias público de la foto y una referencia interna del archivo.
- catalogo-service conserva el ID de foto y una referencia interna; puede leer los archivos binarios antiguos durante la transición.
- pagos-service conserva el PDF emitido y sus datos originales, sin regenerarlo para cada descarga. Solo se emiten comprobantes de adelantos aprobados.
- almacenamiento-compartido contiene el almacenamiento local y el adaptador HTTP de Drive, compilados dentro de los tres microservicios. No añade otro microservicio.
- Drive usa el espacio de la cuenta de Google y sus límites de API. No es un almacenamiento ilimitado ni una CDN. El backend transporta las imágenes sin guardarlas en disco cuando usa Drive.
- El proveedor por defecto es local para que el sistema no deje de funcionar antes de configurar Google. No confundas tener el código preparado con tener Drive activo.

Documentación oficial: https://developers.google.com/workspace/drive/api/guides/manage-uploads y https://developers.google.com/identity/protocols/oauth2/web-server
