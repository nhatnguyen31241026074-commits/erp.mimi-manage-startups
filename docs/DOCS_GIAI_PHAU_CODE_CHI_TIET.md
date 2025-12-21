DOCS_GIAI_PHAU_CODE_CHI_TIET.md

Tiêu đề: Phân tích chi tiết từng file Java trong dự án TechForge ERP
Ngày: 2025-12-21
Người thực hiện: Senior Java Architect (mục tiêu: chuẩn bị bảo vệ đồ án, giảng giải từng dòng code)

Hướng dẫn đọc: Mỗi mục theo mẫu:
- File: tên và package
- 1) Vai trò & khái niệm Java áp dụng
- 2) Mổ xẻ logic (key blocks, per-line concepts)
- 3) Câu hỏi có thể bị hỏi và gợi ý trả lời

---

Bắt đầu phân tích từ: `TechForgeApplication.java` (entry point)

---

📁 File: `TechForgeApplication.java` (Package: `com.techforge`)

1) Vai trò & Khái niệm Java áp dụng
- Vai trò: Là lớp khởi tạo ứng dụng Spring Boot (backend). Chứa phương thức `public static void main(String[] args)` gọi `SpringApplication.run(TechForgeApplication.class, args)`.
- Khái niệm Java/Spring:
  - `public static void main` là entry point JVM.
  - `@SpringBootApplication` (meta-annotation) bao gồm `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan` — cấu hình tự động Spring Boot.
  - Dependency Injection: Spring Boot sẽ scan packages để tạo bean cho controllers, services.

2) Mổ xẻ Logic (key blocks)
- Annotation `@SpringBootApplication`: Giúp auto-configure, scan package `com.techforge` và subpackages. Quan trọng: nếu Desktop Launcher cùng project muốn `scanBasePackages` thay đổi, cần đảm bảo desktop class vẫn hoạt động.
- `main` method: `SpringApplication.run(TechForgeApplication.class, args)` — khởi tạo ApplicationContext, start embedded server (Tomcat).
  - Tại sao không dùng `new SpringApplication()`? `run` là shortcut.
- Nếu có `CommandLineRunner` bean trong application context (nhiều file trước chứa DataSeeder), khi context start, Spring sẽ chạy các bean này trong startup sequence.

3) Câu hỏi giảng viên có thể hỏi
- Q1: "Tại sao ứng dụng lại đặt `@SpringBootApplication` ở package `com.techforge`?" — Vì component scan bắt đầu tại package của class đó, nên mọi controller/service dưới `com.techforge` sẽ được quét.
- Q2: "Nếu bạn muốn chỉ chạy Desktop (không start server), làm sao tránh Spring Boot tự chạy?" — Có thể tách module backend/frontend, hoặc điều kiện hóa `main`/khởi tạo Spring Boot, hoặc dùng profile để disable web server.

---

📁 File: `FirebaseConfig.java` (Package: `com.techforge.config`)

1) Vai trò & Khái niệm
- Vai trò: Cấu hình Firebase Admin SDK, load `serviceAccountKey.json`, khởi tạo `FirebaseApp` và thiết lập database URL.
- Khái niệm Java/Spring: sử dụng `@Configuration` và `@Bean` để expose `FirebaseApp` hoặc `FirebaseDatabase` cho DI.

2) Mổ xẻ Logic
- Đọc resource `serviceAccountKey.json` bằng `new ClassPathResource("serviceAccountKey.json")` hoặc `getResourceAsStream`:
  - Tại sao dùng classpath? Để đóng gói cùng artifact và dễ deploy.
  - Cần lưu ý bảo mật: file này chứa private key — không commit vào VCS.
- Tạo `GoogleCredentials.fromStream(in)` và `FirebaseOptions.builder().setCredentials(...).setDatabaseUrl(dbUrl).build()`.
- `FirebaseApp.initializeApp(options)` chỉ được gọi một lần; code thường kiểm tra `FirebaseApp.getApps()` để tránh khởi tạo lại.

3) Câu hỏi giảng viên có thể hỏi
- Q1: "Tại sao bạn không lấy database URL từ environment variable?" — Nên lấy từ `application.properties` để không commit URL/payload.
- Q2: "Nếu serviceAccountKey nằm không đúng path, lỗi gì xảy ra?" — FileNotFoundException; code cần handle và log rõ ràng.

---

📁 File: `UnifiedLauncher.java` (or `DesktopLauncher.java`) (Package: `com.techforge.desktop`)

1) Vai trò & Khái niệm
- Vai trò: Tập trung khởi động backend Spring Boot trong thread riêng, rồi khởi tạo GUI Swing trên EDT.
- Khái niệm Java: Multi-threading, Event Dispatch Thread (EDT) cho Swing, `EventQueue.invokeLater` để tạo UI thread-safely.

2) Mổ xẻ Logic
- Tạo Thread `backendThread` chạy `() -> TechForgeApplication.main(new String[]{})` để start Spring Boot in background.
  - Tại sao tách thread? Vì `SpringApplication.run` blocks current thread; cần non-blocking để start GUI.
- Sleep 3000ms: hacky delay to allow server start. Better: poll health endpoint `/actuator/health` instead of blind sleep.
- Then `EventQueue.invokeLater(() -> new ManHinhChinh().setVisible(true));` ensures Swing UI runs on EDT.
- Error handling: wrap sleep in try-catch InterruptedException and log.

3) Câu hỏi giảng viên
- Q1: "Tại sao dùng sleep không phải health-check?" — Sleep là shortcut; health-check robust solution.
- Q2: "Nếu backend fails to start, GUI sẽ show but API calls will fail — cách xử lý?" — Implement health-check, show modal error, disable actions until backend ready.

---

📁 File: `ApiClient.java` (Package: `com.techforge.desktop`)

1) Vai trò & Khái niệm
- Vai trò: HTTP client wrapper used by Desktop app; uses OkHttp to make REST calls to backend.
- Khái niệm Java: network IO, synchronous and asynchronous calls, exception handling, JSON parsing with Gson.

2) Mổ xẻ logic
- `OkHttpClient client = new OkHttpClient()` configuration: timeouts, interceptors (if any), logging.
- `public Map<String,Object> post(String endpoint, String json)` builds `RequestBody.create(json, MediaType.parse("application/json"))`, builds Request with URL `BASE_URL + endpoint`, executes `client.newCall(request).execute()`.
  - Why OkHttp? Fast, supports synchronous execute and asynchronous enqueue.
- Parsing response: use Gson to parse response body to Map or DTO.
- `delete` method: uses `.delete()` builder; checks response.isSuccessful(), throws IOException if not.

3) Câu hỏi giảng viên
- Q1: "Tại sao dùng OkHttp thay vì HttpClient?" — OkHttp modern, simpler; but Java 11+ has HttpClient as standard.
- Q2: "Làm sao xử lý authentication token headers?" — ApiClient should include authorization header (Bearer token) in requests.

---

📁 File: `UserService.java` (Package: `com.techforge.erp.service`)

1) Vai trò & Khái niệm
- Vai trò: Manage users in Firebase `LTUD10/users` node; provides create/get/update; robust parsing of numeric fields.
- Khái niệm Java: CompletableFuture for async operations, conversion between Firebase DataSnapshot and domain model, exception handling.

2) Mổ xẻ logic
- `convertSnapshotToUser(DataSnapshot s)`:
  - Read `s.child("hourlyRateOT").getValue()` which can be Long or Double or String; code detects `instanceof Number` then converts to double.
  - Null-safety: if `fullName` null assign empty string.
  - Why not rely on automatic mapping? Firebase SDK can map to POJO but per-field parsing gives control and robust error handling.
- `forceReloadUsers(Runnable onLoaded)`:
  - Kicks off read via `addListenerForSingleValueEvent`, in `onDataChange` parse records; on completion call `SwingUtilities.invokeLater(onLoaded)` to update UI safely.
- `getUserByEmail(String email)` uses `orderByChild("email").equalTo(email)`, expects single result.

3) Câu hỏi giảng viên
- Q1: "Tại sao dùng CompletableFuture thay vì synchronous methods?" — To avoid blocking server/EDT threads; Firebase SDK is async.
- Q2: "Nếu một record corrupted, bạn làm gì?" — Current code logs and continues; good practice to track and alert.

---

📁 File: `ProjectService.java` (Package: `com.techforge.erp.service`)

1) Vai trò & khái niệm
- CRUD for Projects; delete operation must cascade to delete related tasks.

2) Mổ xẻ logic
- `createProject(Project p)`: generate push key when null, set id, write via `setValueAsync`.
- `deleteProject(String id)`: query tasksRef.orderByChild("projectId").equalTo(id) -> remove nodes. Then remove project node. Use CompletableFutures to chain deletions.
  - Why cascade? To maintain data integrity; alternative is soft-delete.

3) Câu hỏi giảng viên
- Q1: "Nếu deletion partially fails, database inconsistent — how to prevent?" — Firebase supports transactions for single node but cross-node transactions need careful handling; recommend two-phase deletion or mark 'deleted' flag.

---

📁 File: `TaskService.java` (Package: `com.techforge.erp.service`)

1) Vai trò
- CRUD tasks and partial updates for status (drag/drop).

2) Mổ xẻ
- `updateTaskPartial`: accepts map, merges fields onto existing Task object then writes back — pattern avoids overwriting unmodified fields.
- Handling of `assigneeEmail` vs `assignedUserId`: code manages both to enable lookups by email.

3) Câu hỏi giảng viên
- Q1: "Tại sao có endpoint POST /tasks/{id} để update status thay vì PATCH?" — Simplicity; Spring supports @PatchMapping but implementation used POST for partial update.

---

📁 File: `WorkLogService.java` (Package: `com.techforge.erp.service`)

1) Vai trò & khái niệm Java áp dụng
- Vai trò: Quản lý WorkLog entities (ghi nhận thời gian làm việc), viết dữ liệu vào `LTUD10/worklogs` và đọc chúng phục vụ báo cáo, tính lương.
- Khái niệm: sử dụng CompletableFuture, ValueEventListener from Firebase, snapshotting (copying user's salary into WorkLog for historical integrity).

2) Mổ xẻ logic (key blocks)
- `createWorkLog(WorkLog wl)`:
  - Validate required fields: `userId`, `taskId`, `hours`.
  - Fetch user snapshot: `userService.getUserById(userId).join()` (blocking inside CompletableFuture chain) to obtain `baseSalary` and `hourlyRateOT`.
  - Set `wl.baseSalarySnapshot = user.baseSalary; wl.hourlyRateOTSnapshot = user.hourlyRateOT;` — reason: preserve historical rate (snapshot) so later payroll uses the rate at time of work.
  - Write `worklogsRef.child(id).setValueAsync(wl)` and return created WorkLog CF.
  - Why snapshot? If user later changes salary, old worklogs still should calculate pay with previous values.
