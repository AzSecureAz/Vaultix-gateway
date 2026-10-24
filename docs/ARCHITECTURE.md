# Vaultix API Gateway - Architecture & Routing Pipeline

Technical specification of the reverse proxy routing, request decoration, client context extraction, and CORS pipeline in the **Vaultix API Gateway**.

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

## Core Capabilities

### 1. Client Context Enrichment (`ClientContextFilter`)
Intercepts incoming requests and decorates downstream headers using `ClientContextRequestWrapper`:
- **`X-Client-IP`**: Resolves public client IP from `X-Forwarded-For` or `X-Real-IP`, discarding loopback and private RFC1918/RFC4193 addresses when proxy headers exist.
- **`X-Device-Info`**: Parses browser and OS (e.g. `Chrome 122 on Windows`) via `DeviceParserService` with Caffeine caching and fallback to raw user agent.
- **`X-Device-Location`**: Resolves location (e.g. `London, United Kingdom` or `Local / Private Network`) via `GeoIpService` backed by MaxMind `GeoLite2-City.mmdb` and Caffeine caching.

### 2. CORS Management (`CorsConfig`)
Centralized cross-origin request handling configured dynamically through `vaultix.cors` properties, supporting pre-flight caching, wildcard headers, and cookie credentials.

### 3. Dynamic Routing
Imperative declarative routes forwarding requests to backend microservices using Eureka load balancer (`lb://`):
- `/api/v1/auth/**` &rarr; `lb://authorization`

### 4. Gzip Response Compression
Native HTTP response compression enabled for responses exceeding 1KB (`application/json`, `text/plain`, `application/xml`).

### 5. Platform-Wide Snake Case JSON (`SNAKE_CASE`)
Enforces `spring.jackson.property-naming-strategy: SNAKE_CASE` across all gateway responses and error envelopes.
