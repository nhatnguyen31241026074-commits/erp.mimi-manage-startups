# AGENTS.md — TechForge ERP (erp.mimi-manage-startups)

> **Read this first.** Onboarding brief for any developer or AI agent continuing this repo.

---

## 1. What this project is

A full-stack **ERP for managing a software startup / SMB**. It has two runnable parts in one Maven module:

- **Spring Boot REST API** (`com.techforge`) on `http://localhost:8080/api/v1`, persisting to **Firebase Realtime Database**.
- **Java Swing desktop client** (`com.techforge.desktop`, FlatLaf) that calls the API via OkHttp/Gson.

Domains covered: projects, Kanban tasks & assignments, employees, worklogs, payroll, clients, finance
(invoices/expenses). Extras: role-based access (ADMIN/MANAGER/EMPLOYEE/FINANCE/CLIENT), **Gemini** risk
analysis & staffing suggestions, and a **MoMo** payment flow.

## 2. Current state (works vs. demo-only)

| Area | State |
|------|-------|
| Spring Boot API + Firebase RTDB CRUD | ✅ Works |
| Swing desktop client | ✅ Works (needs backend up) |
| Swagger/OpenAPI docs | ✅ Available |
| Reports (JFreeChart) | ✅ Present |
| MoMo **outbound** payment URL (HMAC signing) | ✅ Works |
| MoMo **inbound** IPN (HMAC verify + idempotency + audit) | ❌ Not implemented |
| Auth / security | ⚠️ Work-in-progress — hardening planned (see §6) |
| Tests | ⚠️ Minimal; specs in `docs/` outpace code |

## 3. Architecture & data flow

```
Swing client (FlatLaf, OkHttp/Gson)
        │  HTTP  http://localhost:8080/api/v1
        ▼
Spring Boot API  ──Controller──►  Service  ──►  Firebase Admin SDK  ──►  Firebase RTDB
        │
        ├─ RoleInterceptor  (role checks — hardening planned, see §6)
        ├─ MomoService      (outbound createPaymentUrl, HMAC)
        └─ Gemini client    (risk analysis / staffing suggestions)
```

`docs/DATABASE_SCHEMA.sql` is a **reference model only** — real storage is Firebase RTDB (JSON trees), not SQL.

## 4. Key files (start here)

- `pom.xml` — single module, Java 21, Spring Boot 3.2.3.
- `src/main/java/com/techforge/` — Spring app: controllers, services, config, Firebase integration.
- `AuthController` — login/register/roles (hardening planned, see §6).
- `RoleInterceptor`, `SecurityConfig` — authorization (hardening planned, see §6).
- `MomoService`, `PaymentController` — payments.
- `com.techforge.desktop.DesktopLauncher` — Swing entry point.
- `docs/` — thorough Vietnamese specs + PlantUML sequence diagrams. **Best place to understand intended behavior.**

## 5. How to run

See `README.md`. Needs JDK 21, Maven, a Firebase `serviceAccountKey.json`, and `application.properties`
with firebase/momo/smtp keys (all uncommitted). `mvn spring-boot:run`, then launch `DesktopLauncher`.

## 6. Areas to harden before production

This is a demo. The following areas are **work-in-progress** and should be completed before any real
deployment (files noted so you know where to look):

1. **Authentication & authorization** — move to hashed credentials and enforce server-side Firebase
   ID-token verification with deny-by-default route protection. Relevant: `AuthController`,
   `SecurityConfig`, `RoleInterceptor`.
2. **Payments** — the MoMo flow currently does outbound payment-URL creation only; the inbound IPN path
   (HMAC verification, idempotency, audit trail) still needs to be built. Relevant: `MomoService`,
   `PaymentController`.
3. **Observability** — replace `System.out`/`printStackTrace` with a structured logger (SLF4J) and return
   generic client-facing error messages via a `@ControllerAdvice` handler.

Repo hygiene: `mvn_err.txt`, `mvn_out.txt`, `payload.json` (0-byte) and `.idea/` are tracked despite being
in `.gitignore`.

## 7. Recommended next steps (roadmap, priority order)

- [ ] Complete authentication/authorization hardening (see §6, item 1).
- [ ] Build the inbound MoMo IPN path and wire real invoices (see §6, item 2).
- [ ] Move to structured logging + generic error responses (see §6, item 3).
- [ ] Hygiene: untrack build artifacts/IDE files (`mvn_*.txt`, `payload.json`, `.idea/`).
- [ ] Consolidate duplicate structure: two `MomoPaymentDialog` classes; config split across `com.techforge.config` vs `com.techforge.erp.config`.
- [ ] Add tests for payroll, permissions, and payment flows; add `LICENSE`.

## 8. Conventions

- Layering: Controller → Service → Firebase adapters. Business rules belong in services.
- Never commit secrets. `application.properties` and `serviceAccountKey.json` stay out of git.
- Update `docs/` when behavior changes — the specs are the source of truth for intended behavior.