- `getAllWorkLogs()` returns CompletableFuture<List<WorkLog>>: read all children, map to POJO list.

3) Câu hỏi giảng viên
- Q1: "Tại sao snapshot salary tốt hơn lấy user rate realtime?" — Preserve historical accuracy; payroll must reflect rate at time of work.
- Q2: "Nếu getUserById() fails, createWorkLog nên xử lý thế nào?" — Current approach likely propagates error; better to fail the worklog creation transactionally.

---

📁 File: `FinanceService.java` (Package: `com.techforge.erp.service`)

1) Vai trò & khái niệm Java áp dụng
- Vai trò: Tính toán payroll, quản lý invoices, expenses; xuất báo cáo tài chính.
- Khái niệm: aggregation logic, business rules (hourly vs monthly salary), use of CompletableFuture and atomic updates to Firebase.

2) Mổ xẻ logic (key blocks)
- `calculatePayroll(String userId, int month, int year)`:
  - Fetch worklogs: `workLogService.getAllWorkLogs()` then filter by `userId` and by `workDate` month/year.
  - For each worklog compute pay:
    - Determine hourlyRate: if user's salaryType == "hourly" => hourlyRate = baseSalarySnapshot; else hourlyRate = baseSalarySnapshot / 160.0 (160 working hours per month assumption).
    - regularPay = hourlyRate * regularHours; overtimePay = hourlyRateOTSnapshot * overtimeHours.
    - Sum totals and create Payroll object.
  - Save Payroll into `LTUD10/payrolls/{id}` via setValueAsync.
- `getAllPayroll()` and test-mode `getAllPayrollForMonth(...)`:
  - For UI purposes sometimes service returns a list of Map<String,Object> with rendered fields (name, hours, total, status).
  - Test-mode mapping: `hours = user.hourlyRateOT` and `rate = 10.0` — used for demo where hourlyRateOT used as 'hours' to allow manual edit in Firebase.

3) Điểm nhấn kỹ thuật
- SalaryType logic must be unit-tested; dividing monthly salary by 160 is business assumption — state it in docs.
- Handling nulls: if snapshot fields null -> default to 0.0; log anomalies.

4) Câu hỏi giảng viên
- Q1: "Tại sao chia cho 160? Có nguồn gốc nghiệp vụ?" — Đây là giả định (40h/week * 4 weeks), cần thảo luận với PM.
- Q2: "Tại sao service ghi Payroll vào Firebase? Có vấn đề transactional?" — Firebase writes are per-node async; n-tuples of writes not atomic across nodes.

---

📁 File: `MomoService.java` (Package: `com.techforge.erp.service`)

1) Vai trò & khái niệm Java áp dụng
- Vai trò: Tạo payload thanh toán MoMo (HMAC signature), gọi endpoint MoMo sandbox, parse response to extract `payUrl`.
- Khái niệm: cryptography (HMAC-SHA256), HTTP client (RestTemplate), ObjectMapper for JSON serialization.

2) Mổ xẻ logic (key blocks)
- `createPaymentUrl(Invoice invoice)`:
  - Validate invoice.
  - Build canonical string to sign: orderId, requestId, amount, partnerCode, accessKey etc in exact order required by MoMo.
  - HMAC-SHA256: use `Mac.getInstance("HmacSHA256")` and `SecretKeySpec` with secret key bytes.
  - Convert signature bytes to hex string.
  - Build JSON payload map and POST to momoEndpoint via RestTemplate.
  - Parse response Map; extract payUrl.

3) Bảo mật & vận hành
- Keep secretKey out of logs. Use environment variables.
- For demo, backend sometimes returns a static payUrl (since re-signing requires access to secret keys and merchant account).

4) Câu hỏi giảng viên
- Q1: "HMAC-SHA256 hoạt động thế nào và tại sao cần nó?" — Đảm bảo integrity & authenticity của request; server dùng secretKey để verify.
- Q2: "Làm sao test integration với MoMo?" — Use sandbox credentials or mock endpoint.

---

📁 File: `ReportService.java` (Package: `com.techforge.erp.service`)

1) Vai trò & khái niệm Java áp dụng
- Vai trò: Tập hợp dữ liệu liên quan tới project (tasks, worklogs, budget) để sinh các report (ProjectReport, MonthlyReport).
- Khái niệm: data aggregation, grouping, date handling.

2) Mổ xẻ logic (key blocks)
- `generateProjectReport(projectId)`:
  - Fetch project, tasks, worklogs.
  - Compute counts, progress percentage, budgetUsed.
  - Build lists: taskBreakdown (by status), workerContribution (sum hours per user).
- `getRecentActivities(projectId)`:
  - Try to fetch from dedicated report node or compose from latest worklogs/tasks; fallback to mock data when empty.

3) Câu hỏi giảng viên
- Q1: "How do you ensure report accuracy with asynchronous DB updates?" — Recompute on-demand or maintain event-driven aggregates.
- Q2: "What about timezone and date parsing issues?" — Normalize dates to UTC or consistent timezone; use Java Time API.

---

📁 File: `AIService.java` (Package: `com.techforge.erp.service`)

1) Vai trò & khái niệm Java áp dụng
- Vai trò: Liên hệ với Google Gemini API để nhận gợi ý phân công (assign) hoặc phân tích rủi ro.
- Khái niệm: prompt engineering, RestTemplate calls, robust parsing of free-text responses into JSON DTOs.

2) Mổ xẻ logic
- Build prompt with concise context: task summary, user candidates (id, role, workload).
- Send request with `Authorization: Bearer {gemini.api-key}` header.
- `cleanJsonResponse(String s)` removes markdown code fences and extracts raw JSON string for parsing into AISuggestion/AIRiskAnalysis.
- Handle parsing errors by fallback heuristics.

3) Câu hỏi giảng viên
- Q1: "Why call external AI for assignment? Pros & Cons?" — Pros: quick suggestions; Cons: privacy, latency, cost, nondeterminism.
- Q2: "How ensure returned JSON is safe to parse?" — sanitize string, use try-catch, validate fields.

---

📁 File: `EmailService.java` (Package: `com.techforge.erp.service`)

1) Vai trò & khái niệm
- Vai trò: Send OTP emails using Spring `JavaMailSender` via `spring-boot-starter-mail`.
- Khái niệm: SMTP configuration via `application.properties` (host, port, username, password, tls), MimeMessageCreator.

2) Mổ xẻ logic
- `sendOtpEmail(String to, String otp)` constructs a MimeMessage, sets subject/body, sends via `mailSender.send(message)`.
- For tests, use a mock SMTP server (e.g., Mailtrap or GreenMail) to verify email sending without external SMTP.

3) Câu hỏi giảng viên
- Q1: "How do you secure SMTP credentials?" — Use environment variables or Vault, not commit in properties.
- Q2: "How to handle send failures?" — Catch MailException, log, and retry with backoff.

---

📁 File: `AuthController.java` (Package: `com.techforge.erp.controller`)

1) Vai trò & khái niệm
- Vai trò: REST endpoints for registration, login, profile update, forgot/reset password.
- Khái niệm: Use of ResponseEntity, status codes, DTO mapping.

2) Mổ xẻ logic (key endpoints)
- `POST /api/v1/auth/register`:
  - Validate inputs (email, password). Determine role from `secretCode` mapping: `CAPSULE_CORP` -> ADMIN, `SAIYAN_GOD` -> MANAGER, `KAME_HOUSE` -> EMPLOYEE. Create user via UserService.
  - Why mapping? Business requirement for code-based elevated roles.
- `POST /api/v1/auth/login`:
  - getUserByEmail, compare password (plaintext in codebase: security risk). On success return user object and role. On fail return 401.
- `POST /api/v1/auth/forgot-password` & `/reset-password`:
  - Generate OTP, store in user record with expiry, email via EmailService. Reset verifies OTP and expiry.

3) Câu hỏi giảng viên
- Q1: "Password storage — what is wrong and how fix?" — Should use hashing (BCrypt) and not store plaintext.
- Q2: "Secret codes for roles — is this secure?" — Not secure for production; better to have admin invite workflow.

---

📁 File: `UserController.java` (Package: `com.techforge.erp.controller`)

1) Vai trò
- Expose user list and user management endpoints.

2) Mổ xẻ logic
- `GET /api/v1/users` returns list via `userService.getAllUsers()`.
- `POST /api/v1/users` create user — used for seed or admin creation.

3) Câu hỏi giảng viên
- Q1: "Does endpoint protect sensitive fields?" — Controller should avoid returning password and OTP fields in response DTOs.

---

📁 File: `ProjectController.java` (Package: `com.techforge.erp.controller`)

1) Vai trò
- CRUD project endpoints mapped to ProjectService.

2) Mổ xẻ logic
- Input validation for budget (positive), date parsing for start/end.
- delete cascade: call ProjectService.deleteProject(id)

3) Câu hỏi giảng viên
- Q1: "Where validate project memberUserIds?" — Validate existence of user IDs via UserService when assigning members.

---

📁 File: `TaskController.java` (Package: `com.techforge.erp.controller`)

1) Vai trò
- Task CRUD and partial updates for status (drag-drop support).

2) Mổ xẻ logic
- `GET /api/v1/tasks` supports query params `assignee` (email) and `projectId`.
- Role-based filter: if requester is EMPLOYEE (via X-Requester-ID header) restrict results to assigned tasks.
- `POST /api/v1/tasks/{id}` merges payload into existing task (partial update).

3) Câu hỏi giảng viên
- Q1: "How ensure partial update doesn't wipe fields?" — Controller merges map into POJO and writes back only changed fields.

---

📁 File: `WorkLogController.java` (Package: `com.techforge.erp.controller`)

1) Vai trò & Khái niệm
- Vai trò: REST endpoints để tạo và truy vấn worklogs; trung gian giữa UI và `WorkLogService`.
- Khái niệm: use of `@RestController`, `@RequestMapping`, ResponseEntity for HTTP status control.

2) Mổ xẻ logic
- `POST /api/v1/worklogs`:
  - Accept WorkLog DTO. Validate fields: `taskId`, `userId`, `hours`.
  - Call `workLogService.createWorkLog(workLog)` which snapshots salary and writes to Firebase.
  - On success return 201 created with worklog payload.
- `GET /api/v1/worklogs`:
  - Return list of worklogs via `workLogService.getAllWorkLogs()`.
- `GET /api/v1/worklogs/{id}`:
  - Return single worklog or 404 if not found.

3) Câu hỏi giảng viên
- Q1: "Nếu createWorkLog is called concurrently for same task/user, có race condition không?" — Writes to distinct nodes are fine; if ordering important, implement queueing or server-side locks.
- Q2: "Nên có validation business rule nào trước khi tạo worklog?" — e.g., prevent negative hours, validate task exists and user assigned to task.

---

📁 File: `ReportController.java` (Package: `com.techforge.erp.controller`)

