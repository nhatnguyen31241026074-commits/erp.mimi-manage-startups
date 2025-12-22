System Design — Detailed Architecture

This document gives a more detailed look at components, data models, API contracts, sequences, and deployment considerations for TechForge ERP.

1. Component Diagram (logical)
- Desktop Client (Java Swing)
  - ApiClient (session store)
  - UI modules (Login, Dashboard, Panels, Dialogs)
- Backend (Spring Boot)
  - Controllers
  - Services
  - Firebase integration (Google SDK)
- Persistence: Firebase RTDB (LTUD10 root node)
- External: MoMo payment gateway

2. Data Models (primary)
- User
  - id, username, email, fullName, role, baseSalary, hourlyRateOT, ...
- Project
  - id, name, description, budget, startDate, endDate, status, clientId
- Task
  - id, projectId, assignedUserId, assigneeEmail, title, description, status, priority, estimatedHours
- Payroll
  - id, userId, month, year, baseSalary, overtimePay, totalPay, paid (boolean), transactionId
- Invoice
  - id, projectId, amount, status, createdAt

3. API Contracts (selected)
- POST /api/v1/auth/login
  - Request: {email, password}
  - Response: {userId, role, user}
- GET /api/v1/users
  - Response: [User]
- GET /api/v1/clients
  - Response: [Client] OR {} (firebase map)
- POST /api/v1/projects
  - Request: Project JSON (include clientId)
  - Response: Created project JSON with id
- GET /api/v1/finance/transactions
  - Response: [Payroll] filtered server-side to only paid records

4. Sequences
- Project Creation (UI -> Backend -> Firebase)
  - User opens ProjectDialog
  - ProjectDialog loads clients (/clients -> fallback /users)
  - User selects client and saves
  - ApiClient.post("/projects", payload)
  - Backend ProjectController saves to Firebase and returns created project (id)

- Client login notification sequence
  - Client logs in via /auth/login
  - Desktop sets session and opens dashboard
  - LoginFrame calls /projects and filters by clientId
  - If assigned projects exist, show notification dialog

5. Error handling & fallback patterns
- API returns different JSON structures (arrays vs firebase maps): client parsing logic handles both.
- API 403 handling: Desktop prompts for authentication if lacking permissions; no fake data is inserted.
- Finance panel loads payroll from `/api/v1/finance/transactions` (server filters only PAID payrolls). UI shows empty table when no transactions.

6. Deployment & Dev notes
- `UnifiedLauncher` currently starts backend and UI for convenience in developer mode. In production, start Spring Boot as separate process.
- Recommended production deployment: Spring Boot behind a reverse proxy (Nginx), with Firebase service account and restricted access.

7. Design decisions and trade-offs
- Desktop uses polling (10s refresh) or manual refresh for near-realtime UX; full realtime via Firebase websockets is left as an extension to reduce complexity.
- Using `X-Requester-ID` simplifies session propagation for desktop; it requires secure storage of session tokens in future.
- Using Firebase RTDB enables easy realtime features but imposes JSON-map format differences — client parsing handles both array and map cases.

Appendix A: Suggested Roadmap
- Notification Center backed by Firebase notifications node
- Background sync agent to persist local changes in case of network loss
- OAuth2 / JWT sessions for stronger security instead of X-Requester-ID header

Document owner: TechForge engineering
Last updated: 2025-12-22

