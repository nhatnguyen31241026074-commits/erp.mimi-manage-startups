# DOCS_API_VA_LOGIC_NGHIEP_VU.md

Đặc tả API nội bộ và logic nghiệp vụ cho dự án TechForge ERP
Ngày: 2025-12-21
Tác giả: Senior Java Architect

Hướng dẫn: Tài liệu này tập trung vào các lớp thuộc package `service`, `controller` và `utils`. Với mỗi phương thức quan trọng (thường là `public`) sẽ có bảng mô tả gồm: tên hàm, signature, input/output, các bước thực hiện (step-by-step), điểm nhấn kỹ thuật và cách xử lý lỗi.

---

## Hướng dẫn đọc nhanh
- Tên class được bôi đậm. Dưới mỗi class là các phương thức public cần mô tả.
- Giữ nguyên tên phương thức và chữ viết hoa/đặt tên trong dấu `code` để dễ tra cứu mã nguồn.

---

## 1. Services

### A. `UserService` (package: `com.techforge.erp.service`)

| Tên hàm (Signature) | Input | Output | Logic bước-by-step | Ghi chú kỹ thuật / Xử lý lỗi |
|---|---:|---|---|---|
| `public CompletableFuture<User> getUserById(String id)` | `id: String` | `CompletableFuture<User>` (User or null) | 1. Truy cập node `LTUD10/users/{id}` trên Firebase.
2. Thêm `ValueEventListener` (single) để lấy `DataSnapshot`.
3. Nếu tồn tại -> map snapshot sang `User.class` và `future.complete(user)`; ngược lại `future.complete(null)`.
4. OnCancelled -> `future.completeExceptionally(RuntimeException(...))`. | Đảm bảo gọi trên luồng không phải EDT (không block). Ghi log khi onCancelled. |

| `public CompletableFuture<User> getUserByEmail(String email)` | `email: String` | `CompletableFuture<User>` | 1. Tạo Query: `usersRef.orderByChild("email").equalTo(email).limitToFirst(1)`.
2. Lắng nghe single value event, lấy child đầu tiên nếu có -> map sang User.
3. Hoàn thành future hoặc null/exception. | Firebase query trả về map, cần lấy child đầu tiên; handle nhiều kết quả (take first). |

| `public CompletableFuture<List<User>> getAllUsers()` | - | `CompletableFuture<List<User>>` | 1. Đọc node `LTUD10/users` bằng `addListenerForSingleValueEvent`.
2. Duyệt `snapshot.getChildren()` và map từng child sang `User`.
3. Trả về danh sách (có thể rỗng). | Phải xử lý kiểu số (Long/Double/String) khi map các trường số; bắt try/catch để bỏ qua record hỏng và tiếp tục. |

| `public CompletableFuture<User> createUser(User user)` | `user: User` | `CompletableFuture<User>` | 1. Nếu `user.id` rỗng -> `push().getKey()` để sinh id.
2. Gán id và `setValueAsync(user)` vào Firebase.
3. Hoàn thành future khi listener gọi xong. | Không hash password ở đây (hiện trạng). Cần chú ý: production phải hash mật khẩu trước khi lưu. |

| `public CompletableFuture<Void> updateUser(User user)` | `user: User` | `CompletableFuture<Void>` | 1. Kiểm tra `user.id` khác null.
2. Ghi đè node `users/{id}` bằng `setValueAsync(user)`.
3. Hoàn thành future hoặc completeExceptionally khi lỗi. | Viết toàn bộ object -> nguy cơ lost update nếu nhiều client cập nhật đồng thời; cân nhắc `updateChildren` cho partial updates. |

| `public void forceReloadUsers(Runnable onLoaded)` | `onLoaded: Runnable` | `void` (callback) | 1. Gọi `getAllUsersFromFirebase()` trả về `CompletableFuture<List<User>>`.
2. Khi future complete, cập nhật cache nội bộ và gọi `SwingUtilities.invokeLater(onLoaded)` để cập nhật UI.
3. Nếu lỗi, log và vẫn gọi callback để UI không chờ vô hạn. | Không block UI; callback chạy trên EDT. Cần đảm bảo onLoaded xử lý trạng thái null/rỗng. |

---

### B. `ProjectService` (package: `com.techforge.erp.service`)

