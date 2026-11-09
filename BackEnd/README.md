# Backend local

## Configuracion de pagos

1. Copia `.env.example` a `.env` y completa las credenciales de prueba de
   Mercado Pago. No subas `.env` al repositorio.
2. Desde `BackEnd`, carga las variables en PowerShell antes de iniciar cada
   proceso Java. Spring Boot no carga `.env` automaticamente:

   ```powershell
   Get-Content .env | ForEach-Object {
     if ($_ -match '^\s*([^#=]+)=(.*)$') {
       Set-Item -Path "Env:$($matches[1].Trim())" -Value $matches[2]
     }
   }
   ```

   Ejecuta este bloque en cada terminal que uses para levantar un servicio.
3. Para recibir notificaciones de prueba, configura en el panel de Mercado Pago una URL HTTPS pública que reenvíe `/api/v1/pagos/webhook` al API Gateway. En Windows, ejecuta `ngrok http 8061` y selecciona **Order (Mercado Pago)** en Webhooks. Usa la firma del entorno Prueba en `MP_WEBHOOK_SECRET`.
4. Levanta MySQL y ejecuta en IntelliJ Config Server, Eureka, usuarios, auth, catálogo, reservas, pagos y el API Gateway. El frontend
   envia las solicitudes al gateway en `http://localhost:8061`.

El gateway enruta `/api/v1/pagos/**` a `MS-PAGOS` (puerto local 8083). El webhook
tambien debe llegar a ese servicio, no al catalogo que utiliza el puerto 8086.

## Pruebas

Desde cada modulo, ejecuta `.\mvnw.cmd test`. Las pruebas de pagos y reservas
usan una base H2 aislada y no requieren credenciales reales ni servicios
externos.
