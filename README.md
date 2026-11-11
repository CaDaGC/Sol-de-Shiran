# Sol de Shirán

El proyecto está separado por área:

- **[BackEnd](BackEnd/README.md)**: seguridad, Config Server, Discovery Server, Gateway, Maven y Docker de MySQL.
- **FrontEnd/**: espacio para el desarrollo de la interfaz por el equipo de frontend.

Abre **BackEnd/pom.xml** como proyecto Maven en IntelliJ con Java 21.
Entra en **BackEnd/** para ejecutar `docker compose up -d mysql`.
Las instrucciones y ejemplos de Postman están en [BackEnd/README.md](BackEnd/README.md).
La configuración privada se encuentra en BackEnd/.env y no se sube a Git.

## HU19: métricas administrativas

- [Guía funcional y criterios de validación](docs/HU19-metricas.md).
- [Colección Postman de las API de métricas](docs/postman/HU19.postman_collection.json).
- [Inicio local del frontend y acceso al dashboard](FrontEnd/README.md).