| Tên hàm (Signature) | Input | Output | Logic bước-by-step | Ghi chú kỹ thuật / Xử lý lỗi |
|---|---:|---|---|---|
| `public CompletableFuture<Project> createProject(Project p)` | `p: Project` | `CompletableFuture<Project>` | 1. Nếu `p.id` null -> key = `projectsRef.push().getKey()`.
2. Gán id, `projectsRef.child(key).setValueAsync(p)`.
3. Hoàn thành future với project đã lưu. | Nên validate các field (budget >=0, name not empty) trước khi ghi. |

| `public CompletableFuture<List<Project>> getAllProjects()` | - | `CompletableFuture<List<Project>>` | 1. Đọc tất cả children tại `projectsRef`.
2. Map từng snapshot sang `Project`.
3. Trả về list. | Nếu nhiều project, trả về toàn bộ có thể gây tốn bộ nhớ; cân nhắc paging. |

| `public CompletableFuture<Project> getProjectById(String id)` | `id` | `CompletableFuture<Project>` | 1. Lắng nghe single event node `projects/{id}` và map.
2. Trả `null` nếu không tồn tại. | Trả về null để controller map thành 404 nếu cần. |

| `public CompletableFuture<Void> updateProject(Project p)` | `p: Project` | `CompletableFuture<Void>` | 1. Kiểm tra `p.id`.
2. `projectsRef.child(p.id).setValueAsync(p)`.
3. On success complete. | Viết toàn bộ object; partial update cần `updateChildren` nếu muốn. |

| `public CompletableFuture<Void> deleteProject(String id)` | `id` | `CompletableFuture<Void>` | 1. Query tasks where `projectId == id` và xóa các task liên quan (cascade).
2. Xóa node `projects/{id}`.
3. Hoàn thành future. | Cascade delete làm mất dữ liệu; nếu cần audit thì nên soft-delete. |

---

### C. `TaskService` (package: `com.techforge.erp.service`)

| Tên hàm (Signature) | Input | Output | Logic bước-by-step | Note |
|---|---:|---|---|---|
| `public CompletableFuture<Task> createTask(Task t)` | `Task` | `CompletableFuture<Task>` | 1. Sinh key nếu cần, `t.id=key`.
2. `tasksRef.child(key).setValueAsync(t)`.
3. Complete future với task. | Nếu có `assigneeEmail`, có thể resolve sang `assignedUserId` trước khi lưu.

| `public CompletableFuture<List<Task>> getAllTasks()` | - | `CompletableFuture<List<Task>>` | 1. Đọc tất cả tasks.
2. Map thành List<Task>. | Hiện implementation đọc tất cả và filter client-side; với large dataset nên query server-side.

| `public CompletableFuture<Task> getTaskById(String id)` | `id` | CompletableFuture<Task> | 1. Read node `tasks/{id}` and map to Task. | |

| `public CompletableFuture<Void> updateTask(Task t)` | `Task` | CompletableFuture<Void> | 1. Write full task object to `tasks/{id}`.
2. Complete. | Overwrites full object; partial update phải merge trước.

| `public CompletableFuture<Void> deleteTask(String id)` | `id` | CompletableFuture<Void> | 1. `tasksRef.child(id).removeValueAsync()`.
2. Complete. | Should cascade delete worklogs if required.

| `public CompletableFuture<Void> updateTaskPartial(String id, Map<String,Object> patch)` | `id`, `patch` | CompletableFuture<Void> | 1. Lấy task hiện tại, merge các key trong patch vào object.
2. Gọi `setValueAsync(updatedTask)` hoặc `updateChildren(patch)`. | Better to use `updateChildren` to avoid read-modify-write race conditions.

---

### D. `WorkLogService` (package: `com.techforge.erp.service`)

| Tên hàm | Input | Output | Logic | Ghi chú |
|---|---|---|---|---|
| `public CompletableFuture<WorkLog> createWorkLog(WorkLog wl)` | `WorkLog` | `CompletableFuture<WorkLog>` | 1. Validate `wl.userId` (required).
2. Gọi `userService.getUserById(wl.userId)` để lấy user hiện tại.
3. Snapshot salary: `wl.baseSalarySnapshot = user.baseSalary; wl.hourlyRateOTSnapshot = user.hourlyRateOT`.
4. Sinh id nếu cần, `worklogsRef.child(id).setValueAsync(wl)`.
5. Complete future with created worklog. | Snapshot salary để giữ lịch sử tính lương đúng thời điểm.

