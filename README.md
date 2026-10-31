# Sol de Shirán - Sistema de Gestión y Reservas (PMS)

Sistema de gestión, reservas y control para el centro campestre **Sol de Shirán**, desarrollado bajo una arquitectura de microservicios y aplicando metodologías ágiles (Scrum).

##  Tecnologías y Stack Tecnológico

* **Backend:** Java, Spring Boot, Spring Cloud (Eureka, API Gateway), MySQL.
* **Frontend:** Angular, TypeScript, Tailwind CSS / Bootstrap.
* **Arquitectura:** Microservicios orientados a dominios (`ms-catalogo`, `ms-reservas`, `ms-pagos`).
* **Despliegue & Contenedores:** Docker, Docker Compose.

---

## Estructura del Repositorio

El proyecto se encuentra organizado en un **Monorepo** para facilitar la integración y el despliegue mediante contenedores:

```text
ProjectSolDeShiran/
├── BackEnd/        # Contiene los microservicios y componentes de Spring Cloud
└── FrontEnd/       # Aplicación web desarrollada en Angular