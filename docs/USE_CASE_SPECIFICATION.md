# Use Case Specification — TechForge ERP

**Người soạn:** Senior Business Analyst

**Ngữ cảnh:** TechForge (ERP)
- Stack: Java Swing (Desktop) + Spring Boot (API) + Firebase Realtime Database
- Modules chính: Project Management, Task Management, User Auth, Finance

**Mục tiêu tài liệu:** Mô tả chi tiết các Use Case quan trọng, tập trung vào Luật nghiệp vụ (Business Logic) và Kiểm tra/Hợp lệ hệ thống (System Validation). Dùng làm tài liệu tham chiếu cho dev và QA.

---

## PART 1 — ACTORS

- **Admin**
  - Quản lý toàn hệ thống: quản lý user, cấu hình, seed data, phân quyền.
  - Có quyền thực hiện mọi hành động: tạo/sửa/xóa users, projects, tasks, invoices.

- **Project Manager (Manager / PM)**
  - Tạo và quản lý Project, gán Client, phân công Tasks, phê duyệt ngân sách.

- **Team Member (Employee)**
  - Thực hiện task, cập nhật trạng thái (Kanban), ghi WorkLog, báo lỗi/time tracking.

- **Client**
  - Xem Project được gán, nhận thông báo khi có project mới hoặc cập nhật; không có quyền chỉnh sửa project.

- **Finance / Accountant**
  - Quản lý payroll, invoice, expenses; thực hiện/ghi nhận thanh toán; xuất báo cáo tài chính.

- **System (Backend)**
  - Spring Boot API + RoleInterceptor; thực thi business logic, xác thực RBAC, persist vào Firebase.

- **ApiClient (Desktop)**
  - OkHttp wrapper giữ session và header `X-Requester-ID`, log cho debugging, gọi API từ giao diện Swing.

- **External Payment Gateway (MoMo)**
  - Cổng thanh toán cho chức năng thanh toán invoice/payroll.

---

## PART 2 — USE CASE LIST (BẢNG)

| ID | Module | Use Case Name | Primary Actor | Description |
|---:|---|---|---|---|
| UC-001 | Projects | Tạo Project & Gán Client | Manager | Manager tạo project mới, chọn client từ danh sách, lưu clientId vào project. |
| UC-002 | Projects | Cập nhật Project | Manager | Chỉnh sửa thông tin project (status, budget, dates, clientId). |
| UC-003 | Tasks | Tạo & Gán Task | Manager/PM | Tạo task, gán cho employee, set priority, estimated hours. |
| UC-004 | Tasks | Cập nhật trạng thái Task (Kanban) | Employee | Kéo thả task giữa cột (ToDo/Doing/Done), backend cập nhật status và tính tiến độ. |
| UC-005 | Auth/Notifications | Đăng nhập & Thông báo | All | Sau login, hệ thống thông báo task mới cho employee, project assigned cho client. |
| UC-006 | Finance | Tính Payroll | Finance | Tính payroll dựa trên WorkLogs và kỹ luật salaryType, lưu payroll record (paid=false). |
| UC-007 | Finance | Thanh toán & Lịch sử giao dịch | Finance | Kết nối MoMo, xử lý callback, mark payroll as PAID, lưu transaction history. |
| UC-008 | Reports | Sinh báo cáo project | Manager/Client | Tạo báo cáo project: progress, risks, activity feed. |
| UC-009 | Notifications | Notification Center | All | Trung tâm thông báo (hiển thị, mark-as-read); future: realtime push via Firebase. |
| UC-010 | Budget | Kiểm soát Ngân sách (Budget Control) | Manager/Finance | Validate budget khi tạo/ghi expense; cảnh báo khi vượt thresholds. |
| UC-011 | Tasks | Task Dependencies | Manager | Thiết lập phụ thuộc giữa tasks (blocking), hệ thống prevents start if dependencies open. |

---

## PART 3 — DETAILED SPECIFICATIONS (CORE)

> Chọn 6 Use Cases quan trọng: UC-001, UC-003, UC-004, UC-005, UC-007, UC-010.

---

### Use Case: Tạo Project & Gán Client (UC-001)

1. **Pre-conditions:**
   - User đã đăng nhập và có role = MANAGER hoặc ADMIN.
   - Backend sẵn sàng (reachable).