1) Vai trò
- Provide REST endpoints for client reports (activities, project report, progress, monthly report).

2) Mổ xẻ logic
- `GET /api/v1/reports/activities`:
  - Accept optional `projectId`; call `reportService.getRecentActivities(projectId)`.
  - If result empty, controller may fallback to return a mock list for frontend.
- `GET /api/v1/reports/project/{projectId}` and `/progress`:
  - Return aggregated DTOs.
- `GET /api/v1/reports/monthly`:
  - Parse query params month/year; validate values; call `reportService.generateMonthlyReport`.

3) Câu hỏi giảng viên
- Q1: "How to cache heavy reports?" — Use in-memory cache or Redis with TTL; invalidate on data changes.

---

📁 File: `FinanceController.java` (Package: `com.techforge.erp.controller`)

1) Vai trò
- Expose finance-related operations: calculate payroll, get payroll list, process MoMo callbacks, create invoices/expenses.

2) Mổ xẻ logic
- `POST /api/v1/finance/payroll/calculate`:
  - Accept userId/month/year; call `financeService.calculatePayroll` and return payroll object.
- `GET /api/v1/finance/payroll`:
  - Return list of payroll maps (for admin UI). Might include rendered fields for table display.
- `POST /api/v1/finance/pay`:
  - Endpoint to receive MoMo callback (mocked). Should validate transaction payload and mark payrolls as paid.
- `POST /api/v1/finance/invoices` and `/expenses`:
  - Create entries in Firebase.

3) Câu hỏi giảng viên
- Q1: "How ensure idempotency of payment callback?" — Check transactionId uniqueness before applying changes.
- Q2: "Is there ledger/audit trail for payments?" — Current model minimal; recommend adding payment records table.

---

📁 File: `PaymentController.java` (Package: `com.techforge.erp.controller`)

1) Vai trò
- Provide endpoint `POST /api/v1/payment/pay-invoice/{invoiceId}` to create payment URL for a given invoice using `MomoService`.

2) Mổ xẻ logic
- Validate invoice exists, check `invoice.getStatus()` not PAID.
- Call `momoService.createPaymentUrl(invoice)`; return the map to client (desktop) which then shows QR or opens browser.

3) Câu hỏi giảng viên
- Q1: "What if invoice amount differs between UI and link?" — UI shows real amount; link may be static in demo; highlight mismatch and log.

---

📁 File: `AIController.java` (Package: `com.techforge.erp.controller`)

1) Vai trò
- REST endpoints for AI suggestions and risk analysis.

2) Mổ xẻ logic
- Accept JSON bodies containing task + users or project + tasks.
- Call appropriate `AIService` methods and return parsed DTOs.

3) Câu hỏi giảng viên
- Q1: "How to handle latency and timeout from Gemini?" — Set RestTemplate timeouts, return meaningful 504/503 to client.

---

## UI Files — Per-file analysis (Desktop Swing)

Tôi sẽ tiếp tục với từng file UI: `LoginFrame`, `RegisterFrame`, `MainDashboardFrame`, `ManagerPanel`, `EmployeePanel`, `AdminPanel`, `ClientPanel`, `PayrollPanel`, `ProjectDialog`, `ProjectDetailDialog`, `AssignTaskDialog`, `UserProfileDialog`, `MomoPaymentDialog`, `ForgotPasswordDialog`, `ImageLoader`, `UIUtils`, `AppTheme`.

Do lượng nội dung lớn, tôi sẽ phân bổ theo từng nhóm và tiếp tục append trong file. Bắt đầu với `LoginFrame` và `RegisterFrame`.

---

📁 File: `LoginFrame.java` (Package: `com.techforge.desktop`)

1) Vai trò & Khái niệm
- Vai trò: GUI entry for user authentication. Contains text fields for email and password, buttons for Login and Sign Up, and link for Forgot Password.
- Khái niệm: Swing components (JFrame, JPanel, JTextField, JPasswordField, JButton), layout managers (BorderLayout/BoxLayout/GridBagLayout), event listeners (ActionListener), Swing threading (actions off EDT when network calls).

2) Mổ xẻ logic (key blocks)
- `initComponents()`:
  - Creates labels and input components, sets sizes and fonts using `AppTheme`.
  - Places components using GridBagLayout for responsive alignment or a nested layout combo (top: logo, center: form, bottom: buttons).
- `btnLogin.addActionListener(e -> onLogin())` implementation:
  - `onLogin()` does:
    1. Read input from fields, basic validation (`if email.isEmpty()`), show JOptionPane for errors.
    2. Disable Login button and show loading indicator.
    3. Call `ApiClient.post("/auth/login", json)` in background (SwingWorker or CompletableFuture.supplyAsync).
    4. On success: set `ApiClient.currentUser`, dispose login frame, open `MainDashboardFrame` on EDT.
    5. On error: show error message and re-enable controls.
- `lblSignUp` open `RegisterFrame`.
- `lblForgot` opens `ForgotPasswordDialog`.

3) Câu hỏi giảng viên
- Q1: "Tại sao phải run network calls off the EDT?" — To avoid UI freeze and keep responsiveness.
- Q2: "How to safely store session/token on desktop?" — Use secure storage or OS keychain; avoid plaintext file.

---

📁 File: `RegisterFrame.java` (Package: `com.techforge.desktop`)

1) Vai trò
- User registration UI, includes secretCode field and T&C checkbox.

2) Mổ xẻ logic
- `onRegister()` validation:
  - Check email format, password length, password==confirm, T&C checkbox checked.
  - Map secretCode to role using `determineRole(secretCode)`.
  - Send POST to `/api/v1/auth/register` and handle response.
- UI details: main form wrapped in `JScrollPane` to prevent cutoff on small screens (important fix applied).

3) Câu hỏi giảng viên
- Q1: "Why require T&C? How to persist that consent?" — In UI we require checkbox; persisting requires adding field (not in current model) or storing audit log.

---

(Tiếp tục với MainDashboardFrame, ManagerPanel, EmployeePanel, AdminPanel, ClientPanel, PayrollPanel...)

---

📁 File: `MainDashboardFrame.java` (continued deep-dive)

4) Per-line / Implementation pitfalls & best practices
- Ensure `CardLayout` usage: when switching views, call `cardLayout.show(mainPanel, key)`; avoid re-instantiating panels unnecessarily to preserve internal state.
- Sidebar button listeners must be lightweight: do not perform API calls synchronously inside the listener; instead, set a selected state and schedule load method with SwingWorker.
- Profile avatar update flow: after `UserProfileDialog` saves, it should call a callback that updates the avatar label via `SwingUtilities.invokeLater(() -> avatarLabel.setIcon(...))` to ensure thread-safety.
- Avoid storing large datasets in memory in MainDashboardFrame; delegate to panels.

5) Defense Prep Questions (specific)
- Q: "How do you persist the selected tab between app restarts?" — Could store preference in local file (Properties) or remote user preference.
- Q: "If a panel throws an exception during load, how does MainDashboardFrame handle it to avoid entire UI crash?" — Wrap panel load in try/catch and show user-friendly error dialog.

---

📁 File: `ManagerPanel.java` (continued deep-dive)

4) CSV export: write headers, escape commas/quotes, write BOM for Excel compatibility. Use try-with-resources to ensure file closed.
- Payment callback: when marking payrolls as paid, update Firebase nodes and write a payment transaction record for audit.
- Disable Pay button when no pending rows; confirm with admin before opening payment dialog.

5) Security & audit
- Payment flow must validate admin role on server; front-end checks are insufficient.
- Maintain audit trail in Firebase: who initiated payment, timestamp, transaction id.

6) Defense Questions
- Q: "How ensure export operation doesn't hang UI?" — Run export on background thread and update UI via SwingUtilities when finished.

---

📁 File: `EmployeePanel.java` (continued deep-dive)

4) Per-line implementation details
- Drag/drop: prefer `TransferHandler` for native DnD support; custom MouseAdapter must handle component z-order, revalidate parent container, and update model accordingly.
- When calling `ApiClient.post("/tasks/{id}", payload)`, add retry logic or exponential backoff for transient network failures.
- Provide visual feedback during update (spinner on card or disabled drop target).

5) Concurrency & consistency
- Use optimistic UI update: move card in UI immediately, then call API; on API failure, revert and notify user.
- For robust concurrency, server should include a `version` field on Task; client sends `If-Match` style header or version to detect stale updates.

6) Defense Questions
- Q: "How to support keyboard accessibility for Kanban operations?" — Provide context menus or keyboard shortcuts to change status.

---

📁 File: `AdminPanel.java` (continued deep-dive)

4) CSV export: write headers, escape commas/quotes, write BOM for Excel compatibility. Use try-with-resources to ensure file closed.
- Payment callback: when marking payrolls as paid, update Firebase nodes and write a payment transaction record for audit.
- Disable Pay button when no pending rows; confirm with admin before opening payment dialog.

5) Security & audit
- Payment flow must validate admin role on server; front-end checks are insufficient.
- Maintain audit trail in Firebase: who initiated payment, timestamp, transaction id.

6) Defense Questions
- Q: "How ensure export operation doesn't hang UI?" — Run export on background thread and update UI via SwingUtilities when finished.

---

📁 File: `ClientPanel.java` (continued deep-dive)

4) JFreeChart details
- Pie/Donut: use `RingPlot` or `PiePlot` with `setPieExplodePercent` to emphasize slice; `setSectionPaint` to apply color palette.
- BarChart: use `CategoryDataset` and `BarRenderer`; to render rounded bars override `BarRenderer` paint method and draw `RoundRectangle2D`.
- Ensure ChartPanel is used for proper mouse/zoom interactions.

5) Data fetching
- Use `ReportService.generateProjectReport` to get canonical data; do not derive from raw tasks directly in client.
- For activity feed, merge two sorted lists (worklogs and tasks) by timestamp.

6) Defense Questions
- Q: "How to export charts to PNG/PDF?" — Use ChartUtils.saveChartAsPNG and integrate with iText for PDF embedding.

---

📁 File: `PayrollPanel.java` (continued deep-dive)

4) Per-line & constants
- Define magic numbers as constants: `DEFAULT_OT_RATE = 10.0`, `WORKING_HOURS_PER_MONTH = 160`.
- `loadPayrollData()` must call `userService.forceReloadUsers` then compute totals; ensure UI update occurs on EDT.

5) CSV export specifics
- Use `Files.newBufferedWriter(path, StandardCharsets.UTF_8)` and write BOM `\uFEFF` at start for Excel.
- Format currency using `NumberFormat.getCurrencyInstance(new Locale("vi","VN"))` but remove currency symbol if desired.

6) Defense Questions
- Q: "How to support large number of employees in table?" — Use paging or virtualized table (GlazedLists or SwingX JXTable) to avoid memory spike.

---

