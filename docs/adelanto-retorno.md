# Retorno del adelanto de Mercado Pago

## Comportamiento
Antes de salir al checkout se conserva, en la sesión de esa pestaña, la operación, reserva, cabaña, fechas, huéspedes, imagen y URL actual. Al pulsar Volver a la tienda, la ruta existente `/pago/resultado` devuelve al detalle original y reabre el modal. Se consulta el pago en el servidor: solo APROBADA muestra la marca animada y confirma la reserva. PENDIENTE consulta hasta seis veces y permite actualizar manualmente; los errores y rechazos no se presentan como pagos aprobados.

La animación respeta la preferencia de movimiento reducido. El importe confirmado procede de la respuesta del servidor. Una operación de otra reserva no puede activar la confirmación.

Si se vuelve desde otra pestaña, se perdió el almacenamiento o el pago se inició antes de estos cambios, se muestra una página de resultado con el mismo diseño y verificación. El contexto guardado caduca a las 24 horas y solo admite URLs locales del detalle de cabaña.

## Conflictos al crear el checkout
Se observó un intento local rechazado inmediatamente antes de otro aprobado, pero la base no conserva la respuesta que causó ese rechazo; no se atribuye una causa única al incidente anterior.

Los 409 transitorios del proveedor se reintentan una vez con la misma clave y cuerpo. La solicitud original queda guardada en `transaccion_pago.solicitud_checkout` antes de llamar al proveedor. Si persiste el conflicto o la respuesta queda incierta, el siguiente intento recupera esa misma solicitud y transacción. No se convierte un conflicto incierto en un rechazo definitivo ni se crea otro pago para recuperarlo. Los intentos antiguos sin cuerpo recuperable mantienen el bloqueo de seguridad existente.

## Validación y aplicación
- Angular: compilación de producción correcta; 23 pruebas aprobadas, incluidas cinco nuevas de retorno y confirmación.
- Pagos: diez pruebas de órdenes aprobadas; se simularon el 409 transitorio y persistente, la recuperación del cuerpo original, la reutilización del checkout y la verificación de importes y propietario.
- Las pruebas usan un proveedor simulado y no ejecutan cobros reales. No se modificaron pagos ni reservas existentes.
- Reiniciar pagos-service para incorporar el código y la columna nueva mediante el `ddl-auto: update` existente; recargar el frontend. Probar el retorno con un checkout nuevo en la misma pestaña.
- No se modificó el flujo HU7 de pago restante y check-in. El fallo histórico de la prueba de webhook order/payment, ya documentado, permanece fuera de este cambio.

Referencia de la integración: https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-orders/create-order