2. **Main Flow:**
   - Step 1: Manager mở `ProjectDialog` trong UI.
   - Step 2: System gọi `ApiClient.get("/clients")`.
   - Step 3: Nếu `/clients` trả về `[]` thì System gọi fallback `ApiClient.get("/users")` và filter `role == "CLIENT"` để hiển thị danh sách client.
   - Step 4: UI hiển thị `JComboBox<Client>` với item chứa `{id, name (fullName), email}`.
   - Step 5: Manager nhập `name`, `description`, `budget`, `startDate`, `endDate`, chọn `client` từ combobox.
   - Step 6: Khi nhấn Save -> System validate dữ liệu theo Business Rules (xem phần 4).
   - Step 7: Nếu hợp lệ -> System gửi `POST /api/v1/projects` với payload có `clientId`.
   - Step 8: Backend tạo project trong Firebase (`/LTUD10/projects/{id}`) và trả về JSON project.
   - Step 9: Frontend hiển thị thông báo thành công và (optionally) tạo default task bằng POST `/api/v1/tasks`.

3. **Alternative/Error Flows:**
   - Nếu user không có quyền: server trả 403 -> UI hiển thị "Bạn không có quyền tạo project".
   - Nếu tên project rỗng: validation fail -> hiển thị "Tên dự án là bắt buộc.".
   - Nếu budget không hợp lệ (ví dụ âm): hiển thị "Budget phải là số không âm".
   - Nếu network error: retry 2 lần, sau đó hiện "Không thể kết nối server. Vui lòng thử lại sau.".
   - Nếu server trả lỗi 500: hiện "Lỗi server. Vui lòng liên hệ quản trị viên.".

4. **Business & System Logic (IMPORTANT):**
   - *Logic 1:* `budget` phải >= 0.
   - *Logic 2:* `startDate` phải <= `endDate`.
   - *Logic 3:* `clientId` phải tồn tại (server sẽ verify) — nếu `clientId` invalid server trả 400.
   - *Logic 4:* Nếu `clientId` là giá trị placeholder (ví dụ "-1"), project vẫn cho phép tạo nhưng UI cảnh báo "Project chưa gán client" và yêu cầu xác nhận.
   - *Logic 5:* Sau khi project được tạo thì `project.status` mặc định = `PLANNING` và `createdAt`/`createdBy` được lưu (audit).

**API refs:**
- GET /api/v1/clients (fallback GET /api/v1/users)
- POST /api/v1/projects

**Validation Messages:**
- "Tên dự án là bắt buộc."; "Budget phải là số không âm."; "Ngày bắt đầu không thể lớn hơn ngày kết thúc.".

---

### Use Case: Tạo & Gán Task (UC-003)

1. **Pre-conditions:**
   - User đăng nhập là MANAGER hoặc ADMIN.
   - Project mục tiêu (`projectId`) tồn tại.

2. **Main Flow:**
   - Step 1: Manager mở `ProjectDetailDialog` và chọn "New Task".
   - Step 2: Manager nhập `title`, `description`, `estimatedHours`, `priority`, chọn `assignee` (employee) từ combobox.
   - Step 3: System validate các trường (title không rỗng, estimatedHours >= 0, assignee tồn tại và active).
   - Step 4: System gửi `POST /api/v1/tasks` với payload `{projectId, title, description, estimatedHours, priority, assignedUserId}`.
   - Step 5: Backend lưu task vào Firebase `/LTUD10/tasks/{id}` và trả về task JSON.
   - Step 6: Nếu có assignee -> Backend hoặc frontend gửi notification (hoặc ghi record) để employee thấy khi login hoặc qua notification center.

3. **Alternative/Error Flows:**
   - Nếu `title` rỗng -> show "Title không được để trống".
   - Nếu `estimatedHours` < 0 -> show "Estimated hours phải >= 0".
   - Nếu assignee inactive -> show "Không thể gán cho người dùng không hoạt động".
   - Nếu network error -> retry once, nếu fail -> suggest "Lưu nháp cục bộ?" (optional).

4. **Business & System Logic:**
   - *Logic 1:* `estimatedHours` có giới hạn hợp lý (ví dụ <= 1000); nếu vượt, thông báo xác nhận.
   - *Logic 2:* Khi task lưu, snapshot `assigneeEmail` and `assignedUserId` nên được lưu để truy vấn hiệu quả.
   - *Logic 3:* Mặc định `priority` = MEDIUM nếu không chọn.
   - *Logic 4:* Khi tạo task, cập nhật `project.updatedAt`.