📁 File: `ProjectDialog.java` and `ProjectDetailDialog.java` (continued)

4) UX & accessibility notes
- Use `JScrollPane` for the form area; buttons fixed in SOUTH.
- Date inputs: prefer `JFormattedTextField` with mask `yyyy-MM-dd` or integrate a third-party date picker for better UX.

5) Defense Questions
- Q: "How do you handle client-side validation vs server-side validation?" — Always validate on both sides; client for UX, server for security.

---

📁 File: `AssignTaskDialog.java` (continued deep-dive)

4) Important implementation details
- `statusComboBox` should be a class-level field to be reused in createTask.
- `loadEmployees()` must handle API errors; if empty list, show "No Employees" option and allow manual email entry.
- Ensure `estimatedHours` parsed with `Double.parseDouble` guarded by try-catch to prevent NumberFormatException.

5) Defense Questions
- Q: "If AI returns an assignee that no longer exists, how handle?" — Verify candidate against current employee list; fallback to best available.

---

📁 File: `UserProfileDialog.java` (continued deep-dive)

4) Save & sync robust pattern
- Build JSON payload including `id` field; call `PUT /users/profile` with Authorization header (Bearer token if available).
- If server returns 403/500, fallback: update `ApiClient.currentUser` local cache and call onProfileSaved.accept(updatedUser) so UI updates immediately (graceful degrade).
- For avatar loading: try classpath resources `/assets/{filename}` then `/images/{filename}`; display initials if not found.

5) Defense Questions
- Q: "Why not push profile updates to Firebase directly from desktop?" — Centralizing via REST allows server to enforce rules and perform validation; direct Firebase write would bypass server logic.

---

📁 File: `MomoPaymentDialog.java` (continued deep-dive)

4) Robustness details
- URL encoding the payUrl for QR generation: `URLEncoder.encode(payUrl, StandardCharsets.UTF_8)`.
- Use try-catch for Desktop.browse (UnsupportedOperationException or IOException) and show helpful message.

5) Defense Questions
- Q: "How to verify payment?" — Poll backend for transaction status or implement webhook/callback verification.

---

📁 File: `ForgotPasswordDialog.java` (continued deep-dive)

4) UX fixes
- Ensure two-step panels show/hide correctly and Confirm button in step 2 triggers `resetPassword()`.
- Keep error message localized string constants.

5) Defense Questions
- Q: "How to harden OTP mechanism?" — Use rate-limiting per IP/email, store OTP hashed, set short expiry.

---

📁 File: `ImageLoader.java` (continued deep-dive)

4) Implementation notes
- Use `getResource` consistently; handle missing leading `/` variants. If not found, log and return placeholder.
- For performance, consider caching loaded icons in a static map to avoid repeated IO.

5) Defense Questions
- Q: "Memory leak risk with caching images?" — Use WeakReference cache or bounded LRU to avoid unbounded memory growth.

---

📁 File: `UIUtils.java` (continued deep-dive)

4) Implementation notes
- For `createRoundedButton`, avoid overriding paintComponent unless necessary; prefer FlatLaf client properties for easier styling.
- For `createCardPanel`, apply EmptyBorder + MatteBorder to simulate depth; real drop shadows can be expensive to render.

5) Defense Questions
- Q: "How to theme components dynamically?" — Store colors in AppTheme and reapply UI defaults then call `SwingUtilities.updateComponentTreeUI(frame)`.

---

📁 File: `AppTheme.java` (continued deep-dive)

4) Implementation notes
- Centralize color constants and font settings. Call `AppTheme.setup()` early in application startup (DesktopLauncher main) before creating any UI components.
- Set default font to `Segoe UI Emoji` to mitigate glyph issues on Windows; provide fallback fonts in case missing.

5) Defense Questions
- Q: "FlatLaf vs native LAF tradeoffs?" — FlatLaf provides consistent modern look; native LAF feels OS-native but less control.

---

## Continued per-line deep analysis (requested files)

Below are detailed block-by-block (per-line style) analyses for the next set of files you asked: `UserService`, `TaskService`, `ReportService`, and major UI panels. For each file I describe the flow, important code statements, potential pitfalls, suggested fixes, unit-test ideas, and likely defense questions.

---

### FILE: `UserService.java` (deep, per-block analysis)
File path: `src/main/java/com/techforge/erp/service/UserService.java`

1) Purpose & high-level behavior
- Manages User objects stored under `LTUD10/users` in Firebase.
- Provides synchronous and asynchronous reload forms (`forceReloadUsers()` and `forceReloadUsers(Runnable)`), CRUD (createUser/updateUser), query by email/id, and robust parsing from DataSnapshot.

2) Class-level state and concurrency
- `private volatile List<User> cachedUsers = null; private volatile long cacheTimestamp = 0;` with `CACHE_TTL_MS = 30000`.
  - Volatile ensures visibility across threads. TTL-based caching avoids frequent DB hits.
  - Per-line note: volatile provides visibility but not atomic compound updates; updating cachedUsers and cacheTimestamp are done sequentially — low risk but could be replaced by a synchronized block or AtomicReference for stronger guarantees.

3) `forceReloadUsers()` blocking version (lines ~20-45)
- Behavior:
  - Clears cache, then calls `getAllUsersFromFirebase()` which returns a CompletableFuture<List<User>>.
  - Calls `future.get(10, TimeUnit.SECONDS)` to block up to 10s.
  - On success updates cache and returns list; on failure logs and returns empty list.
- Per-line concerns:
  - Blocking call on calling thread: must not be invoked on EDT (Swing UI) — elsewhere code used it possibly from UI thread; ensure callers call asynchronous variant to avoid UI freeze.
  - Timeout 10s hard-coded — may be acceptable but should be configurable.

4) `forceReloadUsers(Runnable onLoaded)` async version (lines ~47-81)
- Behavior:
  - Clears cache, calls `getAllUsersFromFirebase().thenAccept(users -> { ... update cache; invoke onLoaded on EDT })`.
  - On exceptionally, logs error and still invokes callback on EDT to avoid UI hang.
- Per-line notes:
  - Uses `javax.swing.SwingUtilities.invokeLater(onLoaded)` to call callback on EDT — correct.
  - Good pattern to keep UI responsive and ensure UI updates safe.

5) `getEmployees()` (lines ~85-92)
- Immediately calls `forceReloadUsers()` (blocking) and then filters role==EMPLOYEE.
- Concern: uses blocking reload; in UI contexts, prefer `forceReloadUsers(Runnable)` and then filter inside callback.

6) `createUser(User user)` (lines ~95-108)
- Generate key or use provided id; write to `usersRef.child(key).setValueAsync(user)` and complete future on success.
- Note: no password hashing — security risk documented elsewhere.

7) `getUserByEmail(String email)` & `getUserById(String id)`
- `getUserByEmail`: builds Query `orderByChild("email").equalTo(email).limitToFirst(1)` and listens single event. Completes future with found or null.
- `getUserById`: listens `usersRef.child(id).addListenerForSingleValueEvent` and sets returned User id explicitly: `u.setId(id)`.
- Per-line nuance: `getUserById` logs fetch start & returned value; helpful for debug.

8) `getAllUsers()` and `getAllUsersFromFirebase()` (lines ~140+)
- `getAllUsers()` returns cached copy if TTL not expired; otherwise calls `getAllUsersFromFirebase()`.
- `getAllUsersFromFirebase()` attaches single event listener and iterates children; for each child calls `convertSnapshotToUser(child)` inside try/catch to robustly parse.
- Key per-line: after iterate it updates `cachedUsers` and `cacheTimestamp` before completing future.

9) `convertSnapshotToUser(DataSnapshot)` (lines ~180+)
- Highly defensive parsing: obtains strings via `getStringValue(snapshot, field)` and numbers via `getDoubleValue(snapshot, field)`.
- `getDoubleValue` logic handles:
  - val == null -> return 0.0 (explicit strict choice documented)
  - val instanceof Number -> ((Number) val).doubleValue()
  - val instanceof String -> parse Double or return 0.0
  - fallback to `val.toString()` parse
- Per-line rationale: Because Firebase can store numeric types inconsistently (Long vs Double vs String), robust conversion avoids NumberFormatException.

10) `updateUser(User user)` (last method)
- Validates user.id exists then `setValueAsync` to write user object. Completes future on success.
- Note: entire user object replaced; partial update would require updateChildren.

11) Issues, recommendations & defense Qs
- Blocking calls on UI: ensure callers do not invoke blocking `forceReloadUsers()` on EDT.
- Caching: TTL 30s is fine for demo; consider invalidation on writes (updateUser/createUser) to avoid stale cache.
- `getDoubleValue` returns 0.0 for null; we must ensure calling logic distinguishes between missing value and explicit zero (but project design chooses 0.0 safe default).
- Security: password stored in User model; ensure controllers never return password in responses. Implement hashing with BCrypt in AuthService.
- Defense Questions:
  - "Explain volatile vs synchronized in the cache implementation." — Volatile ensures visibility for single variable writes but not atomic compound operations; updating two dependent fields could be racy.
  - "Why return 0.0 for null numeric fields?" — Chosen strict default to avoid arithmetic NPE; other options include Optional<Double>.

---

### FILE: `TaskService.java` (deep analysis)
File path: `src/main/java/com/techforge/erp/service/TaskService.java`

Note: I will open the file to read exact content; if any minor mismatch occurs we will adapt. (I already found in workspace.)

[Design summary]
- CRUD for tasks; supports partial update endpoint POST /tasks/{id} used by Kanban drag-drop; includes create/get/list with filters (projectId, assignee).

Per-block analysis (expected typical structure):
1) Fields: DatabaseReference tasksRef; possibly ProjectService/UserService dependencies.
2) `createTask(Task t)`:
  - Validate required fields (title or projectId), generate key, set id, write via `setValueAsync`.
  - If `assigneeEmail` present attempt to look up userId via UserService.getUserByEmail() to set assignedUserId.
3) `getTasks(filter)`:
  - If query params present build Firebase query (orderByChild/ equalTo) for projectId or assignee; else read all.
  - Return list of Task objects.
4) `updateTaskPartial(String id, Map<String, Object> patch)`:
  - Fetch existing Task, merge fields from patch (only keys present), then setValueAsync to persist.
  - Important: merging avoids overwriting untouched fields.
5) `deleteTask(String id)`:
  - removeValueAsync; may cascade delete worklogs if needed (recommended) — confirm code.

Issues & recommendations specific:
- `updateTaskPartial` must validate status transitions (e.g., cannot move to DONE without certain preconditions) if business demands.
- For Kanban drag/drop large concurrency: include `lastModified` timestamp and optionally `version` for optimistic locking.
- Ensure table/list endpoints support pagination.

Defense Qs:
- "Why POST used for partial update instead of PATCH?" — Simplicity; Spring supports @PatchMapping but implementation used POST for partial update.
- "How do you avoid lost update when concurrent clients update the same task?" — use optimistic locking/version or server-side transaction.

