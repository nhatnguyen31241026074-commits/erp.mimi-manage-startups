# TechForge ERP

<p>
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.3-6DB33F?logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Firebase-RTDB-FFCA28?logo=firebase&logoColor=black" />
  <img src="https://img.shields.io/badge/UI-Swing%20%2B%20FlatLaf-007396" />
  <img src="https://img.shields.io/badge/status-demo--grade-yellow" />
</p>

Internal ERP for **startup / SMB operations**: a **Java Swing desktop client** + a **Spring Boot REST API**,
backed by **Firebase Realtime Database**. It covers projects, Kanban tasks & assignments, employees,
worklogs, payroll, clients, and finance (invoices/expenses), with role-based access
(ADMIN / MANAGER / EMPLOYEE / FINANCE / CLIENT), **Gemini**-assisted risk analysis, and a **MoMo** payment flow.

> **Note:** this is a functional **demo / work-in-progress** — see **Project maturity** below.

---

## Highlights

| Area | Detail |
|------|--------|
| **Architecture** | Single Maven module: `com.techforge` Spring Boot app + `com.techforge.desktop` Swing UI |
| **Backend** | Spring Boot 3.2.3, Spring Security, WebSocket, Spring Mail, Firebase Admin SDK, springdoc-openapi (Swagger UI), JFreeChart |
| **Desktop** | FlatLaf Swing, OkHttp + Gson client calling `http://localhost:8080/api/v1` |
| **Data** | Firebase RTDB. `docs/DATABASE_SCHEMA.sql` is an **illustrative reference only** — persistence is Firebase, not SQL |
| **Payments** | MoMo outbound payment-URL creation with HMAC signing |
| **AI** | Gemini-assisted risk analysis & staffing suggestions |

## Documentation (read this first)

Substantial product/technical specs live under [`docs/`](docs/):

- [`DOCS_CAU_HINH_VA_KIEN_TRUC.md`](docs/DOCS_CAU_HINH_VA_KIEN_TRUC.md) — configuration & architecture
- [`DOCS_API_VA_LOGIC_NGHIEP_VU.md`](docs/DOCS_API_VA_LOGIC_NGHIEP_VU.md) — APIs & business logic
- [`DATABASE_SCHEMA.sql`](docs/DATABASE_SCHEMA.sql) — schema reference (illustrative)
- [`USE_CASE_SPECIFICATION.md`](docs/USE_CASE_SPECIFICATION.md), [`TEST_CASES.md`](docs/TEST_CASES.md)
- [`plantuml/`](docs/plantuml/) — sequence diagrams (login, payroll/payment, projects, tasks)

---

## Prerequisites

- **JDK 21**, **Maven 3.9+**
- A Firebase service account key (`serviceAccountKey.json`, **not committed**)
- MoMo / SMTP secrets via `application.properties` or env vars (**not committed**)

## Run locally

### 1) API server (Spring Boot) — from the repo root (where `pom.xml` lives)

```bash
mvn spring-boot:run
# API base: http://localhost:8080/api/v1
# Swagger UI (if enabled): http://localhost:8080/swagger-ui.html
```

Or package and run the jar:

```bash
mvn clean package -DskipTests
java -jar target/erp-0.0.1-SNAPSHOT.jar
```

### 2) Desktop client (Swing)

Entry point: `com.techforge.desktop.DesktopLauncher`. Run it from your IDE with the Maven
classpath; it expects the backend to be up and prints the API URL on start.

> This repo uses **one** `pom.xml` at the root — ignore older docs referencing `backend/pom.xml` / `desktop/pom.xml`.

---

## Project maturity

This is a **functional demo / work-in-progress**, not a production deployment. Security hardening,
full payment reconciliation, and observability are on the roadmap. The security model is designed
around **Firebase ID-token authentication** (verified server-side) with hashed credentials and
secrets kept out of the repo.

---

## Testing

```bash
mvn test
```

## License

Add a `LICENSE` file (MIT or proprietary) per your organization.
