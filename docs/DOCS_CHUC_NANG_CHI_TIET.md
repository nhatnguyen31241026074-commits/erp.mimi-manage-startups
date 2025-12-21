# Tài liệu chức năng chi tiết — TechForge ERP (DOCS_CHUC_NANG_CHI_TIET.md)

**Ghi chú:** Tài liệu này được tạo bằng cách quét mã nguồn trong `src/main/java`. Nó chỉ mô tả hành vi và các endpoint, UI thực tế có trong mã; những phần chưa được code sẽ được ghi chú là `[CHƯA IMPLEMENT TRONG CODE]`.

---

## Mục Lục
1. Phân tích User Stories (các endpoint & hành động)  
2. Danh sách Màn hình & UI Components  
3. Logic Phân quyền thực tế (RBAC)  
4. Test Cases & Edge Cases  
5. Definition of Done (DoD) & UAT Checklist

---

## 1) PHÂN TÍCH USER STORIES (Endpoints & Actions)

> Dưới đây các API được nhóm theo chức năng để dễ theo dõi.

### A. Authentication (Auth)

| ID | Role | Controller | Hành động (API) | Mục đích | Tiêu chí chấp nhận |
|----|------|------------|-----------------|----------|---------------------|
| AUTH-001 | Visitor | `AuthController` | `POST /api/v1/auth/register` | Đăng ký tài khoản mới | Trả về 201 và thông tin user; role phân theo Secret Code; kiểm tra email chưa tồn tại |
| AUTH-002 | Visitor | `AuthController` | `POST /api/v1/auth/login` | Đăng nhập | Trả về user object khi credentials đúng; 401 khi sai |
| AUTH-003 | Authenticated User | `AuthController` | `PUT /api/v1/auth/profile` | Cập nhật profile người dùng | Trả về user đã cập nhật; callback UI cập nhật sidebar |
| AUTH-004 | Authenticated User | `AuthController` | `POST /api/v1/auth/change-password` | Đổi mật khẩu | Kiểm tra oldPassword đúng, lưu newPassword, trả về success |
| AUTH-005 | Visitor | `AuthController` | `POST /api/v1/auth/forgot-password` | Gửi OTP qua email | Tạo OTP + expiry, gọi EmailService.sendOtpEmail(), trả về message chung |
| AUTH-006 | Visitor | `AuthController` | `POST /api/v1/auth/reset-password` | Reset mật khẩu bằng OTP | Kiểm tra OTP hợp lệ, cập nhật mật khẩu, trả về success |

---

### B. User Management

| ID | Role | Controller | Hành động (API) | Mục đích | Tiêu chí chấp nhận |
|----|------|------------|-----------------|----------|---------------------|
| USER-001 | Admin | `UserController` | `GET /api/v1/users` | Lấy danh sách users | Trả về list users (JSON) |
| USER-002 | Admin | `UserController` | `POST /api/v1/users` | Tạo user (seed/admin) | Tạo user trong Firebase, trả về created user |

---

### C. Projects

| ID | Role | Controller | Hành động (API) | Mục đích | Tiêu chí chấp nhận |
|----|------|------------|-----------------|----------|---------------------|
| PROJ-001 | Manager/Admin | `ProjectController` | `POST /api/v1/projects` | Tạo project mới | Project lưu thành công, trả về project id |
| PROJ-002 | Any | `ProjectController` | `GET /api/v1/projects` | Lấy tất cả projects | Trả về danh sách projects |
| PROJ-003 | Any | `ProjectController` | `GET /api/v1/projects/{id}` | Lấy chi tiết project | Trả về project object hoặc 404 |
| PROJ-004 | Manager/Admin | `ProjectController` | `PUT /api/v1/projects/{id}` | Cập nhật project | Lưu fields truyền lên, trả về success |
| PROJ-005 | Manager/Admin | `ProjectController` | `DELETE /api/v1/projects/{id}` | Xóa project (cascade tasks) | Xóa project, trả về success; cascade xóa tasks |

---

### D. Tasks

