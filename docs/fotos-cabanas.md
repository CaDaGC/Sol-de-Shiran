# Fotos de cabañas

En Gestión y cuenta > Cabañas, selecciona una imagen principal y hasta ocho fotos adicionales desde tu computadora. Puedes cambiar la portada y quitar fotos de la galería antes de guardar la cabaña. Los archivos deben ser JPG o PNG, máximo 2 MB y 16 megapíxeles por archivo.

Las imágenes se guardan en la tabla fotos_cabana de MySQL mediante catálogo. La cabaña conserva las URL en sus campos existentes. Hibernate crea la nueva tabla al iniciar catálogo con ddl-auto=update. Quitar una foto del formulario elimina su asociación cuando guardas la cabaña; no borra el archivo de la tabla de fotos.

## Contrato HTTP

- POST /api/v1/cabanas/fotos: token de ADMIN, multipart/form-data con campo archivo. Respuesta 201 con {"url":"/api/v1/cabanas/fotos/<uuid>"}.
- GET /api/v1/cabanas/fotos/<uuid>: lectura pública, devuelve bytes image/jpeg o image/png; 404 si no existe.
- Carga inválida: 400; archivo que supera el límite del servidor: 413; sin autenticación: 401; rol sin permiso: 403.

Gateway local: http://localhost:18061. El formulario convierte la ruta devuelta a una URL del gateway y la envía al guardar la cabaña.

## Probar

1. Reiniciar Config Server, catálogo y API Gateway. Mantener usuarios y registry activos.
2. Recargar Angular en http://localhost:14200/gestion e ingresar como ADMIN.
3. Seleccionar una foto principal y varias fotos adicionales. Comprobar las vistas previas.
4. Guardar la cabaña y abrir su ficha desde el catálogo; comprobar portada y galería.
5. Recargar la página y comprobar que permanecen las fotos.
6. Intentar cargar un archivo inválido o mayor de 2 MB. Debe mostrarse un error.

Pruebas unitarias del backend: FotoCabanaServiceTest (contenido PNG real, archivo disfrazado, tamaño y rol CLIENTE). La prueba manual con todos los servicios debe realizarse después del reinicio.