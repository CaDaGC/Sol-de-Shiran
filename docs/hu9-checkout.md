# HU9 · Registro de check-out

El trabajador abre una reserva en estadía desde el panel. Si recibió un pago por daños, marca la casilla e indica la descripción, el importe en soles y el medio de pago presencial. Al confirmar, la reserva pasa a Finalizada y la cabaña queda Disponible. El cargo queda en el registro de salida, separado de los importes de alojamiento.

## Diagrama de secuencia

```mermaid
sequenceDiagram
    actor T as Trabajador
    participant A as Angular
    participant G as API Gateway
    participant R as ms-reservas
    participant C as Catálogo
    T->>A: Confirma salida con o sin cobro por daños
    A->>G: POST /api/v1/reservas/{id}/check-out
    G->>G: Valida el token de sesión
    G->>R: Reenvía la solicitud autenticada
    R->>C: Verifica acceso operativo a la cabaña
    C-->>R: Permisos de la cabaña
    alt Solicitud válida y reserva ocupada
        R->>R: Registra salida y cargo opcional dentro de la transacción
        R->>C: Libera la ocupación asociada a la reserva
        alt Catálogo confirma
            C-->>R: Liberación confirmada
            R->>R: Confirma la transacción
            R-->>G: 200 y reserva finalizada
            G-->>A: Reserva con registro de salida
            A->>G: Refresca el listado de reservas
            G->>R: GET /api/v1/reservas/panel
            R-->>G: Listado actualizado
            G-->>A: Listado actualizado
            A-->>T: Muestra la salida registrada
        else Catálogo falla
            C-->>R: Error
            R->>R: Revierte el registro
            R-->>G: Error de catálogo
            G-->>A: Error de catálogo
            A-->>T: Permite reintentar con los mismos datos
        end
    else Reintento con los mismos datos
        R-->>G: 200 y registro original
        G-->>A: Registro original
    else Operación inválida o sin permiso
        R-->>G: Error de validación, permiso o estado
        G-->>A: Error
        A-->>T: Muestra el mensaje
    end
```

## Validación

Importa [HU9.postman_collection.json](postman/HU9.postman_collection.json) y ejecuta la colección en orden con el API Gateway, reservas, catálogo y usuarios activos. Prepara dos reservas de prueba en estado OCUPADA, en cabañas distintas asignadas al trabajador del token. Completa los IDs de ambas reservas y cabañas, y los tokens de trabajador y cliente.

En Angular, abre el detalle de una reserva en estadía. Comprueba en pantalla estrecha que puedes marcar y desmarcar la casilla, que los campos aparecen solo cuando hay cobro y que el importe admite dos decimales. Prueba ambas salidas con reservas de prueba distintas. Tras confirmar, comprueba que la lista cambia a Finalizada sin recargar la página y que al reabrir el detalle se ve el registro persistido.
