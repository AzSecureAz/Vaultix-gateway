# Vaultix API Gateway

API Gateway for the **Vaultix** microservices platform, built using **Spring Cloud Gateway Server MVC** and **Java 27 Virtual Threads** (Project Loom).

---

## Service Details

- **Group ID**: `com.vaultix`
- **Artifact ID**: `gateway`
- **Version**: `0.0.1`
- **Port**: `8085` (Main client entrance)

---

## Key Features

- **Spring Cloud Gateway Server MVC**: Simple, readable imperative code replacing reactive WebFlux complexity.
- **Java Virtual Threads**: High-throughput non-blocking request routing enabled with `spring.threads.virtual.enabled: true`.
- **Eureka Service Discovery**: Dynamic client routing to downstream microservices registered with Eureka Server (`http://localhost:8761`).
- **Unified API Routing**: Single entrance point for mobile and web clients.

---

## Running Locally

### Build
```powershell
.\mvnw.cmd clean package
```

### Run
```powershell
.\mvnw.cmd spring-boot:run
```
Once started, the gateway listens on [http://localhost:8085](http://localhost:8085).
