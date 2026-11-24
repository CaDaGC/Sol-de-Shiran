# Foto de perfil

En Gestión y cuenta > Mi perfil puedes elegir una foto JPG o PNG (máximo 2 MB y 16 megapíxeles), revisar la vista previa y pulsar Guardar foto. También puedes cancelar la selección o quitar la foto guardada. El avatar del menú lateral se actualiza y muestra la inicial cuando no existe foto.

La foto se guarda en MySQL, en las columnas foto_perfil y tipo_foto_perfil de usuarios. Hibernate las crea al reiniciar usuario-service con ddl-auto=update. Los bytes no se incluyen en los JSON de usuario.

## API privada

Todas las rutas requieren JWT y usan exclusivamente el ID de la cuenta autenticada, nunca un ID proporcionado por el formulario.

- PUT /api/v1/usuarios/me/foto: multipart/form-data, campo archivo. Respuesta 204.
- GET /api/v1/usuarios/me/foto: imagen binaria, 404 si no existe foto. Cache-Control: no-store.
- DELETE /api/v1/usuarios/me/foto: elimina la foto de la cuenta actual, respuesta 204.
- Archivo inválido: 400; tamaño excedido: 413; token inválido: 401.

## Verificación manual

Reiniciar Config Server, usuario-service y API Gateway. Recargar Angular. Entrar en Mi perfil, guardar una foto, comprobar avatar, cerrar sesión e ingresar de nuevo. La foto debe volver a cargarse desde el servidor. Quitarla debe restaurar la inicial.

Pruebas unitarias FotoPerfilServiceTest: conservación de imagen real en la cuenta autenticada, eliminación sin alterar el usuario, rechazo de contenido inválido y límite de tamaño. La prueba completa en navegador requiere los servicios reiniciados.