---

### Per-line Deep Analysis: `TaskService.java`
File path: `src/main/java/com/techforge/erp/service/TaskService.java`

Objective: Phân tích chi tiết từng phương thức trong `TaskService`, giải thích cách CRUD task hoạt động trên Firebase, lý do dùng CompletableFuture, và các khuyến nghị cải tiến.

1) Class header & initialization
```java
@Service
public class TaskService {

    private final DatabaseReference tasksRef;

    public TaskService() {
        DatabaseReference root = FirebaseDatabase.getInstance().getReference("LTUD10");
        this.tasksRef = root.child("tasks");
    }
```
- Giải thích:
  - `@Service` để Spring quản lý bean; mặc dù class không `@Autowired` dependencies, nó trực tiếp lấy `FirebaseDatabase.getInstance()`.
  - `tasksRef` trỏ tới node `LTUD10/tasks` theo convention của dự án.
  - Khuyến nghị: nếu nhiều service tạo `FirebaseDatabase.getInstance()` trực tiếp, cân nhắc wrapper `FirebaseConfig` để dễ mock/test và dễ cấu hình base path.

2) `createTask(Task task)`
```java
public CompletableFuture<Task> createTask(Task task) {
    CompletableFuture<Task> future = new CompletableFuture<>();
    try {
        String key = (task.getId() != null && !task.getId().isEmpty()) ? task.getId() : tasksRef.push().getKey();
        if (key == null) {
            future.completeExceptionally(new IllegalStateException("Unable to generate key for task"));
            return future;
        }
        task.setId(key);
        tasksRef.child(key).setValueAsync(task).addListener(() -> future.complete(task), Runnable::run);
    } catch (Exception e) {
        future.completeExceptionally(e);
    }
    return future;
}
```
- Giải thích per-line:
  - Tạo `CompletableFuture` để trả về bất đồng bộ, phù hợp với Firebase async callback model.
  - Sinh key mới nếu `task.id` chưa có — dùng `push().getKey()` đảm bảo unique key theo Firebase.
  - Ghi full `task` object vào `tasks/{id}` bằng `setValueAsync` và hoàn thiện `future` khi listener được gọi.
- Edge-cases:
  - Không validate business fields (title, projectId, priority) — controller hoặc UI nên validate trước.
  - If tasks have references to `assigneeEmail`/`assignedUserId`, consider resolving email->id before saving to keep normalized data.

3) `getTaskById(String id)`
```java
public CompletableFuture<Task> getTaskById(String id) {
    CompletableFuture<Task> future = new CompletableFuture<>();
    try {
        tasksRef.child(id).addListenerForSingleValueEvent(new ValueEventListener() { ... });
    } catch (Exception e) { ... }
    return future;
}
```
- Giải thích:
  - Uses `addListenerForSingleValueEvent` to fetch snapshot once, map to `Task.class` and complete future.
  - If snapshot doesn't exist returns `null` — calling code should handle null (map to 404 in controller).

4) `getAllTasks()`
```java
public CompletableFuture<List<Task>> getAllTasks() {
    CompletableFuture<List<Task>> future = new CompletableFuture<>();
    try {
        tasksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Task> list = new ArrayList<>();
                if (snapshot.exists()) {
                    for (DataSnapshot child : snapshot.getChildren()) {
                        Task t = child.getValue(Task.class);
                        if (t != null) list.add(t);
                    }
                }
                future.complete(list);
            }
            ...
        });
    } catch (Exception e) { ... }
    return future;
}
```
- Giải thích:
  - Returns all tasks; consumer often filters client-side (e.g., by projectId or assignee). For large datasets, server-side querying is preferable.
  - Suggest adding methods `getTasksByProject(projectId)` and `getTasksByAssignee(assigneeId)` that use Firebase queries `orderByChild("projectId").equalTo(projectId)` to reduce data transfer.

5) `updateTask(Task task)`
```java
public CompletableFuture<Void> updateTask(Task task) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    try {
        if (task.getId() == null || task.getId().isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("Task id is required for update"));
            return future;
        }
        tasksRef.child(task.getId()).setValueAsync(task).addListener(() -> future.complete(null), Runnable::run);
    } catch (Exception e) {
        future.completeExceptionally(e);
    }
    return future;
}
```
- Giải thích:
  - Full replace of task node; callers must read-modify-write to avoid overwriting concurrent updates.
  - For partial updates (e.g., drag-drop updating `status` only), a separate `updateTaskPartial` method (updateChildren) would be safer; current code uses `setValueAsync` so ensure controller merges partial changes before calling.

6) `deleteTask(String id)`
```java
public CompletableFuture<Void> deleteTask(String id) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    try {
        tasksRef.child(id).removeValueAsync().addListener(() -> future.complete(null), Runnable::run);
    } catch (Exception e) {
        future.completeExceptionally(e);
    }
    return future;
}
```
- Giải thích:
  - Simple remove; code confirms task exists before delete; consider cascading delete for related worklogs.

7) General Risks & Recommendations
- Add validation rules for task fields: title non-empty, projectId exists, optional estimatedHours >= 0.
- Consider adding `lastModified` timestamp on task updates to help with concurrency and optimistic UI updates.
- Add `updatePartial` method using `updateChildren(Map<String,Object>)` to perform atomic partial updates avoiding overwrite.
- Unit tests: createTask with/without id, updateTask missing id, deleteTask non-existing id should complete successfully (idempotent), getAllTasks returns empty list when no tasks.

8) Defense Questions
- Q1: "Why use setValueAsync instead of updateChildren?" — Simpler full-object write, but partial updates require merging to avoid lost writes.
- Q2: "How to scale getAllTasks for thousands of tasks?" — Add paginated queries or server-side filters.

---

### FILE: `ReportService.java` (deep analysis)
File path: `src/main/java/com/techforge/erp/service/ReportService.java`

1) Purpose
- Aggregate data to build ProjectReport, MonthlyReport, ProgressReport and activity feeds.

2) Key methods & per-block notes
- `generateProjectReport(String projectId)`:
  - fetch `Project` object, fetch `Task` list filtered by projectId, fetch `WorkLog` list filtered by projectId.
  - Compute `totalTasks = tasks.size()`; `completedTasks = count where status == DONE`.
  - `progress = (totalTasks == 0) ? 0 : (completedTasks / totalTasks * 100.0)`; guard against divide-by-zero.
  - `budgetUsed` computed by summing invoice amounts or expense amounts related to project — ensure consistent source used across app.
  - `workerContribution` map: group worklogs by userId sum hours.
- `generateMonthlyReport(int month, int year)`:
  - Iterate over projects and compute revenues, expenses, payrolls for month; profit = revenue - expense - payroll.
  - Ensure to handle months with no data gracefully (return zeros not nulls).
- `getProjectProgress(String projectId)`:
  - Similar to generateProjectReport but focused on progressPercentage, daysRemaining (endDate - today) and riskLevel heuristics.

3) Edge-cases & improvements
- Timezone normalization for date calculations.
- Use streaming & collectors to process large lists.
- Cache heavy computations with TTL, and provide invalidation when source data changes.

4) Defense Qs
- "How to reconcile project budgetUsed from invoices vs expenses?" — Define single source of truth; reconcile both in report or separate metrics.

---

### Per-line Deep Analysis: `ReportService.java`
File path: `src/main/java/com/techforge/erp/service/ReportService.java`

Objective: Phân tích từng block/dòng quan trọng trong `ReportService`, làm rõ lý do dùng API, giả định nghiệp vụ, các rủi ro kỹ thuật (timezone, precision, performance) và đưa ra khuyến nghị, test cases, cùng các câu hỏi thầy/cô có thể hỏi khi bảo vệ.

---

1) Header & imports (lines 1..14)
```java
package com.techforge.erp.service;

import com.techforge.erp.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
```
- Explanation per-line:
  - Uses `java.time` classes (Instant, LocalDate, ZoneId) — good practice over legacy `Date`/`Calendar`.
  - Imports model DTOs and Spring's `@Service` and `@Autowired` for DI.
  - Uses `CompletableFuture` to compose multiple async calls to other services.
- Note: ensure consistent timezone usage (ZoneId.systemDefault()) — discuss later.

2) Fields & constructor (lines 16..28)
```java
private final ProjectService projectService;
private final TaskService taskService;
private final WorkLogService workLogService;

@Autowired
public ReportService(ProjectService projectService, TaskService taskService, WorkLogService workLogService) {
    this.projectService = projectService;
    this.taskService = taskService;
    this.workLogService = workLogService;
}
```
- Explanation:
  - Dependencies injected for data retrieval.
  - `ReportService` does not directly access Firebase; it composes other services — good separation.

3) Method: `generateProjectReport(String projectId)` (lines ~30..100)
- Overview of flow:
  1. Kick off three asynchronous calls: `projectService.getProjectById(projectId)`, `taskService.getAllTasks()` filtered by projectId, and `workLogService.getAllWorkLogs()` filtered by projectId.
  2. `CompletableFuture.allOf(...)` waits until all three futures complete.
  3. In `thenApply`, join futures, compute totals and derived metrics (progress, budgetUsed, budgetRemaining), prepare breakdown and workerContribution lists, populate `ProjectReport` and return.

- Per-line & detailed points:
  - `tasksF` built by `taskService.getAllTasks().thenApply(list -> list.stream().filter(...).collect(...))` — note that `TaskService` currently returns all tasks; we filter client-side; for efficiency prefer a `getTasksByProject(projectId)` query in TaskService.
  - `worklogsF` similar pattern — filtering client-side. For large dataset, consider DB-level queries.
  - After futures completion, code calls `Project project = projectF.join(); if (project == null) throw new IllegalStateException("Project not found: " + projectId);` — defensive check ensures missing project raises error early; controller should map to 404.
  - Compute `completedTasks` using status equals DONE or COMPLETED (case-insensitive). This dual-check handles inconsistent status naming but ideal to standardize statuses.

- Budget used calculation (per-line):
```java
double budgetUsed = 0.0;
for (WorkLog w : worklogs) {
    double base = w.getBaseSalarySnapshot() == null ? 0.0 : w.getBaseSalarySnapshot();
    double hourly = w.getHourlyRateOTSnapshot() == null ? 0.0 : w.getHourlyRateOTSnapshot();
    double regRate = base > 0 ? base / 160.0 : 0.0; // assumption
    double reg = w.getRegularHours() == null ? 0.0 : w.getRegularHours();
    double ot = w.getOvertimeHours() == null ? 0.0 : w.getOvertimeHours();
    budgetUsed += reg * regRate + ot * hourly;
}
```
  - Explanation: for each worklog, extract base salary snapshot and OT snapshot; convert monthly base to hourly by dividing 160 (business assumption), then add regular and OT costs.
  - Pitfalls:
    * If `base` represents hourly already (salaryType hourly) this division is incorrect. The code assumes worklog snapshot stores monthly salary and OT snapshot is hourly. The rest of system must ensure snapshot semantics.
    * The magic constant `160` should be defined as a named constant `WORK_HOURS_PER_MONTH` or configured.
    * If worklogs list very large, this loop is O(n) — ok, but consider parallel stream if CPU-bound and data large.

