# Alcance inicial de seguridad

Fuentes: Project Charter Ágil - Sol de Shirán (Actualizado con Entrevista) y
Historias de usuario .xlsx. Se leyeron sin modificar los documentos.

## Correspondencia

- Excel, Proyecto Descrip. General E63:E66: autenticación, registro y recuperación.
- Excel, Sprints E31: correo y contraseña; E122:E123: registro y recuperación.
- Excel, DoD y DoR D10:D14: interfaz adaptativa, mensajes y redirección según rol.
- Charter, Alcance: administrador, recepción, limpieza, restaurante y clientes.
- Charter, Stakeholders y Excel Bitácora Q7: propietarios limitados a sus unidades.

La numeración de registro y recuperación cambia entre hojas (HU22/HU23 y HU19/HU20).
Se usa la descripción como referencia. El acceso utiliza correo, como indica Sprints.
La meta de autenticación menor de tres segundos necesita medición con MySQL y carga
reales; no se declara cumplida a partir de pruebas con dobles.

## Roles

| Rol | Seguridad disponible | Funciones futuras |
|---|---|---|
| ADMIN_GENERAL | Crear usuarios, asignar roles y activar/desactivar accesos | Administración del negocio |
| PROPIETARIO | Perfil y contexto de identidad propios | Asignar cabañas y limitar sus datos |
| CLIENTE | Registro público, sesión y perfil propios | Reservas y servicios |
| EMPLEADO | Cuenta interna y perfil propio | Funciones de recepción, limpieza o restaurante |

El registro público fija CLIENTE. Clientes y propietarios no administran usuarios.
El administrador no puede cambiar su propio rol ni desactivarse mediante la API.
La API aplica los permisos aunque alguien escriba una URL directamente.
El contexto de propietario devuelve su identidad; la autorización de negocio sobre
cabañas se implementará con esos módulos y no se declara terminada en esta entrega.

## Controles

- BCrypt coste 12; mínimo 12 caracteres y máximo 72 bytes UTF-8.
- Correo normalizado y único en la base de datos.
- Cinco fallos bloquean una cuenta por 15 minutos.
- Límite adicional de 30 solicitudes de autenticación por IP/minuto por instancia.
- Sesiones Bearer con tokens opacos de 256 bits y vencimiento de ocho horas.
- Solo se guarda el hash SHA-256 de los tokens.
- Logout revoca la sesión; cambios de contraseña, rol o estado revocan todos sus accesos.
- Recuperación por correo con código aleatorio de un solo uso y 15 minutos de validez.
- Respuesta genérica de recuperación; el token no se devuelve por la API.
- Auditoría sin contraseñas ni tokens en texto claro.
- Sin autenticación por cookies; CORS restringido a un origen concreto.

Las cuentas persisten en MySQL. El frontend será desarrollado por otra persona.

## Endpoints

- Públicos: GET /api/salud; POST /api/auth/login, /registro, /recuperacion, /restablecer.
- Sesión válida: GET /api/auth/me; POST /api/auth/logout y /api/auth/password.
- Administrador: GET/POST /api/admin/usuarios; PATCH /api/admin/usuarios/{id}/acceso.
- Propietario o administrador: GET /api/propietario/contexto (identidad propia).

## Verificación

36 pruebas de servicio, correo y autorización HTTP aprobadas. Backend compilado.
Arranque real de seguridad y migración Flyway comprobados contra MySQL 8.4.
El envío SMTP real queda pendiente de configurar Gmail.
La entrega incluye Config Server, Discovery Server, Gateway y el microservicio de seguridad; solo MySQL usa Docker. Los módulos de negocio se incorporarán después.



