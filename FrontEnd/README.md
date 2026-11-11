# FrontEnd

## Sol de Shirán: entorno local

1. Inicia MySQL y los servicios service-registry, config-server, usuario-service, auth-service, catalogo-service, reservas-service, pagos-service y api-gateway.
2. Desde `FrontEnd`, ejecuta `npm install` y `npm start`. Abre `http://localhost:4200`.
3. Para HU19, inicia sesión con una cuenta ADMIN y abre **Métricas** en el menú o visita `http://localhost:4200/admin/metricas`.
4. El dashboard consulta ingresos, frecuencia de reservas y nombres de cabañas mediante el gateway en `http://localhost:8061/api/v1`. Si una de las tres solicitudes falla, muestra un mensaje de error.

La [guía de HU19](../docs/HU19-metricas.md) explica los filtros y las pruebas. La [colección Postman](../docs/postman/HU19.postman_collection.json) permite verificar las API y los permisos sin cambiar datos.

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.0.3.

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