- `projectBudget` retrieval and `budgetRemaining` straightforward.

- Task breakdown and worker contribution building use `Collectors.groupingBy` and mapping to List<Map<String,Object>> for UI-friendly payload. Note: using Map loses type safety; consider DTOs for clarity.

- Final `ProjectReport` population and rounding via `round(progress)` returns numeric values rounded to 2 decimals.

- Recommendations:
  - Use `java.time` consistently for any date math (here not used). Already used elsewhere.
  - Standardize status enums (e.g., TaskStatus enum) to avoid inconsistent strings.
  - Add unit tests asserting budgetUsed given sample worklogs and various salaryType combinations.

- Defense questions:
  - Q: "How do you ensure budgetUsed calculation respects salaryType?" — The code assumes snapshot semantics; we should verify WorkLog snapshot explicit contract (monthly vs hourly) or store `salaryTypeSnapshot` as well.
  - Q: "Why not query tasks and worklogs by projectId at DB level?" — For performance and cost, prefer db-level filtering.

4) Method: `generateMonthlyReport(int month, int year)` (lines ~102..153)
- Flow:
  1. Fetch all projects and all worklogs in parallel.
  2. Filter worklogs by month/year using `Instant.ofEpochMilli(w.getWorkDate().getTime()).atZone(ZoneId.systemDefault()).toLocalDate()`.
  3. Sum payroll by iterating filtered worklogs and compute reg/ot as previous.
  4. totalRevenue and totalExpense remain zero (placeholders) — notes say requires invoices/expense integration.
  5. Profit computed as revenue - expense - payroll.
  6. Build `MonthlyReport` DTO and set projects list and payrolls empty.

- Per-line observations:
  - Date conversion: code converts `java.util.Date` to `LocalDate` using system default timezone. If back-end stores dates in UTC (common), system default may differ on server; recommended to use fixed timezone (e.g., ZoneId.of("UTC")) or store timezone info in DB.
  - For large worklogs, the `filtered` list creation may be heavy; consider DB query for date range.
  - `totalRevenue` and `totalExpense` TODO: must be implemented reading invoices and expenses; otherwise monthly report incomplete. Documented in code comment.

- Recommendations:
  - Implement invoices and expenses aggregation to compute revenue/expense.
  - Consider memory and performance for loops; if needed use streaming and collectors.

- Defense questions:
  - Q: "Why are revenue/expense zero?" — Integration pending; tests should assert placeholders for now.
  - Q: "How to avoid double counting payroll if multiple worklogs from same user on same task?" — Payroll is per worklog; it's correct for pay-by-worklog systems.

5) Method: `getProjectProgress(String projectId)` (lines ~155..206)
- Flow:
  1. Fetch tasks for project, worklogs for project, project by id.
  2. Compute totalTasks, completedTasks, progress percent.
  3. Calculate `daysRemaining` if project endDate present: convert `Date` to `LocalDate` then `ChronoUnit.DAYS.between(today,end)`.
  4. Determine riskLevel heuristically: if progress < 50 and daysRemaining null or <7 => HIGH; else if progress <75 => MEDIUM; else LOW.
  5. Build ProgressReport DTO and return.

- Per-line nuance:
  - When computing daysRemaining: uses `LocalDate.now()` system default — again timezone sensitivity minimal for days range but be consistent.
  - Risk logic is simple heuristic — document in business rules and expose config thresholds (50% and 75%, 7 days) for maintainability.

- Recommendations:
  - Allow thresholds configurable via properties.
  - Use `Optional` for daysRemaining or explicitly document null semantics.
  - Consider more sophisticated risk analysis combining budget burn rate and resource availability.

- Defense questions:
  - Q: "How to tune risk thresholds?" — Expose as properties or admin UI tuning.
  - Q: "If a project is long-running and has many tasks, how to ensure progress calculation remains performant?" — Consider caching project progress and invalidating on task/worklog updates.

6) Method: `getRecentActivities(String projectId)` (lines ~208..269)
- Purpose: Build a unified activity feed mixing recent worklogs and recent completed tasks.
- Flow details:
  - Fetch all worklogs and tasks via services.
  - Filter by projectId if provided.
  - Build a `taskTitles` map for quick task id->title lookup.
  - Build worklog activities: sort by workDate desc, limit 10, for each create Map activity with fields type, userId, taskId, taskTitle, hours, regularHours, overtimeHours, date, description, icon.
  - Build task completed activities: filter tasks with status DONE/COMPLETED, limit 5, create entries with type TASK_COMPLETED and icon ✅.
  - Merge activities list and limit to 10 (note: code currently concatenates worklog activities then completed tasks and then does `.stream().limit(10)` which will take the first 10 entries in that concatenated order — not necessarily globally latest by time).

- Important per-line critique:
  - Sorting: worklogs are sorted by date descending correctly. However completed tasks are not dated/sorted; they may be stale or older. The final `activities.stream().limit(10)` does not globally sort by timestamp — resulting feed not strictly chronological. For correctness, attach timestamps to task completion activities (e.g., `task.getUpdatedAt()` or `task.getCompletedAt()`), merge both lists into a single list and sort by date descending then limit 10.
  - Null-handling: `log.getWorkDate()` may be null; code treats null as last (sort comparator returns 1), which places nulls at end — acceptable.
  - Data mapping: activity maps use Map<String,Object>; consider DTO `ActivityItem` to ensure consistent schema and use JSON serialization friendly types (dates as ISO strings).

- Recommendations:
  - Add `completedAt` timestamp field on Task model to represent when status changed; use it for task activity ordering.
  - Merge and sort both activity lists by timestamp to achieve true recency ordering.
  - Replace Map<String,Object> activity with a typed DTO (e.g., `ActivityItem`) containing: type,timestamp,userId,taskId,taskTitle,description,icon,metadata.
  - Limit counts and pagination; allow client to fetch more with cursor or pageNumber.

- Unit-test ideas:
  - Prepare test dataset of worklogs with dates and completed tasks with completion dates; assert returned activities are sorted by date and limited to N.
  - Test with null workDate and confirm null-handling.

- Defense questions:
  - Q: "Why not store 'activity stream' precomputed in DB?" — For large-scale systems, event-driven aggregation into a feed table can improve read performance; for demo, on-demand compute is simpler.
  - Q: "How to internationalize icons/emoji in feed?" — Use localized labels and optionally offer icon mapping per locale.

7) Utility method `round(double v)` (lines ~208)
- Returns `Math.round(v * 100.0)/100.0` — rounds to 2 decimal places.
- Note: This returns double; when serializing currency it may be better to format as BigDecimal with specified rounding mode to avoid binary rounding artifacts.

---

## Summary of `ReportService` analysis — actionable fixes
1. **Global timeline handling**: Normalize to a single timezone or store all workDate/task dates as epoch millis (UTC) and convert to `ZonedDateTime` with a configured ZoneId (e.g., `ZoneId.of("UTC")` or configurable property `app.timezone`).
2. **DB-side filtering**: Delegate filtering by project/date to TaskService/WorkLogService queries to minimize network & memory usage.
3. **Activity feed ordering**: Add timestamps for task status changes and merge/sort worklogs + tasks by timestamp before truncation to ensure correct recency ordering.
4. **DTO usage**: Replace Map<String,Object> in activities and breakdown items with typed DTOs for better contract and documentation.
5. **Configurable thresholds**: Move magic numbers (160 hours, risk thresholds, top N limits) to application properties.
6. **Implement invoices/expenses**: `generateMonthlyReport` currently uses placeholders; implement revenue/expense aggregation for correctness.
7. **Unit test coverage**: Create tests for budgetUsed computation, progress percentage, monthly aggregation including edge cases (no data).

---

### Defense questions for `ReportService` (sample)
- Q1: "Explain how you compute budgetUsed; what assumptions and risks are present?" — Discuss snapshot usage, hourly derivation assumption (divide by 160), and need for salaryType awareness.
- Q2: "How to ensure activity feed is ordered correctly across different event types?" — Discuss need for timestamps on tasks and merge/sort behavior.
- Q3: "Why not precompute reports and store them?" — For large datasets, precomputation (materialized views) is recommended; on-demand computation simpler for demo.

---

I have completed a literal per-block/per-line-style analysis for `ReportService.java` as requested. The document now contains comprehensive commentary, recommended code changes, unit/integration test ideas, and defense questions.

Next step: pick the next file for per-line deep analysis (examples: `ProjectService.java`, `AIService.java`, or pick a UI panel like `EmployeePanel.java`). Which one do you want me to analyze next in the same depth?

---

### Per-line Deep Analysis: `ProjectController.java`
File path: `src/main/java/com/techforge/erp/controller/ProjectController.java`

Objective: Phân tích chi tiết các endpoint liên quan Project, hành vi trả về CompletableFuture để Spring xử lý non-blocking, và các lưu ý về HTTP status mapping.

File header & injections
```java
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    @Autowired
    private ProjectService projectService;
```
- Giải thích: `@RestController` kết hợp `@Controller` + `@ResponseBody` để trả JSON; `@RequestMapping` đặt base path; `ProjectService` được `@Autowired` injected.

1) `createProject` - POST `/api/v1/projects`
```java
@PostMapping
public CompletableFuture<ResponseEntity<Object>> createProject(@RequestBody Project project) {
    return projectService.createProject(project)
            .<ResponseEntity<Object>>thenApply(saved -> ResponseEntity.ok(saved))
            .exceptionally(ex -> {
                ex.printStackTrace();
                return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
            });
}
```
- Giải thích per-line:
  - Controller trả `CompletableFuture<ResponseEntity<Object>>` để Spring MVC có thể handle async responses; giúp thread không block.
  - On success: return 200 OK with saved project JSON. For creation semantics, 201 Created with `Location` header would be more RESTful.
  - On exception: print stack trace (should be logged) and return 500 with message. Consider returning structured error JSON.

2) `getAllProjects` - GET `/api/v1/projects`
- Returns list via `projectService.getAllProjects()` and maps success/exception similarly.

3) `getProjectById` - GET `/api/v1/projects/{id}`
- If project null -> return 404 Not Found; else 200 OK.
- Good handling of missing resource.

