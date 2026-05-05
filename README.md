# TechForge ERP

Internal ERP-style system for **startup / SMB operations**: **Java Swing desktop client** + **Spring Boot REST API**, backed by **Firebase Realtime Database**. Includes **MoMo payment-style integration** (HMAC verification, idempotency mindset) and optional **Gemini**-assisted suggestions documented in `/docs`.

---

## Highlights

| Area | Detail |
|------|--------|
| **Architecture** | Single Maven module: `com.techforge` Spring Boot app + `com.techforge.desktop` Swing UI |
| **Backend** | Spring Boot 3.2.x, Spring Security, WebSocket, Firebase Admin SDK, OpenAPI (Swagger UI) |
| **Desktop** | FlatLaf, OkHttp + Gson client calling `http://localhost:8080/api/v1` |
| **Data** | Firebase RTDB (see `docs/DATABASE_SCHEMA.sql` + Vietnamese specs in `docs/`) |
| **Payments** | MoMo IPN path with HMAC, idempotency, audit-oriented notes in existing README sections |

---

## Documentation (read this first)

The repo ships substantial **product/technical specs** under [`docs/`](docs/):

- [`DOCS_CAU_HINH_VA_KIEN_TRUC.md`](docs/DOCS_CAU_HINH_VA_KIEN_TRUC.md) — configuration & architecture  
- [`DOCS_API_VA_LOGIC_NGHIEP_VU.md`](docs/DOCS_API_VA_LOGIC_NGHIEP_VU.md) — APIs & business logic  
- [`DATABASE_SCHEMA.sql`](docs/DATABASE_SCHEMA.sql) — schema reference  
- [`USE_CASE_SPECIFICATION.md`](docs/USE_CASE_SPECIFICATION.md), [`TEST_CASES.md`](docs/TEST_CASES.md) — QA-oriented artifacts  
- [`plantuml/`](docs/plantuml/) — sequence diagrams (login, payroll/payment, projects, tasks)

---

## Prerequisites

- **JDK 21**
- **Maven 3.9+**
- Firebase service account (not committed)
- MoMo / SMTP secrets as per your environment (not committed)

---

## Run locally

### 1) API server (Spring Boot)

From the **repository root** (where `pom.xml` lives):

```bash
mvn spring-boot:run
```

API base URL (default): `http://localhost:8080/api/v1`  
Swagger UI (when enabled): `http://localhost:8080/swagger-ui.html` (path may vary by Springdoc config).

Alternative:

```bash
mvn clean package -DskipTests
java -jar target/erp-0.0.1-SNAPSHOT.jar
```

(JAR name follows `artifactId` + `version` in `pom.xml`.)

### 2) Desktop client (Swing)

The desktop entry point is `com.techforge.desktop.DesktopLauncher`.

**Recommended:** run `DesktopLauncher` from your IDE (IntelliJ / VS Code Java) with **classpath = Maven project**.

The UI expects the backend to be up (`DesktopLauncher` prints the API URL on start).

> **Important:** This repo uses **one** `pom.xml` at the root. Earlier docs referencing separate `backend/pom.xml` / `desktop/pom.xml` paths were inaccurate for this layout.

---

## Security notes (non-negotiable)

- Authenticate with **Firebase ID Token** via `Authorization: Bearer <token>`; verify server-side with Firebase Admin SDK.  
- Do **not** trust `X-Requester-ID` (or similar) without a verified token.  
- Passwords: hashed only; OTP: secure random, hashed at rest, short TTL, single-use.  
- MoMo IPN: verify HMAC, enforce idempotency, persist audit trails.  
- Never commit secrets—use env vars or a secret manager.

---

## Testing & CI

```bash
mvn test
```

Align CI with: unit tests, static analysis, and secret scanning on PRs.

---

## Contributing

- Layering: Controller → Service → persistence adapters / models.  
- Add tests for business rules (payroll, payments, permissions).  
- PRs should reference updated docs under `docs/` when behavior changes.

---

## License

Add a `LICENSE` file (e.g. MIT or proprietary) according to your organization.