**API refs:** POST /api/v1/tasks

**Validation Messages:**
- "Title không được để trống"; "Estimated hours phải >= 0".

---

### Use Case: Cập nhật trạng thái Task (Kanban) (UC-004)

1. **Pre-conditions:**
   - Employee đã đăng nhập.
   - Task tồn tại và thuộc Project đang xem.

2. **Main Flow:**
   - Step 1: Employee kéo card task từ cột A sang cột B trong Kanban UI.
   - Step 2: Frontend gọi API partial update `/api/v1/tasks/{id}` hoặc POST `/api/v1/tasks/{id}` để cập nhật `status`.
   - Step 3: Server validate chuyển trạng thái theo Transition Matrix (vd: TODO->DOING ok; TODO->DONE may require subtask checks).
   - Step 4: Nếu hợp lệ -> Server cập nhật task.status, set `updatedAt`, `updatedBy`.
   - Step 5: Nếu status == DONE -> Server tính lại `project.progress` và có thể set project.status = COMPLETED nếu tất cả task hoàn tất.
   - Step 6: Frontend hiển thị success và lock/confirm trạng thái.

3. **Alternative/Error Flows:**
   - Nếu rule violated (vd: cannot mark DONE because dependency open) -> show "Không thể chuyển sang DONE: phụ thuộc chưa hoàn thành".
   - Nếu network error: UI có thể optimistic update; nếu server reject, rollback và show error.

4. **Business & System Logic:**
   - *Logic 1:* Server-side Transition Matrix enforces valid status changes.
   - *Logic 2:* Khi task->DONE, cập nhật `project.progress` = (completedTasks / totalTasks) * 100.
   - *Logic 3:* Nếu all tasks DONE -> project.status = COMPLETED và send notification to Client/Manager.
   - *Logic 4:* Concurrency: dùng `updatedAt` hoặc CAS để tránh overwrite; server xử lý theo last-write-wins với audit.

**API refs:** POST /api/v1/tasks/{id} (partial update)

**Validation Messages:**
- "Không thể đổi trạng thái: <lý do>"; "Cập nhật thất bại. Thử lại.".

---

### Use Case: Đăng nhập & Thông báo Assigned Projects / Tasks (UC-005)

1. **Pre-conditions:**
   - User account tồn tại với credentials hợp lệ.

2. **Main Flow:**
   - Step 1: Desktop gửi `POST /api/v1/auth/login` với `{email, password}`.
   - Step 2: Backend authenticate (Firebase) trả `{userId, role, user}`; ApiClient lưu session (`setCurrentUser`) và set header `X-Requester-ID`.
   - Step 3: Frontend mở `MainDashboardFrame`.
   - Step 4: Nếu role == EMPLOYEE hoặc MANAGER -> gọi `GET /api/v1/tasks` và đếm tasks assigned to user với status TODO -> nếu count>0 show modal "Bạn có X task mới".
   - Step 5: Nếu role == CLIENT -> gọi `GET /api/v1/projects` và filter `clientId == userId` (ignore COMPLETED/CANCELLED) -> nếu count>0 show modal "Bạn có X project được gán".

3. **Alternative/Error Flows:**
   - Nếu login thất bại -> show "Đăng nhập thất bại. Kiểm tra thông tin.".
   - Nếu network error khi tải tasks/projects -> silent fail (không block login); có thể retry once.

4. **Business & System Logic:**
   - *Logic 1:* Notifications at login are snapshot-based; không tự động mark-as-read.
   - *Logic 2:* Role authoritative từ server; client không tự quyết role.
   - *Logic 3:* All subsequent API requests must include `X-Requester-ID` header; nếu header thiếu server trả 401/403.

**API refs:** POST /api/v1/auth/login; GET /api/v1/tasks; GET /api/v1/projects

**Messages:**
- "Bạn có X task mới được giao!"; "Bạn có X project đang được gán!".

---

### Use Case: Thanh toán Payroll & Lưu Lịch sử Giao dịch (UC-007)

1. **Pre-conditions:**
   - Finance/Admin đăng nhập.
   - Có payroll records `isPaid == false` hoặc invoices pending.