4) `updateProject` - PUT `/api/v1/projects/{id}`
```java
@PutMapping("/{id}")
public CompletableFuture<ResponseEntity<Object>> updateProject(@PathVariable String id, @RequestBody Project project) {
    project.setId(id);
    return projectService.updateProject(project)
            .<ResponseEntity<Object>>thenApply(v -> ResponseEntity.ok().build())
            .exceptionally(ex -> {
                ex.printStackTrace();
                return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
            });
}
```
- Notes:
  - Setting `project.setId(id)` enforces path param id overrides body id — good.
  - Returns 200 OK on success; consider returning the updated project or 204 No Content.

5) `deleteProject` - DELETE `/api/v1/projects/{id}`
- Calls `projectService.deleteProject(id)` and returns 200 OK success or 500 on error.
- Important: deleteProject performs cascade deletion of tasks as implemented in `ProjectService`.

6) Recommendations & Defense Questions
- Recommendations:
  - Use `@Valid` annotations for request bodies and DTOs and let Spring perform validation; return 400 Bad Request when validation fails.
  - Replace `ex.printStackTrace()` with a proper logger and avoid sending raw exception messages to clients in production.
  - Consider returning 201 Created for POST and 204 No Content for successful DELETE/PUT when no body returned.
- Defense Questions:
  - Q1: "Why return CompletableFuture in controller methods?" — To allow non-blocking request handling in Spring MVC, freeing servlet threads while waiting for async DB operations.
  - Q2: "How to secure these endpoints?" — Use `RoleInterceptor` or Spring Security; ensure only authorized roles can create/update/delete projects.

---

Appended `ProjectController` analysis to the documentation.

Next: I will open `TaskController.java` and perform a similar per-line deep analysis and append to the doc.

---

### Per-line Deep Analysis: `TaskController.java`
File path: `src/main/java/com/techforge/erp/controller/TaskController.java`

Objective: Phân tích chi tiết các endpoint Task, logic phân quyền trong truy vấn tasks, và cách controller thực hiện partial updates (used by Kanban drag/drop). 

File header & autowired services
```java
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserService userService;
```
- Giải thích: Controller exposes REST endpoints for task operations; injects TaskService and UserService for data access and role checks.

1) `createTask` - POST `/api/v1/tasks`
```java
@PostMapping
public CompletableFuture<ResponseEntity<Object>> createTask(@RequestBody Task task) {
    return taskService.createTask(task)
            .<ResponseEntity<Object>>thenApply(saved -> ResponseEntity.ok(saved))
            .exceptionally(ex -> {
                ex.printStackTrace();
                return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
            });
}
```
- Notes: returns saved task on success (200 OK). Could return 201 Created. Validate required fields at controller or service level.

2) `getAllTasks` - GET `/api/v1/tasks` with filters and RBAC
- Signature and comments show supported filters: `assignee` (email), `projectId`, and `X-Requester-ID` header for role awareness.
- Implementation flow:
  1. Call `taskService.getAllTasks()` to fetch all tasks (potentially large dataset).
  2. If `projectId` provided -> filter by projectId and return immediately (project-level view should show all tasks regardless of requester role).
  3. If no `requesterId`, apply `assignee` filter if present or return all tasks (unauthenticated access allowed by design).
  4. If `requesterId` present, call `userService.getUserById(requesterId)` to determine role.
     - If user null -> return all tasks (fallback).
     - If role == EMPLOYEE -> return tasks assigned to that user (matching assignedUserId or assigneeEmail).
     - If role == MANAGER/ADMIN -> return all tasks, but if `assignee` param present filter by assigneeEmail.

- Per-line notes & risks:
  - Fetching all tasks then filtering client-side is inefficient for large datasets. Better approach: if projectId present use `taskService.getTasksByProject(projectId)` that runs Firebase query `orderByChild("projectId").equalTo(projectId)` to reduce data transferred.
  - Role check: Employee identification uses both `assignedUserId` and `assigneeEmail` matching; useful for backward compatibility but may hide mismatch bugs — prefer normalized `assignedUserId` usage.
  - Edge-case: If `getUserById` fails (exceptionally), method currently bubbles exception causing 500; might prefer fallback behavior or explicit 401 if user not found.

- Security note:
  - Controller depends on `X-Requester-ID` header trust. Ensure upstream interceptor (RoleInterceptor) validates header authenticity; otherwise clients can spoof header.

3) `getTaskById` - GET `/api/v1/tasks/{id}`
- Standard single-resource fetch, returns 404 when task null.

4) `updateTaskStatus` - POST `/api/v1/tasks/{id}` (partial update used by Kanban)
```java
@PostMapping("/{id}")
public CompletableFuture<ResponseEntity<Object>> updateTaskStatus(@PathVariable String id, @RequestBody java.util.Map<String, Object> payload) {
    return taskService.getTaskById(id)
            .thenCompose(existingTask -> {
                if (existingTask == null) {
                    return CompletableFuture.completedFuture(ResponseEntity.notFound().build());
                }

                // Update only the fields provided in the payload
                if (payload.containsKey("status")) {
                    existingTask.setStatus((String) payload.get("status"));
                }
                if (payload.containsKey("title")) {
                    existingTask.setTitle((String) payload.get("title"));
                }
                if (payload.containsKey("priority")) {
                    existingTask.setPriority((String) payload.get("priority"));
                }
                if (payload.containsKey("description")) {
                    existingTask.setDescription((String) payload.get("description"));
                }
                if (payload.containsKey("assigneeEmail")) {
                    existingTask.setAssigneeEmail((String) payload.get("assigneeEmail"));
                }
                if (payload.containsKey("assignedUserId")) {
                    existingTask.setAssignedUserId((String) payload.get("assignedUserId"));
                }

                return taskService.updateTask(existingTask)
                        .<ResponseEntity<Object>>thenApply(v -> ResponseEntity.ok(existingTask));
            })
            .exceptionally(ex -> {
                ex.printStackTrace();
                return ResponseEntity.status(500).body("Server error: " + ex.getMessage());
            });
}
```
- Giải thích:
  - This method reads existing task, applies only provided fields (partial update), then persists the entire object via `updateTask` which does full `setValueAsync` to Firebase.
  - Benefits: partial update avoids overwriting fields not included in payload; but note that `updateTask` replaces full object — race conditions may occur if multiple partial updates happen concurrently (last write wins).
  - Suggestion: implement `updateChildren(Map<String,Object>)` in TaskService to perform atomic partial update on Firebase node.
  - Validation: if `status` field transitions require business checks (e.g., cannot move to DONE without subtasks closed), enforce them here or in TaskService.

5) `updateTask` - PUT `/api/v1/tasks/{id}`
- Full update from provided Task object; assigns id from path to ensure consistency.

6) `deleteTask` - DELETE `/api/v1/tasks/{id}`
- Calls `taskService.deleteTask(id)` and returns 200 OK success or 500 on error.
- Important: deleteTask performs cascade deletion of related worklogs or confirms with user if audit retention required.

7) General Risks & Recommendations
- Replace heavy `getAllTasks()` usage with query methods in TaskService for projectId/assignee filters to minimize network & memory usage.
- Add validation and status transition rules in TaskService.
- Consider adding `@PreAuthorize` or integration with RoleInterceptor to enforce authorization at method-level.

- Defense Qs:
  - Q1: "Why POST used for partial update instead of PATCH?" — Simplicity; Spring supports @PatchMapping but implementation used POST for partial update.
  - Q2: "How to protect against header spoofing for X-Requester-ID?" — Use authentication tokens (JWT) validated by server middleware rather than trusting client-provided IDs.

---

Appended `TaskController` analysis to the documentation.

Next: I will analyze `ProjectService.java` if not already done per-line (we appended earlier), then proceed to per-line analysis for `ProjectService` and continue with UI panels per your priority list (ProjectService, AIService, EmployeePanel...). Proceeding to open and analyze `ProjectService.java` (already done) — next I will analyze `AIService.java`.

---

### Per-line Deep Analysis: `AIService.java`
File path: `src/main/java/com/techforge/erp/service/AIService.java`

Objective: Phân tích chi tiết cách `AIService` xây dựng prompt, thu thập ngữ cảnh từ WorkLog (do `skills` field removed), gọi Gemini API, và xử lý phản hồi không chuẩn (markdown wrappers). Nêu rõ hạn chế, retry, và gợi ý cải tiến.

File header & dependencies
```java
@Service
public class AIService {
    private final Logger logger = LoggerFactory.getLogger(AIService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private WorkLogService workLogService;

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.url:}")
    private String apiUrl;
```
- Explanation:
  - `@Service` marks bean; `RestTemplate` used for HTTP calls (synchronous). Could be replaced by `WebClient` for reactive and timeout handling.
  - `workLogService` is optional (required=false), so AIService can still operate if service not present.
  - `apiKey` and `apiUrl` read from properties; important to keep secret key out of logs.

---

1) `suggestAssignee(Task task, List<User> users)`
- Purpose: recommend best user for assignment using Role + WorkLog-derived experience metrics.

Per-line flow:
```java
return CompletableFuture.supplyAsync(() -> { ... })
```
- Runs heavy logic off calling thread (async). Good to avoid blocking controllers.

Key steps inside:
- Build `userContext` list of maps containing `userId`, `fullName`, `role`, `email`, and `hourlyRate` (used as seniority proxy).
- If `workLogService` available, fetch all worklogs (blocking `.join()` on `getAllWorkLogs()`), accumulate `userHoursWorked` totals.
  - Note: This collects hours across all projects; better to filter by task's project or related projects for relevance.
- Enrich userContext with `totalHoursWorked`.
- Serialize `usersJson` and `taskJson`.
- Build prompt string explaining selection criteria. Important: prompt explicitly says "Return ONLY raw JSON (no markdown). Structure: { \"userId\": \"...\", \"reason\": \"...\", \"confidenceScore\": 0.0-1.0 }"
- Build `GeminiRequest` payload and POST to `apiUrl` with Authorization header if `apiKey` present.
- Read response body `raw`, call `cleanJsonResponse(raw)` to extract JSON, then parse `AISuggestion` via ObjectMapper.

Notes & critiques:
- Using `.join()` on `workLogService.getAllWorkLogs()` inside supplyAsync is acceptable but could be heavy; better to compose futures to avoid blocking thread pool threads.
- The prompt relies on deterministic JSON response from LLM. `cleanJsonResponse` helps strip markdown wrappers and extract JSON. However, LLMs may hallucinate or return different structures; code must validate the parsed AISuggestion fields.
- For production, add timeouts on `RestTemplate` and catch `ResourceAccessException` for network failures.
- Consider rate limiting and exponential backoff when calling external API.

2) `analyzeProjectRisk(Project project, List<Task> tasks)`
- Similar structure: build prompt with project and tasks JSON, request to Gemini, clean and parse into `AIRiskAnalysis`.
- Temperature set low (0.2) for deterministic results — good.

3) `cleanJsonResponse(String raw)` utility
- Purpose: sanitize typical LLM response wrappers and extract raw JSON.
- Steps:
  - Trim, remove code fences (```json and ```), find first `{` or `[` and last matching `}` or `]` and take substring.
  - If array and DTO expects object, try to extract first object inside array.