| `public CompletableFuture<List<WorkLog>> getAllWorkLogs()` | - | CompletableFuture<List<WorkLog>> | 1. Đọc mọi node con dưới `worklogs`.
2. Map sang List<WorkLog>. | |

| `public CompletableFuture<WorkLog> getWorkLogById(String id)` | id | CompletableFuture<WorkLog> | Read node and map. | |

| `public CompletableFuture<Void> updateWorkLog(WorkLog wl)` | WorkLog | CompletableFuture<Void> | Write back full object. | Partial update needs merge. |

| `public CompletableFuture<Void> deleteWorkLog(String id)` | id | CompletableFuture<Void> | Remove node. | |

---

### E. `FinanceService` (package: `com.techforge.erp.service`)

| Tên hàm | Input | Output | Logic step-by-step | Ghi chú kỹ thuật |
|---|---:|---|---|---|
| `public CompletableFuture<Payroll> calculatePayroll(String userId, int month, int year)` | userId, month, year | CompletableFuture<Payroll> | 1. Lấy worklogs bằng `workLogService.getAllWorkLogs()`.
2. Lọc worklogs theo `userId` và khoảng thời gian.
3. Lấy thông tin user (`userService.getUserById(userId)`), kiểm tra `salaryType`.
4. Với mỗi worklog: xác định `hourlyRate`: nếu `salaryType == hourly` -> dùng `baseSalarySnapshot`, else `hourly = baseSalarySnapshot / 160.0`.
5. regularPay = hourly * regularHours; overtimePay = hourlyRateOTSnapshot * overtimeHours.
6. Tổng hợp và lưu Payroll (viết vào `LTUD10/payrolls/{id}`). | Assumption: 160 giờ/tháng; cần document. Xử lý nulls (treat as 0.0).

| `public CompletableFuture<List<Map<String,Object>>> getAllPayroll()` | - | CompletableFuture<List<Map<String,Object>>> | 1. Read payrolls node, map ra list of map (rendered for UI). | Returned structure is UI-friendly, not raw domain objects.

| `public CompletableFuture<Payroll> createPayrollRecord(Payroll p)` | Payroll | CompletableFuture<Payroll> | 1. Create id + write to Firebase. | |

| `public CompletableFuture<Void> markPayrollsAsPaid(List<String> payrollIds, String transactionId)` | list ids, txnId | CompletableFuture<Void> | 1. For each id, set status PAID and (if exists) set transactionId.
2. Complete when all updated. | Ensure idempotency: check if already PAID before update.

---

### F. `MomoService` (package: `com.techforge.erp.service`)

| Tên hàm | Input | Output | Logic | Ghi chú |
|---|---:|---|---|---|
| `public Map<String,Object> createPaymentUrl(Invoice invoice)` | `Invoice` | `Map<String,Object>` | 1. Chuẩn bị dữ liệu: `orderId`, `requestId`, `amount` (string/long).
2. Xây chuỗi canonical để sign theo MoMo spec.
3. Tạo signature HMAC-SHA256 với `secretKey`.
4. Gọi MoMo endpoint (RestTemplate) với payload JSON.
5. Parse response và trả về map chứa `payUrl`, `qrCodeUrl`, `amount`, `resultCode`.
| Secret key/partner keys phải lấy từ env; log không in secret. Trong demo có thể trả mock payUrl. |

---

### G. `ReportService` (package: `com.techforge.erp.service`)

| Tên hàm | Input | Output | Logic | Ghi chú |
|---|---:|---|---|---|
| `public CompletableFuture<ProjectReport> generateProjectReport(String projectId)` | projectId | CompletableFuture<ProjectReport> | 1. Lấy `Project` từ `projectService.getProjectById`.
2. Lấy tasks và worklogs liên quan (filter projectId).
3. Tính `totalTasks`, `completedTasks`, `progress` (guard divide-by-zero).
4. Tính `budgetUsed` từ worklogs (sử dụng snapshot salary) hoặc invoices/expenses.
5. Tạo `ProjectReport` DTO và trả về. | Cần tin cậy `WorkLog.baseSalarySnapshot` semantics; magic number 160 nên đặt constant.

