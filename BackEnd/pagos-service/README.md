# Pagos de prueba: Checkout Pro con Orders

La aplicación Mercado Pago configurada utiliza Checkout Pro / Orders. El frontend crea una orden y redirige a `checkout_url`. No requiere Public Key ni capturar la tarjeta en Angular. El antiguo endpoint `/brick` responde 409 para impedir solicitudes incompatibles a Payments.

## Configuración

- `MP_ACCESS_TOKEN`: Access Token de la aplicación y entorno Prueba.
- `MP_WEBHOOK_SECRET`: firma secreta del mismo entorno y aplicación.
- `INTERNAL_SERVICE_TOKEN`: mismo valor en pagos, reservas y catálogo.
- `MP_FRONTEND_URL`: por defecto `http://localhost:4200`.
- `MP_WEBHOOK_URL`: URL informativa del receptor. En Orders, guardar la URL y el evento en el panel de Mercado Pago; cambiar esta propiedad por sí sola NO configura notificaciones en Mercado Pago.

Ngrok debe reenviar al Gateway: `ngrok http 8061`. Consultar `http://127.0.0.1:4040/api/tunnels` para conocer la URL HTTPS activa. En Mercado Pago / Configuración de Webhooks / Prueba, guardar `<URL HTTPS>/api/v1/pagos/webhook`, seleccionar **Order (Mercado Pago)** y usar su firma secreta en pagos-service. Si cambia el dominio ngrok, actualizar también el panel. No usar el puerto 8086: pertenece a catálogo.

## Flujo

1. Crear una reserva temporal e iniciar sesión.
2. Pagar adelanto / Continuar a Mercado Pago.
3. Abrir la tienda en una ventana de incógnito sin sesión iniciada en Mercado Pago. Iniciar sesión solo en la tienda, crear una reserva nueva y abrir el checkout. Para Checkout Pro Orders no se necesita cuenta comprador de prueba: pagar como invitado con una tarjeta de prueba. Si pide correo, usar uno válido; para simular aprobación, usar APRO como titular y el documento indicado en la documentación de Perú.
4. Volver a `/pago/resultado?transaccion=<ID local>`. El servidor consulta la orden; nunca acepta como comprobante el resultado de la URL.
5. El webhook verifica HMAC con el ID en minúsculas, consulta `/v1/orders/{id}` y coteja referencia, moneda e importe. Solo `processed/accredited` con importe pagado completo confirma la reserva.

La reserva debe seguir vigente al confirmar. El estado APROBADA se conserva aunque falle la comunicación con reservas, y un reenvío vuelve a intentar confirmarla. Los pagos pendientes se reutilizan para no crear otra orden al repetir el botón; un intento sin ID de resultado incierto bloquea el reintento automático.

## Validación realizada

- Nueve pruebas aisladas `OrdersFlowTests` pasan: creación, acreditación, importe alterado, orden sin pagar, titularidad, firma alfanumérica, reutilización de checkout, bloqueo de resultado incierto y reenvío de confirmación.
- `npm run build -- --configuration development` pasa.
- `npm run build` detecta un problema previo: CSS de detalle-cabana supera el presupuesto de producción (10.78 kB frente a 8 kB).
- La API de Mercado Pago aceptó las credenciales existentes y creó una orden ORDTST sin pagar de S/1. No se enviaron tarjetas ni se procesaron cobros.
- Ngrok alcanza el backend por el Gateway. Una notificación sin firma responde 401. Una notificación correctamente firmada de la orden de diagnóstico consulta Orders y responde 404 porque esa orden de diagnóstico no está asociada a ninguna reserva local: es el resultado esperado de esa prueba técnica.

Pendiente de validación manual: completar una compra de prueba desde una reserva vigente y comprobar su confirmación. La prueba técnica de conectividad no equivale a un pago completado.

Pruebas backend: `mvn -Dtest=OrdersFlowTests test` desde pagos-service con Java 21. La prueba original `contextLoads` requiere los servicios y las variables del entorno; no forma parte de las nueve pruebas aisladas.


Referencia oficial de pruebas Orders: https://www.mercadopago.com.pe/developers/es/docs/checkout-pro-orders/integration-test/test-purchase-with-card?scope=prod

