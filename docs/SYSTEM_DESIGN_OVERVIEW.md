System Design — Overview

This document describes the high-level system design for TechForge ERP (desktop frontend + Spring Boot backend + Firebase RTDB). It explains components, responsibilities, data flows, integration points, and operational considerations. Use this as a primary reference for architects and senior engineers.

1. Goals
- Provide an extensible, maintainable desktop client (Java Swing) integrated with a Spring Boot backend and Firebase Realtime Database.
- Support role-based access (Admin, Manager, Employee, Client, Finance).
- Ensure real-time-ish data for key modules (projects, tasks, payroll) using periodic refreshes or Firebase listeners where appropriate.
- Keep the API surface small, predictable and secure (X-Requester-ID header for desktop session).

2. High-level Components
- Desktop Client (Java Swing)
  - `UnifiedLauncher` starts backend and GUI.
  - `ApiClient` (OkHttp) handles HTTP calls to backend, maintains session (`X-Requester-ID`).
  - UI modules: `LoginFrame`, `MainDashboardFrame`, `ProjectDialog`, `FinancePanel`, etc.
- Backend API (Spring Boot)
  - Controllers: `AuthController`, `UserController`, `ProjectController`, `TaskController`, `FinanceController`, `ReportController`.
  - Services: `FinanceService`, `UserService`, `WorkLogService`, etc.
  - RoleInterceptor enforces RBAC on incoming requests.
- Persistence
  - Firebase Realtime Database (primary datastore for domain objects: users, projects, tasks, payrolls, invoices, expenses).
- Third-party integrations
  - MoMo payment provider (payment URLs / callbacks)
  - (Optional) Firebase events / websockets for realtime push

3. Interaction Flow Examples
- User Login
  - Desktop UI posts credentials to `/api/v1/auth/login`.
  - Backend authenticates against Firebase and returns `userId` and `role`.
  - Desktop stores session in `ApiClient` and sets `X-Requester-ID` for subsequent calls.
  - Post-login notifications: employees get pending task count; clients get assigned-projects notification.

- Project Creation (Manager/Admin)
  - `ProjectDialog` loads clients by calling `/api/v1/clients`; if empty, it falls back to `/api/v1/users` and filters `role==CLIENT`.
  - When saving, `ProjectDialog` includes `clientId` in project payload and posts to `/api/v1/projects`.

- Payroll / Transaction History
  - `FinanceService.getTransactionHistory()` reads all payroll entries from Firebase and returns ONLY records where `paid == true`, sorted newest-first.
  - `FinancePanel` calls `/api/v1/finance/transactions` and renders returned payroll records.

4. Security and Session Model
- Desktop uses a session header `X-Requester-ID` set by `ApiClient` after login. Backend `RoleInterceptor` expects this header and fetches user from Firebase to evaluate role.
- APIs return 403 if session missing or lacking role. Desktop surfaces these errors and prompts for login or shows an appropriate message.

5. Observability and Debugging
- `ApiClient` logs request URLs, response codes and response bodies (truncated) to the desktop console to facilitate debugging.
- Desktop UI shows debug messages during data loads (configurable) to help ops diagnose parsing/format problems.

6. Operational Notes
- Backend should be started before UI; `UnifiedLauncher` waits briefly (3s) then launches frontend.
- For production deploy, the backend must run independently and the desktop launcher should not start Spring Boot internally (UnifiedLauncher behavior is dev convenience only).

7. Extension Suggestions
- Add a Notification Center (in-app) backed by a `notifications` node in Firebase.
- Add real push by subscribing the desktop client to Firebase websocket updates.
- Add pagination & filtering on heavy endpoints (projects, users) to improve performance.


Document owner: TechForge engineering
Last updated: 2025-12-22


