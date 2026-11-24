# Aplicación web de Sol de Shirán

Interfaz Angular 22 para el catálogo de cabañas, reservas, pagos y administración del complejo turístico. Utiliza TypeScript, componentes standalone, formularios Angular, signals, RxJS y Bootstrap 5.3. Incluye configuración de renderizado de servidor.

## Estructura

| Directorio | Contenido |
| --- | --- |
| src/app/pages | Catálogo, detalle, registro, login, gestión, reservas del cliente, panel del personal y métricas. |
| src/app/shared | Componentes reutilizables, pagos, comprobantes, detalle de reserva, notificaciones y directivas de formularios. |
| src/app/services | Acceso HTTP, autenticación y estado compartido. |
| src/app/models | Contratos y modelos de la interfaz. |
| src/app/guards | Restricciones de navegación por sesión y rol. |
| src/app/layouts | Estructura de navegación y menú lateral. |
| src/app/core | Configuración de API y utilidades comunes. |
| public | Recursos estáticos. |

## Funcionalidades

- Catálogo y detalle de cabañas con fotografías y consulta de disponibilidad.
- Selección de ingreso, salida y huéspedes, con validación del intervalo reservado.
- Registro público con datos personales y sugerencias de nombres disponibles.
- Autenticación, perfil, fotografías y cambio de contraseña.
- Reservas del cliente con búsqueda y filtros de estado, fechas y cabaña.
- Panel tabular de reservas para ADMIN y TRABAJADOR, con administración mediante modal.
- Registro presencial de pago restante y check-in; check-out con daños opcionales.
- Adelantos mediante Mercado Pago y consulta del resultado al retornar.
- Vista y descarga de comprobantes PDF del adelanto.
- Reseñas con fotografía, nombre público, estrellas y comentario.
- Gestión administrativa de cuentas, cabañas y fechas bloqueadas.

Las operaciones de escritura solicitan confirmación. Los campos obligatorios se indican con asterisco y muestran su validación al salir del campo o intentar guardar. Las notificaciones de gestión aparecen temporalmente en la esquina superior derecha.

## Desarrollo local

Utiliza Node.js 24, como la construcción Docker, y npm. Desde esta carpeta:

```bash
npm ci
npm start
```

La aplicación se abre en http://localhost:4200. El backend debe estar disponible. La dirección de las API se configura en `src/app/core/api.config.ts` y actualmente apunta a http://localhost:8061/api/v1.

El frontend no contiene secretos de Mercado Pago ni credenciales OAuth de Drive. Las imágenes y los documentos se consultan a través del backend. Para cambiar de almacenamiento no es necesario añadir tokens de Google al navegador.

## Compilación y pruebas

```bash
npm run build
npm test -- --watch=false
```

La compilación genera la aplicación en `dist/FrontEnd`. Las pruebas utilizan Vitest y jsdom. La configuración incluye presupuestos de tamaño; revisa las advertencias de compilación al incorporar recursos o estilos.

## Ejecución integrada

Desde la raíz del repositorio:

```bash
docker compose up -d --build
```

Compose construye el frontend y lo publica en el puerto 4200. No ejecutes npm start al mismo tiempo sobre ese puerto.

## Navegación y permisos

El menú se adapta al rol. CLIENTE dispone de sus reservas, catálogo y cuenta; TRABAJADOR administra estancias y su cuenta; ADMIN gestiona el sistema y las métricas administrativas. Las guardas del navegador complementan la autorización del backend, que determina el acceso real a cada operación.

El rol DUENO existe en el dominio, pero su pantalla web de reportes por propiedad todavía no está completada. No debe confundirse la existencia del rol con una vista ya implementada.