| ID | Role | Controller | Hành động (API) | Mục đích | Tiêu chí chấp nhận |
|----|------|------------|-----------------|----------|---------------------|
| TASK-001 | Any (Authenticated) | `TaskController` | `POST /api/v1/tasks` | Tạo task | Trả về task lưu thành công |
| TASK-002 | Any | `TaskController` | `GET /api/v1/tasks[?assignee=&projectId=]` | Lấy tasks (lọc) | Hỗ trợ lọc theo projectId hoặc assignee; Manager/Admin có thể lấy nhiều hơn |
| TASK-003 | Any | `TaskController` | `GET /api/v1/tasks/{id}` | Lấy chi tiết task | Trả về task object |
| TASK-004 | Employee/Manager/Admin | `TaskController` | `POST /api/v1/tasks/{id}` | Cập nhật partial (dùng cho drag/drop) | Cho phép cập nhật status; endpoint được gọi bởi EmployeePanel.updateTaskStatus |
| TASK-005 | Manager/Admin | `TaskController` | `PUT /api/v1/tasks/{id}` | Cập nhật đầy đủ task | Ghi đè các trường, trả về success |
| TASK-006 | Manager/Admin | `TaskController` | `DELETE /api/v1/tasks/{id}` | Xóa task | Xóa task và trả về success |

---

### E. WorkLogs

| ID | Role | Controller | Hành động (API) | Mục đích | Tiêu chí chấp nhận |
|----|------|------------|-----------------|----------|---------------------|
| WL-001 | Employee | `WorkLogController` | `POST /api/v1/worklogs` | Ghi worklog | Tạo worklog mới, trả về worklog |
| WL-002 | Any | `WorkLogController` | `GET /api/v1/worklogs` | Lấy worklogs | Trả về danh sách worklogs |
| WL-003 | Any | `WorkLogController` | `GET /api/v1/worklogs/{id}` | Lấy worklog chi tiết | Trả về worklog |

---

### F. Reports

| ID | Role | Controller | Hành động (API) | Mục đích | Tiêu chí chấp nhận |
|----|------|------------|-----------------|----------|---------------------|
| RPT-001 | Client/Manager | `ReportController` | `GET /api/v1/reports/activities` | Recent activities | Trả về list activities hoặc fallback mock |
| RPT-002 | Client/Manager | `ReportController` | `GET /api/v1/reports/project/{projectId}` | Project Report | Trả về ProjectReport JSON |
| RPT-003 | Client/Manager | `ReportController` | `GET /api/v1/reports/project/{projectId}/progress` | Progress + Risk | Trả về ProgressReport |
| RPT-004 | Admin/Finance | `ReportController` | `GET /api/v1/reports/monthly?month=&year=` | Monthly report | Trả về MonthlyReport |

---

### G. Payments

| ID | Role | Controller | Hành động (API) | Mục đích | Tiêu chí chấp nhận |
|----|------|------------|-----------------|----------|---------------------|
| PAY-001 | Frontend (client) | `PaymentController` | `POST /api/v1/payment/pay-invoice/{invoiceId}` | Tạo URL thanh toán MoMo | Gọi MomoService.createPaymentUrl(invoice) và trả về map kết quả |

---

### H. Finance (Payroll / Invoices / Expenses)

| ID | Role | Controller | Hành động (API) | Mục đích | Tiêu chí chấp nhận |
|----|------|------------|-----------------|----------|---------------------|
| FIN-001 | Finance/Admin | `FinanceController` | `POST /api/v1/finance/payroll/calculate?userId=&month=&year=` | Tính payroll user | Gọi FinanceService.calculatePayroll và trả về Payroll |
| FIN-002 | Finance/Admin | `FinanceController` | `GET /api/v1/finance/payroll` | Lấy danh sách payroll | Gọi FinanceService.getAllPayroll và trả về list |
| FIN-003 | Finance | `FinanceController` | `POST /api/v1/finance/pay` | Xử lý mock MoMo callback | Nhận transaction, mark payrolls as paid |
| FIN-004 | Finance | `FinanceController` | `POST /api/v1/finance/invoices` | Tạo invoice | Gọi financeService.createInvoice |
| FIN-005 | Finance | `FinanceController` | `POST /api/v1/finance/expenses` | Tạo expense | Gọi financeService.createExpense |

---

### I. AI Services

| ID | Role | Controller | Hành động (API) | Mục đích | Tiêu chí chấp nhận |
|----|------|------------|-----------------|----------|---------------------|
| AI-001 | Manager | `AIController` | `POST /api/v1/ai/suggest` | Gợi ý assignee (AI) | Body: {task, users}; trả về AISuggestion |
| AI-002 | Manager | `AIController` | `POST /api/v1/ai/risk` | Phân tích rủi ro | Body: {project, tasks}; trả về AIRiskAnalysis |

