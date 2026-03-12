# Kineo Training Service

## Descripción
**Kineo Training Service** es un microservicio backend desarrollado con **Spring Boot** para la gestión inteligente de planes de entrenamiento. Utiliza **Google Vertex AI (Gemini)** para generar planes personalizados basándose en las necesidades y limitaciones del usuario.

## Características
*   **Generación de Planes con IA:** Crea planes de entrenamiento completos y estructurados (sesiones, ejercicios, series) adaptados al perfil del usuario.
*   **Gestión de Usuarios:** Registro de usuarios y almacenamiento de evaluaciones físicas (objetivos, lesiones, equipo disponible, etc.).
*   **Catálogo de Ejercicios:** Base de datos de ejercicios con categorización por grupo muscular y equipo.
*   **Seguimiento del Progreso:** Registro detallado de series, repeticiones, peso y RPE.
*   **Persistencia Robusta:** Base de datos relacional PostgreSQL con migraciones gestionadas por Flyway.
*   **Documentación API:** Swagger UI integrado.

## Tecnologías
*   **Java 21**
*   **Spring Boot 3.2.2**
*   **Spring AI (Vertex AI Gemini)**
*   **Spring Data JPA (Hibernate)**
*   **PostgreSQL 16**
*   **Flyway** (Gestión de versiones de BD)
*   **Docker & Docker Compose**
*   **Lombok**
*   **OpenAPI (Swagger)**

## Requisitos Previos
1.  **Java 21 JDK** instalado.
2.  **Docker** y **Docker Compose** instalados.
3.  **Cuenta de Google Cloud** con Vertex AI habilitado y credenciales configuradas (o `gcloud` CLI autenticado localmente).

## Configuración Rápida

### 1. Base de Datos
Levanta la base de datos PostgreSQL usando Docker:
```bash
docker-compose up -d
```

### 2. Configuración de la Aplicación
Edita `src/main/resources/application.properties` para configurar tus credenciales de Google Cloud si es necesario:
```properties
spring.ai.vertex.ai.gemini.project-id=TU_PROJECT_ID
spring.ai.vertex.ai.gemini.location=us-central1
```
*Si ejecutas localmente y ya has hecho `gcloud auth application-default login`, Spring AI detectará las credenciales automáticamente.*

### 3. Ejecutar la Aplicación
```bash
./mvnw spring-boot:run
```
La aplicación iniciará en el puerto `8080`.

## Documentación API (Swagger)
Una vez iniciada la aplicación, accede a la documentación interactiva en:
👉 **[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)**

Ahí podrás ver y probar todos los endpoints disponibles, como:
*   `POST /api/users/register`: Registrar usuario y generar plan con IA.
*   `GET /api/training/history`: Ver historial de consejos.

## Estructura del Proyecto
*   `src/main/java/com/app/kineo`: Código fuente.
    *   `controller`: Endpoints REST.
    *   `service`: Lógica de negocio e integración con IA.
    *   `repository`: Acceso a datos (JPA).
    *   `model`: Entidades de base de datos.
    *   `dto`: Objetos de transferencia de datos.
*   `src/main/resources/db/migration`: Scripts SQL de Flyway para la base de datos.

## Contribución
1.  Haz un Fork del repositorio.
2.  Crea una rama para tu feature (`git checkout -b feature/nueva-funcionalidad`).
3.  Haz Commit de tus cambios (`git commit -m 'Añadir nueva funcionalidad'`).
4.  Haz Push a la rama (`git push origin feature/nueva-funcionalidad`).
5.  Abre un Pull Request.
