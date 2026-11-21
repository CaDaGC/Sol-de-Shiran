# Configuración local y preparación para Git

Los archivos versionables usan variables para las contraseñas y claves. No contienen valores operativos.

Configurar `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `INTERNAL_KEY` e `INTERNAL_SERVICE_TOKEN`. La firma JWT debe tener al menos 32 bytes. Los servicios deben compartir las claves correspondientes.

Para Mercado Pago, configurar `MP_ACCESS_TOKEN`, `MP_WEBHOOK_SECRET` y las URLs de retorno/notificación que ya usa el módulo. `.env.example` solo documenta los nombres: Spring no carga automáticamente un archivo `.env`.

En esta instalación, los valores locales se conservaron en `config-server/src/main/resources/config/application.properties`, excluido de Git. Config Server carga ese recurso junto a la configuración compartida. Los demás equipos deben crear su propio archivo privado o definir las variables de entorno en los servicios. Reiniciar Config Server antes que los microservicios cuando cambien estos valores.

`uploads/`, imágenes de usuarios, comprobantes emitidos, dependencias instaladas, compilados y certificados privados están excluidos de Git. El logo, la fuente tipográfica y su licencia de `pagos-service/src/main/resources/comprobantes/` sí son recursos de la aplicación y se versionan.

Las pruebas utilizan claves ficticias y bases H2 en memoria; no necesitan las credenciales locales.

Antes de publicar, revisar `git status`, `git diff --stat` y `git diff --cached`. Esta preparación no crea commits ni publica ramas. Las exclusiones actuales no eliminan datos que pudieran existir en commits antiguos.
