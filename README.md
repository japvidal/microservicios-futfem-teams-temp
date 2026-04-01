# microservicios-futfem-teams-temp

`microservicios-futfem-teams-temp` is the temporary team service for the Tikitakas backend. It stores staged or provisional team records that should remain separated from the canonical team catalog until they are validated, transformed, or promoted into the main domain.

The application uses Java 21, Spring Boot, Spring Data JPA, MySQL, Springdoc OpenAPI, and Maven Wrapper. It follows the same architecture as the rest of the futfem services by reusing `microservicios-common`, registering in Eureka, and being routed through the API gateway instead of exposing itself directly to browser clients.

Typical local execution:

```bash
./mvnw spring-boot:run
```

Gateway route:

- `/api/futfem/teamstemp/**`

In `v0.1.0`, the repository has been aligned with the current CI/CD and deployment conventions of the platform, including Docker runtime packaging, Jenkins pipelines, and Swagger/OpenAPI server metadata compatible with gateway access.

This microservice is useful for import workflows, synchronization tasks, or temporary editorial processes where team data should be handled independently from the stable team repository. It helps keep the canonical domain clean while still supporting iterative data management processes in the broader Tikitakas ecosystem.
