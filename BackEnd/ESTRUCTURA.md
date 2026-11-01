# Estructura de microservicios

Referencia: Proyecto-microservicio-1. Paquete base: pe.upeu.edu.

```text
Sol-de-Shiran/BackEnd/
  config-server/
    src/main/java/pe/upeu/edu/configserver/
    src/main/resources/
      application.yaml
      config/
        discovery-server.yaml
        security-microservice.yaml
        api-gateway.yaml
  discovery-server/
    src/main/java/pe/upeu/edu/discovery/
  api-gateway/
    src/main/java/pe/upeu/edu/gateway/
  microservices/
    pom.xml
    security-microservice/
      src/main/java/pe/upeu/edu/seguridad/
        bootstrap/
        config/
        controller/
        dto/request/
        dto/response/
        entity/
        enums/
        exception/
        mapper/
        repository/
        security/
        service/
        util/
      src/main/resources/
        application.yaml
        db/migration/V1__seguridad.sql
      src/test/java/pe/upeu/edu/seguridad/
      pom.xml
  .run/                 Cuatro ejecuciones Maven para IntelliJ
  compose.yaml          Solo MySQL, volumen persistente
  .env.example          Plantilla de secretos locales
  pom.xml               Padre Maven: Java 21, Spring Boot y Spring Cloud
```

Config Server centraliza la configuración sin secretos; Discovery Server registra
las instancias con Eureka. Gateway enruta /api/** a security-microservice mediante
el registro Eureka. Seguridad conserva registro, roles, sesiones y recuperación.
El acceso a MySQL usa JDBC, con entidades, repositorios, servicios, controladores
y DTO separados. No hay frontend ni microservicios de negocio en esta entrega.

Abre BackEnd/pom.xml como proyecto independiente en IntelliJ para cargar las configuraciones de BackEnd/.run.