---

> **Ghi chú:** Các user story trên là phản ánh trực tiếp các route/controllers có trong project; UI Swing gọi chúng qua `ApiClient`.

## 2) DANH SÁCH MÀN HÌNH & UI COMPONENTS

> Tất cả class UI quét từ `com.techforge.desktop` (các JPanel/JFrame/JDialog). Các component chính được liệt kê để dễ cân nhắc test/UX.

| Tên Class UI | Business Name | Chức năng chính | Các nút / Component chính |
|---------------|---------------|------------------|---------------------------|
| `LoginFrame` | Đăng nhập | Đăng nhập người dùng | `usernameField`, `passwordField`, `loginButton`, `Forgot Password?`, `Sign Up` |
| `RegisterFrame` | Đăng ký | Đăng ký tài khoản (có Secret Code) | Fields: FullName, Email, Password, Confirm, SecretCode, T&C checkbox, `Register` |
| `MainDashboardFrame` | Dashboard chính | Sidebar role-based, switch views | Sidebar buttons (Project, Execution, Payroll, Monitoring), profile area, logout |
| `ManagerPanel` | Project Planning | Quản lý dự án, stat cards, project table | `+ New Project`, `Export Report`, Project JTable (Manage/Edit/Delete) |
| `ProjectDialog` | Create Project modal | Form tạo project + AI generate | Fields project + `✨ Generate Tasks`, `Save` |
| `ProjectDetailDialog` | Project Tasks / Backlog | Hiện tasks của project, + New Task | Task list, `+ New Task` (mở AssignTaskDialog) |
| `AssignTaskDialog` | Assign / Create Task | Tạo/gán task, AI scouter | `Scan Power Levels`, Assignee dropdown, Status dropdown, Create button |
| `EmployeePanel` | Execution / Kanban | Kanban board, timer | Kanban columns (To Do / Doing / Done), timer, project filter, refresh |
| `AdminPanel` | Payroll & Finance | Payroll sheet, MoMo integration | `Pay via MoMo`, payroll table, `Export CSV`, `Refresh` |
| `ClientPanel` | Project Monitoring | Charts (donut/bar), activity feed | Charts container, `Download Report`, activity feed |
| `UserProfileDialog` | Profile (modal) | Xem & sửa profile, update sidebar | Tabs: Profile / Security, Save button (callback) |
| `ForgotPasswordDialog` | Quên mật khẩu | OTP flow & reset | Email input, OTP, NewPassword, Confirm Reset button |
| `MomoPaymentDialog` (techforge.ui) | MoMo Payment modal | Hiển thị QR & payUrl, xác nhận | QR image, amount, `Open Web`, `I Have Paid` (callback) |
| `PayrollPanel` | Payroll (used by Admin) | Bảng payroll nhỏ | Table, Refresh, Pay button (may call Momo dialog) |
| `UIUtils` | UI helpers | Tạo các button/badge/avatar | `createRoundedButton`, `createOutlineButton`, `createCircleImage` |
| `ReportUtils` | Export / Report helper | Hiển thị export dialog | `showExportDialog()` |


## 3) LOGIC PHÂN QUYỀN THỰC TẾ (RBAC)

> RBAC được enforce tại `RoleInterceptor` (package `com.techforge.erp.config`). Tóm tắt ma trận theo code:

| Path | Allowed Roles / Methods | Notes |
|------|------------------------|-------|
| `/api/v1/finance/**` | `ADMIN`, `FINANCE` full access; `MANAGER` only `GET` | Manager can view but not mutate |
| `/api/v1/ai/**` | `ADMIN`, `MANAGER`, `EMPLOYEE` | Finance & Client blocked |
| `/api/v1/users/**` | `ADMIN` only | Full user management reserved for Admin |
| `/api/v1/projects/**` | `GET` everyone; `POST/PUT/DELETE`: `ADMIN`, `MANAGER` | Clients can read projects but not modify |
| `/api/v1/tasks/**` | `GET` all authenticated; `POST/PUT/PATCH`: `ADMIN`, `MANAGER`, `EMPLOYEE`; `DELETE`: `ADMIN`, `MANAGER` | Enables Kanban drag/drop for EMPLOYEE |
| `/api/v1/worklogs/**` | `ADMIN`, `MANAGER`, `EMPLOYEE` | Create/read worklogs allowed |