2. **Main Flow:**
   - Step 1: Admin chọn payroll rows và click "Pay" -> Frontend gọi service để tạo payment session (POST /api/v1/payment/pay-invoice/{invoiceId}) hoặc nội bộ Finance API.
   - Step 2: Frontend mở `MomoPaymentDialog` với payUrl/QR.
   - Step 3: Khi MoMo callback về backend `/api/v1/finance/pay` -> backend verify và mark payroll(s) as PAID, set `transactionId`.
   - Step 4: Backend viết Transaction record vào Firebase `/LTUD10/transactions/{transactionId}` (bao gồm list payroll IDs, amount, payer, timestamp, gateway response).
   - Step 5: Frontend `FinancePanel` gọi `GET /api/v1/finance/transactions` -> backend trả danh sách payrolls/tansactions đã `paid == true`, sorted newest-first.

3. **Alternative/Error Flows:**
   - Nếu payment not completed -> no change, frontend show "Thanh toán chưa hoàn tất".
   - Nếu callback signature invalid -> log, alert admin, DO NOT mark payroll as paid.
   - If duplicate callback -> use idempotency on transactionId to ignore duplicates.

4. **Business & System Logic:**
   - *Logic 1:* Chỉ mark payroll as PAID sau khi xác thực callback từ gateway.
   - *Logic 2:* Transaction record phải chứa: `transactionId`, `amount`, `relatedPayrollIds[]`, `payerId`, `gatewayResponse`, `timestamp`.
   - *Logic 3:* Lịch sử giao dịch trả về chỉ payroll/invoices `paid == true` (server-side filter).
   - *Logic 4:* Duplicate callbacks => dedupe bằng `transactionId` unique constraint.

**API refs:** POST /api/v1/payment/pay-invoice/{invoiceId}; POST /api/v1/finance/pay; GET /api/v1/finance/transactions

**Messages:**
- "Thanh toán thành công"; "Xác thực thanh toán thất bại".

---

### Use Case: Kiểm soát Ngân sách (Budget Control) (UC-010)

1. **Pre-conditions:**
   - Manager/Admin đang tạo/cập nhật project hoặc tạo expense.

2. **Main Flow:**
   - Step 1: Khi tạo/cập nhật project nhập `budget`.
   - Step 2: System validate `budget` >= 0; system tính `used = sum(expenses)`.
   - Step 3: Nếu `used` > `budget` -> reject change hoặc set expense status = `PENDING_APPROVAL`.
   - Step 4: Nếu `used/budget` > thresholds (70%, 90%) -> show warning và tạo alert (notify finance/admin).

3. **Alternative/Error Flows:**
   - Nếu budget < 0 -> show "Budget không hợp lệ".
   - If user không có quyền tăng budget beyond limit -> require admin approval workflow.

4. **Business & System Logic:**
   - *Logic 1:* Budget không được âm.
   - *Logic 2:* Khi ghi Expense, hệ thống kiểm tra `expense.amount + totalExpenses <= budget`.
   - *Logic 3:* Threshold alerts at configurable percentages (70%, 90%) — send notification to Finance and Project Manager.
   - *Logic 4:* Only ADMIN can increase budget above configured `PROJECT_MAX_LIMIT`.

**API refs:** POST/PUT /api/v1/projects; POST /api/v1/finance/expenses

**Messages:**
- "Budget không hợp lệ"; "Ngân sách không đủ, yêu cầu phê duyệt".

---

## KẾT LUẬN & GHI CHÚ KIỂM TRA

- Tất cả Validation quan trọng phải enforce server-side (không chỉ client-side).
- Mỗi hành động tác động dữ liệu (create/update/delete) phải kèm audit fields: `createdAt`, `createdBy`, `updatedAt`, `updatedBy`.
- Thanh toán cần idempotency và audit chi tiết (transaction objects).
- Notifications: hiện tại logic notify-on-login; đề xuất mở rộng Notification Center trong Firebase để realtime.
- Testing: QA cần test các luồng happy path và edge cases: permission denied, invalid input, network failure, duplicate callbacks.

---

**Hành động tiếp theo:**
- Tôi có thể thêm checklist test case chi tiết, hoặc sinh PlantUML diagrams sequence cho các Use Case chính.