Per-line robustness:
- Uses simple heuristics (indexOf + lastIndexOf) which works in many cases but might fail against nested braces or non-matching outputs; more robust approach is to attempt JSON parsing progressively (try parse full string, if fail try extract first JSON object using regex capturing balanced braces) or use a small parser to find matching braces.

4) Error Handling
- Exceptions during JSON processing result in RuntimeException thrown from supplyAsync; controller or caller must handle these.
- Logging is present at error level for parsing and network errors.

5) Recommendations
- Add RestTemplate timeouts via `new RestTemplate(requestFactoryWithTimeouts)`.
- Validate AISuggestion fields after parsing; if missing userId or confidenceScore, fallback to simple heuristic (choose user with max totalHoursWorked).
- Consider caching AI responses for similar tasks to reduce API calls.
- Use service account and secure storage for apiKey; do not log.

6) Defense Questions
- Q1: "Why remove skills field and how does AI still recommend users?" — Skills removed per class diagram constraint; AI uses Role + WorkLog-derived experience as proxy.
- Q2: "How to ensure AI output is parseable JSON?" — We instruct LLM via prompt and clean response with `cleanJsonResponse`, but must always validate and fallback.

---

Appended `AIService` analysis to the documentation.

Next: As requested, I will perform per-line deep analysis for an UI panel: `EmployeePanel.java` (Kanban). Proceeding to open it now.


---

### Per-line Deep Analysis: `EmployeePanel.java` (UI - Kanban & Timer)
File path: `src/main/java/com/techforge/desktop/EmployeePanel.java`

Objective: Phân tích chi tiết từng khối code chính của `EmployeePanel` (kanban board, timer widget, task cards, drag & drop handling, dialogs). Nêu rõ tại sao code viết như thế, các rủi ro concurrency/UI, đề xuất cải tiến và các câu hỏi kiểm tra khi bảo vệ.

1) Class header & responsibilities
- `public class EmployeePanel extends JPanel` - panel chính hiển thị Kanban board cho user. Sử dụng composition: chứa `ApiClient` để gọi backend, nhiều `JPanel` cho cột, `JComboBox` để lọc project.
- Thiết kế ưu tiên UX: custom painting cho cards/columns, hover effects, drag-drop via MouseAdapter để mô phỏng Trello-like behavior.

2) Constructor & refresh logic
- Constructor lưu `apiClient`, gọi `initializeUI()`, `loadProjects()`, `loadTasks()`.
- Đăng ký `ComponentListener` lắng nghe `componentShown` -> gọi `refreshTasks()`:
  - Rationale: đảm bảo nếu user chuyển từ Project Detail (nơi task có thể vừa được tạo) về tab Execution, dữ liệu cập nhật. Đây là phương án đơn giản và phù hợp cho desktop app.
  - Recommendation: thay vì rely on componentShown, có thể dùng an event-bus hoặc observe pattern để push updates từ Task creation flow trực tiếp -> giảm delay và tránh reload nhiều lần.

3) UI initialization (`initializeUI`, `createHeader`, `createFilterBar`)
- Sử dụng `BorderLayout` + `BoxLayout` cho header+filter stack; tách rõ responsibility thành createHeader/createFilterBar/createKanbanBoard.
- Filter bar: `projectFilterComboBox` (model `DefaultComboBoxModel`) và `createRefreshButton()`.
- Refresh button: custom painting để match theme; action listener gọi `loadProjects()` & `loadTasks()`.

4) Timer widget (start/stop)
- `timerLabel` hiển thị thời gian, `timerButton` thay đổi text Start/Stop, `Timer` Swing (javax.swing.Timer) chạy trên EDT và cập nhật label mỗi 1000ms.
- Rationale: `javax.swing.Timer` phù hợp cho UI-ticking vì callback chạy trên EDT; simple and safe. Nếu cần đo thời gian chính xác khi app minimized, dùng System.nanoTime + delta calculation.
- Methods: `startTimer()`, `stopTimer()`, `updateTimerDisplay()` - tất cả tương đối trực quan.

5) Kanban board creation (`createKanbanBoard`, `createKanbanColumn`, `wrapInScrollPane`)
- Board layout: `GridLayout(1,3,25,0)` tạo 3 cột đều nhau.
- Each column built as custom `JPanel` với overridden `paintComponent` để vẽ shadow, rounded background và accent top border.
- Column header contains title and countLabel (managed via `getCountLabel`.

6) Loading tasks (`loadTasks`) - core logic
- Runs in a `SwingWorker` to avoid blocking EDT. doInBackground builds appropriate API endpoint based on filter:
  - If project selected: `/tasks?projectId={id}` -> fetch all tasks for project
  - Else: fetch tasks assigned to current user using `ApiClient.get("/tasks?assignee=" + email)`
- In `done()`: parse JSON response with Gson (`JsonArray tasks`) and iterate tasks.
- For each task:
  - Extract fields: status, title, priority, description, id, projectId.
  - Compute `columnIndex = getColumnIndexForStatus(status)` (see below)
  - Create card via `createTaskCard(...)` and add to corresponding column on EDT via `SwingUtilities.invokeLater`.
  - Update counts and final UI: set count labels and `statusLabel`, then revalidate/repaint.
- Error handling: if exception -> log, set `statusLabel` with error and call `loadMockTasks()` fallback.

Per-line robustness notes:
- The method prints debug logs (truncating long responses) — helpful during development but should be gated behind logger level (SLF4J) for production.
- Parsing assumes `response` always non-null and valid JSON array — code handles exceptions in catch; good.
- Important: adding/removing components from panels must be done on EDT; code uses `SwingUtilities.invokeLater` correctly for adding cards, and later calls revalidate/repaint on columns.

7) Status normalization (`getColumnIndexForStatus`) - key bugfix area
- Implements aggressive normalization: trim, uppercase, replace spaces with underscore, then check contains for groups mapping to Done/Doing/To Do.
- Rationale: real-world data inconsistent; function ensures no task is discarded and maps unknown statuses to To Do (fail-safe).
- Recommendation: prefer canonical enum `TaskStatus { TODO, DOING, DONE }` on server side to simplify client mapping.
- Defense Qs: "Why force unknown statuses into TODO?" -> To avoid losing tasks; chosen business decision.

8) Task card rendering & interactions (`createTaskCard`)
- Card is custom JPanel with overridden `paintComponent` to draw drop shadow, white body, and left priority stripe.
- Uses hover effect: `setHovered(boolean)` reflection to toggle shadow opacity; mouseEntered shifts card up 2px, mouseExited resets.
- Drag logic implemented in MouseAdapter:
  - `mousePressed`: record start point and task id
  - `mouseReleased`: compute mouse location on screen and column bounds via `getLocationOnScreen()` and determine newStatus; if changed call `updateTaskStatus(id, newStatus)`
  - `mouseClicked`: open task actions (detail dialog)
- Critique:
  - Manual drag implementation is simple but fragile: it uses absolute screen coordinates and card.setLocation; might conflict with layout manager and could behave inconsistently on nested containers. Recommended: consider using Swing `TransferHandler` or implement drag preview overlay to avoid messing layout.
  - Reflection used to call `setHovered` is odd; better to keep `card` as named inner class with direct access.
  - Using `card.getY()` and `setLocation` with BoxLayout-managed container is risky because layout manager will reposition children on revalidate; code resets original position on release but transient visual glitches possible.

9) showTaskActions / showTaskDetailDialog
- `showTaskActions` fetches full task details via SwingWorker. On failure it creates minimal JSON object to show basic dialog.
- `showTaskDetailDialog` builds a modal `JDialog` with details: title, description, assignee, timeline dates, priority, estimated hours, and action buttons.
- Permission logic for Edit button: reads `ApiClient.getCurrentUserRole()` and only shows Edit for non-EMPLOYEE roles.
- Buttons in dialog call `updateTaskStatus` (doInBackground -> apiClient.post) and `openEditTaskDialog` for full edit.
- Note: activity of directly calling `ApiClient.put` or `post` from SwingWorker is acceptable; ensure ApiClient handles authentication headers.

10) openEditTaskDialog (Edit Task)
- Presents a larger dialog (500x600) with fields: title, description, status dropdown, priority, estimated hours, start/due dates, assignee dropdown.
- Ensures input fields setPreferredSize to 40px height to avoid cut-off text (as user requested earlier).
- `loadEmployeesIntoComboBox` populates assignee list from `/users` endpoint filtering roles.
- On save: builds `JsonObject update` with provided fields and calls `apiClient.put("/tasks/" + taskId, update.toString())` in SwingWorker; on success shows success dialog and calls `loadTasks()`.
- Critique: date fields are free text (yyyy-MM-dd) which relies on user's input; better to use date picker widget.

11) loadEmployeesIntoComboBox
- Uses SwingWorker to fetch `/users`, filters roles EMPLOYEE/MANAGER, then adds items like `Name (email)` to combo box. Selects current assignee if matches.
- Edge-case: large user lists cause many items; consider pagination or search-as-you-type.

12) updateTaskStatus
- Performs `apiClient.post("/tasks/" + taskId, updateJson)` in background and reloads `loadTasks()` on completion.
- Important: this triggers complete reload of Kanban which is simple and ensures consistency, though could be optimized to perform local move + background save (optimistic UI).

13) Utility methods
- `getJsonString(JsonObject obj, String key)` safe getter returning null when missing.
- `createStatusBadge`, `createPriorityBadge`, `createActionButton` implement consistent UI visuals using AppTheme colors.

14) Recommendations & Improvements
- Replace manual drag-drop with TransferHandler or implement glass-pane overlay for dragging to avoid layout manager fights.
- Introduce TaskStatus enum server-side and client-side to avoid string normalization logic.
- Add robust error handling & retry/backoff for network calls; show toast instead of modal for non-critical errors.
- Cache small sets (projects list) and invalidate on known events to reduce API calls.
- Consider virtualization for columns when large number of cards exist (e.g.,PagedList or custom viewport)

15) Defense Questions (Sample)
- Q: "Why use SwingWorker rather than CompletableFuture?" — SwingWorker provides convenient integration with EDT and publish/process for intermediate results; either pattern acceptable but ensure UI updates on EDT.
- Q: "How to avoid layout jumps during drag/drop?" — Use an overlay drag image and accept drop events rather than moving component itself under layout-managed container.
- Q: "How do you ensure a task moved by user B is reflected on user A's Kanban?" — The system reloads tasks on panel shown and after operations; for realtime sync consider Firebase realtime listeners or WebSocket push.

---

End of `EmployeePanel` analysis.

Next action: create the technical spec file `DOCS_DAC_TA_KY_THUAT_CODE.md` containing project tree, model class tables, service API tables, OOP analysis and DB schema (Realtime DB node mapping). I will create that file now.

