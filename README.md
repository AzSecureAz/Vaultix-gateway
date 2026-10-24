# Vaultix API Gateway

The single entry-point reverse proxy and API Gateway for the **Vaultix** microservices platform, built with **Spring Cloud Gateway Server MVC** on **Java 27 Virtual Threads** (Project Loom).

---

## Service Overview

| Attribute | Value |
| :--- | :--- |
| **Port** | `8085` (Main entry point for all frontend & mobile clients) |
| **Framework** | Spring Boot 4.1.1 + Spring Cloud Gateway Server MVC (Spring Cloud 2025.1.3) |
| **Runtime** | Java 27 (Amazon Corretto) with Virtual Threads enabled |
| **Discovery** | Eureka Client (`http://localhost:8761/eureka/`) |

---

## Documentation Index

Detailed routing pipeline, filter architectures, and GeoIP enrichment designs are maintained in the [`docs/`](docs/) directory:

| Document | Description |
| :--- | :--- |
| 📖 [**Architecture & Routing Pipeline**](docs/ARCHITECTURE.md) | In-depth breakdown of the client context enrichment pipeline (`ClientContextFilter`), MaxMind GeoLite2 integration, `ua-parser` device resolution, CORS management, Gzip compression, and dynamic Eureka load balancer routing. |

---

## Request Pipeline Architecture

```
Client Request
      │
      ▼
┌─────────────────────────┐
│       CorsFilter        │  (Ordered.HIGHEST_PRECEDENCE)
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│   ClientContextFilter   │  (Ordered.HIGHEST_PRECEDENCE + 5)
│  ┌───────────────────┐  │
│  │ GeoIpService      │  │  -> MaxMind GeoLite2-City (.mmdb) + Caffeine Cache
│  │ DeviceParser      │  │  -> ua-parser + Sec-CH-UA-Platform + Caffeine Cache
│  └───────────────────┘  │
│  Wraps request & injects:
│   • X-Client-IP
│   • X-Device-Info
│   • X-Device-Location
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ Spring Cloud Gateway MVC│  -> lb://<service> via Eureka Service Discovery
└─────────────────────────┘
```

---

## Configuration Reference

Configurable via `src/main/resources/application.yaml` or environment variables:

| Environment Variable | Default Value | Description |
| :--- | :--- | :--- |
| `SERVER_PORT` | `8085` | Gateway HTTP listen port |
| `EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE` | `http://localhost:8761/eureka/` | Eureka server registry URL |
| `VAULTIX_CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Comma-separated allowed CORS origins |
| `VAULTIX_CORS_ALLOWED_METHODS` | `GET,POST,PUT,DELETE,PATCH,OPTIONS` | Allowed HTTP methods |
| `VAULTIX_CORS_ALLOWED_HEADERS` | `*` | Allowed HTTP request headers |
| `VAULTIX_CORS_ALLOW_CREDENTIALS` | `true` | Allows cookies and Authorization headers |
| `VAULTIX_CORS_MAX_AGE` | `3600` | Pre-flight request cache duration (seconds) |
| `VAULTIX_GEOIP_DATABASE_PATH` | `../infra/geoip/GeoLite2-City.mmdb` | Absolute or relative path to MaxMind DB |

---

## Directory Structure

```
gateway/
├── pom.xml
├── README.md
├── .env.example
├── docs/
│   └── ARCHITECTURE.md
└── src/main/
    ├── java/com/vaultix/gateway/
    │   ├── GatewayApplication.java
    │   ├── config/
    │   │   ├── CorsConfig.java
    │   │   └── properties/
    │   │       ├── CorsProperties.java
    │   │       └── GeoIpProperties.java
    │   ├── filter/
    │   │   ├── ClientContextFilter.java
    │   │   └── ClientContextRequestWrapper.java
    │   └── service/
    │       ├── DeviceParserService.java
    │       └── GeoIpService.java
    └── resources/
        └── application.yaml
```

---

## Quick Start

### 1. Prerequisites
- **Java 27** (Amazon Corretto)
- **Eureka Server** running on `http://localhost:8761`
- **MaxMind GeoLite2 City Database** placed at `infra/geoip/GeoLite2-City.mmdb` (see `infra/README.md` for download steps)

### 2. Build & Run

```powershell
# Build the application
.\mvnw.cmd clean package

# Run the Gateway
.\mvnw.cmd spring-boot:run
```

Once launched, the gateway is available at [http://localhost:8085](http://localhost:8085).