**Implementation notes:**
- `RoleInterceptor` fetches user with 5s timeout to avoid deadlocks. Missing header => 401. User not found => 401.
- `User.hasRole(...)` helper used for checks (case-insensitive).


## 4) TEST CASES & EDGE CASES

> Dưới đây là bảng các Test Cases ưu tiên (Happy path & Edge). Mỗi case bao gồm các bước thực hiện, dữ liệu test và kết quả mong đợi.

| ID | Function | Steps | Test data | Expected result |
|----|----------|-------|-----------|-----------------|
| TC-001 | Register (SecretCode->EMPLOYEE) | Open `RegisterFrame` -> fill fields -> SecretCode=`KAME_HOUSE` -> Register | valid email not used | Created user with role `EMPLOYEE`; success message |
| TC-002 | Register (SecretCode->ADMIN) | SecretCode=`CAPSULE_CORP` | valid data | Created user with role `ADMIN` |
| TC-003 | Login success | Use seeded user (e.g., `bulma@capsule.corp`) | correct password | Login success -> MainDashboardFrame shown |
| TC-004 | Login fail | wrong password | invalid password | 401 displayed; no navigation |
| TC-005 | Create Project (Manager) | Manager -> + New Project -> Save | valid project data | Project appears in ManagerPanel table |
| TC-006 | Delete Project cascade | Manager -> Delete Confirm | select project with tasks | Project and tasks removed; stats refreshed |
| TC-007 | Create Task & Assign | ProjectDetail -> + New Task -> Assign to employee | valid task | Task saved and visible in backlog and Kanban after refresh |
| TC-008 | Kanban drag-drop | Employee: drag card to Done | existing task | POST /tasks/{id} called with status=DONE; UI moves card |
| TC-009 | Payroll refresh after Firebase edit | Edit `hourlyRateOT` in Firebase console -> Admin click Refresh | changed hourlyRateOT | AdminPanel shows updated Hours/Total; toast "Synced!" |
| TC-010 | MoMo batch payment | Admin select Pay -> confirm -> Momo dialog -> I Have Paid | payroll with PENDING >0 | Backend called /finance/pay; table rows set PAID; transaction record added |
| TC-011 | Profile update & sidebar sync | Edit fullName in UserProfileDialog -> Save | new fullName | Sidebar updated immediately via callback |
| TC-012 | Forgot Password wrong OTP | Request OTP -> enter wrong OTP | wrong code | Show exact error message `Lỗi OTP tồi BẠN ỚI!` and do not reset password |

**Edge Cases:**
- EC-001: Missing `X-Requester-ID` header -> API returns 401 (RoleInterceptor).  
- EC-002: Firebase timeout when fetching user -> RoleInterceptor returns 503.  
- EC-003: `hourlyRateOT` null/empty in Firebase -> code treats as 0.0 (UserService.getDoubleValue returns 0.0), payroll shows NO_WORK.  
- EC-004: Rapid Refresh spamming -> UI disables Refresh during operation (AdminPanel).  
- EC-005: API or Firebase down -> UI falls back to mock data in many panels (loadMock* methods).  


## 5) DEFINITION OF DONE (DoD) & UAT CHECKLIST

**Definition of Done (Đối với mỗi feature):**
- Code compiles without warnings (mvn clean package).  
- Happy path flows pass (manual UAT).  
- Exceptions và lỗi mạng được bắt và hiển thị thông báo thân thiện.  
- RBAC enforced ở backend (RoleInterceptor) và UI ẩn/hiện các chức năng theo role.  
- Mỗi thay đổi UI/Service có unit/integration test (nếu có khả năng) hoặc ít nhất 1 manual test case.

**UAT Checklist (high-level):**
- [ ] Đăng ký/đăng nhập cho vegeta/goku/bulma/frieza hoạt động.  
- [ ] Manager tạo dự án, gán task, employee nhận được task.  
- [ ] Employee drag-drop cập nhật status tại backend.  
- [ ] Admin refresh payroll phản ánh hourlyRateOT mới.  
- [ ] MoMo dialog mở và xác nhận, bảng payroll được cập nhật PAID.

---

**Tiếp theo:** nếu bạn đồng ý tôi sẽ tiếp tục tạo file `DOCS_DAC_TA_KY_THUAT_CODE.md` (mô tả cấu trúc project, model, services, API nội bộ, OOP analysis, schema) — gõ **"Tiếp tục"** để tôi bắt đầu.