| `public CompletableFuture<MonthlyReport> generateMonthlyReport(int month,int year)` | month,year | CompletableFuture<MonthlyReport> | 1. Lấy tất cả projects, worklogs trong tháng.
2. Tính tổng doanh thu/tổng chi/total payroll.
3. Tạo DTO MonthlyReport. | Pending: phải tích hợp invoices/expenses để tính doanh thu/chi phí.

| `public CompletableFuture<List<Map<String,Object>>> getRecentActivities(String projectId)` | projectId optional | CompletableFuture<List<Map<String,Object>>> | 1. Lấy worklogs + tasks; lọc theo projectId.
2. Tạo activity items (type, timestamp, message).
3. Sắp xếp theo timestamp giảm dần, limit N.
| Important: cần có timestamp trên task completions để merge correctly.

---

## 2. Controllers

> Ghi chú: Controller thường trả `CompletableFuture<ResponseEntity<?>>` để Spring xử lý async non-blocking. Mỗi Endpoint nêu dưới đây chỉ mô tả các hành vi chính.

### A. `AuthController` (package: `com.techforge.erp.controller`)

| Endpoint | Method | Input | Output | Logic summary | Error handling |
|---|---|---|---|---|---|
| `/api/v1/auth/register` | POST | `RegisterRequest` (fullName,email,password,secretCode) | 200/201 + user | 1. Validate input.
2. Determine role từ `secretCode` mapping.
3. Call `userService.createUser`.
| Return 400 nếu invalid; 500 on server errors. |

| `/api/v1/auth/login` | POST | `{email,password}` | 200 + user + token (in demo) | 1. `userService.getUserByEmail(email)`.
2. Compare password (plaintext in current code) -> security risk.
3. Return user DTO. | Return 401 for invalid creds. |

| `/api/v1/auth/forgot-password` | POST | `{email}` | 200 OK or error | 1. Generate OTP, store on user (otp + expiry).
2. `emailService.sendOtpEmail(email, otp)`.
| Rate-limit, avoid leaking which emails exist. |

| `/api/v1/auth/reset-password` | POST | `{email,otp,newPassword}` | 200 OK or 400 | 1. Validate otp and expiry; if ok update password.
| Store hashed password in production. |

### B. `UserController` (package: `com.techforge.erp.controller`)

| Endpoint | Method | Input | Output | Logic |
|---|---|---|---|---|
| `/api/v1/users` | GET | (optional filters) | list users | Calls `userService.getAllUsers()` and returns.
| `/api/v1/users` | POST | `User` | created User | Calls `userService.createUser` (admin/seed use).
| `/api/v1/users/{id}` | PUT | `User` | 200 OK | Calls `userService.updateUser`.

---

### C. `ProjectController`

| Endpoint | Method | Input | Output | Logic |
|---|---|---|---|---|
| `/api/v1/projects` | POST | `Project` | saved project | `projectService.createProject`.
| `/api/v1/projects` | GET | - | list | `projectService.getAllProjects()`.
| `/api/v1/projects/{id}` | GET | id | project or 404 | `projectService.getProjectById`.
| `/api/v1/projects/{id}` | PUT | Project | 200 | `projectService.updateProject`.
| `/api/v1/projects/{id}` | DELETE | id | 200 | `projectService.deleteProject` (cascade tasks).

---

### D. `TaskController`

| Endpoint | Method | Input | Output | Logic |
|---|---|---|---|---|
| `/api/v1/tasks` | POST | Task | saved task | `taskService.createTask`.
| `/api/v1/tasks` | GET | ?assignee=&projectId= | list tasks (filtered) | `taskService.getAllTasks()` then apply role-based filter; recommend server-side queries.
| `/api/v1/tasks/{id}` | POST | partial payload map | updated task | Read existing, merge fields, `taskService.updateTask`.
| `/api/v1/tasks/{id}` | PUT | Task | full update | `taskService.updateTask`.
| `/api/v1/tasks/{id}` | DELETE | id | 200 | `taskService.deleteTask`.

---

### E. `WorkLogController`

| Endpoint | Method | Input | Output | Logic |
|---|---|---|---|---|
| `/api/v1/worklogs` | POST | WorkLog | created | `workLogService.createWorkLog` (snapshot salary).
| `/api/v1/worklogs` | GET | - | list | `workLogService.getAllWorkLogs()`.
| `/api/v1/worklogs/{id}` | GET | id | worklog | `workLogService.getWorkLogById`.

---

### F. `FinanceController` & `PaymentController`

| Endpoint | Method | Input | Output | Logic |
|---|---|---|---|---|
| `/api/v1/finance/payroll/calculate` | POST | userId,month,year | Payroll object | `financeService.calculatePayroll`.
| `/api/v1/finance/payroll` | GET | - | list payrolls (rendered) | `financeService.getAllPayroll`.
| `/api/v1/payment/pay-invoice/{invoiceId}` | POST | invoiceId | map {payUrl,...} | `momoService.createPaymentUrl(invoice)`.
| `/api/v1/finance/pay` | POST | MoMo callback (mock) | 200 | Validate and `financeService.markPayrollsAsPaid`.

---

## 3. Utils (Client-side & Shared)

### A. `ApiClient` (Desktop `com.techforge.desktop`)

| Tên hàm | Signature | Input | Output | Logic | Notes |
|---|---|---|---|---|---|
| `public Map<String,Object> post(String endpoint, String json)` | endpoint,json | Map | 1. Build OkHttp request to `BASE_URL + endpoint`.
2. Execute synchronous `client.newCall(request).execute()`.
3. Parse response body JSON -> Map via Gson. | Should add timeouts and Authorization header handling. |
| `public void delete(String endpoint)` | endpoint | void | 1. Build DELETE request and execute.
2. Throw IOException nếu response not successful. | Implemented for UI delete operations. |

### B. `UIUtils`, `ImageLoader` (Desktop)

- `UIUtils.createRoundedButton(String text, Color bg, Color fg)` -> trả JButton styled.
- `UIUtils.createCardPanel()` -> trả JPanel có rounded border + drop shadow.
- `ImageLoader.loadImage(String path)` -> load ImageIcon từ classpath, fallback placeholder.

Các hàm utils chủ yếu thực hiện rendering/formatting; lỗi thường gặp: resource path incorrect (leading slash), caching icons to avoid IO overhead.

---

## 4. Mẫu mô tả chi tiết một hàm (theo yêu cầu)

**Ví dụ:**

### Hàm: `public List<Project> loadProjects()` (ví dụ minh họa - pattern common)

- **Input/Output:** Không có tham số. Trả về `List<Project>`.

- **Logic Step-by-Step:**
  1. Tạo `ObjectMapper mapper = new ObjectMapper();` (Jackson).
  2. Kiểm tra file `projects.json` tồn tại (ví dụ `Files.exists(Paths.get("projects.json"))`).
  3. Nếu tồn tại: đọc toàn bộ file `String content = Files.readString(path, StandardCharsets.UTF_8)`.
  4. Map sang `List<Project>`: `List<Project> list = mapper.readValue(content, new TypeReference<List<Project>>() {});`.
  5. Trả `list`.
  6. Nếu không tồn tại: trả `Collections.emptyList()`.

- **Điểm nhấn kỹ thuật:**
  - Sử dụng `TypeReference<List<Project>>` để Jackson biết generic type khi deserialize.
  - Phải bắt `IOException` và handle (log + fallback empty list).
  - Nếu dữ liệu lớn, cân nhắc stream parsing (`MappingIterator<Project>`) để giảm memory.

- **Xử lý lỗi & Robustness:**
  - Nếu file corrupt: catch `JsonProcessingException` -> log và trả empty list hoặc propagate exception tùy chính sách.
  - Nếu cần atomic write/read, sử dụng file locking để tránh read khi đang write.

---

## 5. Lời kết và hướng phát triển
- Tài liệu này mô tả API/service/controller/utils lõi của hệ thống. Những hàm được liệt kê là các entry-points quan trọng cho UI và cho tích hợp với hệ thống bên ngoài (MoMo, Email, AI).
- Đề xuất cải tiến tổng quát:
  - Chuẩn hoá status/enum trên server để giảm mapping lỗi trên client.
  - Chuyển mật khẩu sang hash (BCrypt) và triển khai authentication token (JWT) thay vì header `X-Requester-ID` không xác thực.
  - Thêm unit/integration tests cho các service tính toán (Finance/Report) và cho controller endpoints.

---

Phiên bản tài liệu: 0.9 (draft) - sẵn sàng để phê duyệt và bổ sung per-line nếu yêu cầu.


