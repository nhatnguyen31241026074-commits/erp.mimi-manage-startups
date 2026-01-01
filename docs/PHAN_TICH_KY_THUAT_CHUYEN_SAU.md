# Phân tích kỹ thuật chuyên sâu

Tài liệu này giải thích các khái niệm kỹ thuật khó trong dự án TechForge ERP và đưa ra mã giả (pseudo-code) dễ hiểu để minh họa cách hoạt động trên cả backend và frontend.

---

## Mục lục
1. Xử lý bất đồng bộ (Asynchronous)
   - Backend: CompletableFuture
   - Frontend: SwingWorker
2. Design Patterns (Mẫu thiết kế) được áp dụng
3. Tích hợp MoMo & AI
   - MomoService: tạo chữ ký HMAC và luồng thanh toán
   - AIService: gửi prompt tới Gemini và xử lý JSON trả về
4. Lập trình hướng đối tượng (OOP)
   - Đa hình (Polymorphism)
   - Đóng gói (Encapsulation)
   - Kế thừa (Inheritance)
   - Trừu tượng (Abstraction)

---

# 1. Xử lý bất đồng bộ (Asynchronous)

Mục đích: giảm thời gian chờ, tránh block thread, và tận dụng I/O concurrency khi gọi nhiều nguồn dữ liệu (ví dụ: Firebase + external API như MoMo).

### 1.1 Backend: Tại sao dùng `CompletableFuture`?

- Firebase SDK và các HTTP client (MoMo) là I/O-bound: thời gian chờ chủ yếu do mạng/IO, không phải CPU.
- Nếu gọi tuần tự: tổng thời gian = t_firebase + t_momo.
- Nếu gọi song song bằng `CompletableFuture`, tổng thời gian ≈ max(t_firebase, t_momo) (kéo ngắn tổng độ trễ).
- `CompletableFuture` cho phép:
  - Khởi tạo nhiều task I/O song song.
  - Kết hợp kết quả (thenCombine, allOf) và xử lý lỗi không chặn.
  - Sử dụng pool thread riêng (Executor) để tránh chiếm thread của Tomcat main.

Pseudo-code (Java-like):

```java
// Pseudo-code: Tính payroll cho nhiều user và gọi MoMo tạo transaction đồng thời
ExecutorService ioPool = Executors.newFixedThreadPool(50);

CompletableFuture<User> userF = CompletableFuture.supplyAsync(() -> userService.getUserById(uid), ioPool);
CompletableFuture<List<WorkLog>> logsF = CompletableFuture.supplyAsync(() -> workLogService.getByUser(uid), ioPool);

CompletableFuture<Payroll> payrollF = userF.thenCombine(logsF, (user, logs) -> {
    return financeService.calculatePayrollFrom(user, logs);
});

// Nếu cần gọi MoMo để tạo payment URL sau khi có payroll:
CompletableFuture<PaymentResponse> paymentF = payrollF.thenCompose(p ->
    CompletableFuture.supplyAsync(() -> momoService.createPaymentRequest(p), ioPool)
);

// Chờ kết quả (non-blocking at controller level) và trả về khi xong
return payrollF.thenCombine(paymentF, (p, pay) -> buildResponse(p, pay));
```

Lợi ích chính:
- Giảm latency tổng: I/O song song.
- Tăng throughput: Tomcat không bị block, có thể xử lý nhiều request hơn.
- Tốt cho retry, timeouts, fallback: `exceptionally`, `orTimeout`, `handle`.

### 1.2 Frontend: Tại sao dùng `SwingWorker`?

- Swing là single-threaded UI: mọi thao tác cập nhật giao diện phải chạy trên Event Dispatch Thread (EDT).
- Nếu gọi API trực tiếp trên EDT (ví dụ gọi `ApiClient.get()` trong actionPerformed), EDT bị block: giao diện đóng băng, không phản hồi.
- `SwingWorker` tách phần tốn thời gian ra background thread (doInBackground) và cho phép cập nhật kết quả trên EDT trong `done()`.

Pseudo-code (Swing):

```java
loginButton.addActionListener(e -> {
    setLoading(true); // chạy trên EDT
    new SwingWorker<User, Void>() {
        @Override
        protected User doInBackground() throws Exception {
            // gọi mạng, parse JSON - chạy trên background thread
            return apiClient.login(email, password);
        }

        @Override
        protected void done() {
            try {
                User user = get(); // an toàn, chạy trên EDT
                openDashboard(user);
            } catch (Exception ex) {
                showError(ex.getMessage());
            } finally {
                setLoading(false);
            }
        }
    }.execute();
});
```

Nếu không dùng SwingWorker mà gọi trực tiếp: UI sẽ đóng băng cho tới khi request hoàn thành; người dùng có thể nghĩ ứng dụng bị treo.

---

# 2. Design Patterns (Mẫu thiết kế)

Dự án áp dụng nhiều mẫu thiết kế phổ biến; dưới đây là những mẫu nổi bật, giải thích lý do và mã giả minh họa.

### 2.1 Singleton — `FirebaseConfig`

- Mục tiêu: chỉ khởi tạo một instance cấu hình/connection tới Firebase Admin SDK để tái sử dụng.
- Lợi ích: quản lý tài nguyên (socket, thread pools) tập trung.

Pseudo-code:

```java
public class FirebaseConfig {
    private static volatile FirebaseApp instance;

    public static FirebaseApp getInstance() {
        if (instance == null) {
            synchronized(FirebaseConfig.class) {
                if (instance == null) {
                    instance = initializeFirebaseApp(...);
                }
            }
        }
        return instance;
    }
}
```

### 2.2 Adapter — `ApiClient` (desktop)

- Mục tiêu: gom gọn chi tiết HTTP client (OkHttp) và expose API đơn giản cho UI (get/post/json).
- Lợi ích: tách rõ transport layer, dễ thay đổi HTTP lib mà không ảnh hưởng UI code.

Pseudo-code:

```java
public class ApiClient {
    private HttpClient http; // OkHttp
    public ApiClient(String baseUrl) { this.http = new OkHttpClient(...); }

    public String get(String path) { /* build request, execute, return body */ }
    public String post(String path, String json) { /* ... */ }
}
```

### 2.3 Factory / Builder — tạo các DTO/Request

- Khi xây dựng payload phức tạp (ví dụ payload MoMo), dùng pattern Builder để rõ ràng, tránh constructor dài.

Pseudo-code:

```java
PaymentRequest req = PaymentRequest.builder()
    .partnerCode(...)
    .amount(1000)
    .orderId(UUID.randomUUID().toString())
    .build();
```

### 2.4 Interceptor (Pattern kiểu middleware) — `RoleInterceptor`

- `RoleInterceptor` implement `HandlerInterceptor` (Spring) như middleware để kiểm tra auth/role trước controller.
- Pattern này tương tự Chain of Responsibility: request đi qua chain và có thể bị chặn.

### 2.5 Other patterns observed
- Template Method: `initializeUI()` trên các Panel để đồng bộ hoá khởi tạo UI.
- Strategy: Có thể thấy khi chọn chiến lược tính lương (monthly vs hourly) trong `FinanceService`.

---

# 3. Tích hợp MoMo & AI

### 3.1 MomoService — logic tạo chữ ký HMAC & luồng thanh toán

Bối cảnh: MoMo yêu cầu payload có chữ ký (signature) HMAC-SHA256 để xác thực request từ merchant.

Các bước chính của `MomoService`:
1. Chuẩn hoá dữ liệu (amount: integer, không âm)
2. Xây dựng raw string để ký (rawSignature) theo spec MoMo (ví dụ: partnerCode + orderId + amount + ...)
3. Sinh HMAC-SHA256 bằng `secretKey` của merchant
4. Gửi request HTTP tới endpoint MoMo với payload JSON kèm `signature`
5. Lưu transaction tạm thời ở trạng thái `PENDING`
6. Chờ MoMo trả về `payUrl` hoặc xử lý IPN (callback)
7. Khi IPN tới, verify signature của IPN và cập nhật trạng thái `PAID`

Pseudo-code (tạo signature):

```java
public String signWithHmacSHA256(String data, String secret) {
    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    mac.init(keySpec);
    byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    return Base64.getEncoder().encodeToString(rawHmac);
}

// Build payload
String raw = "partnerCode="+partnerCode+"&orderId="+orderId+"&amount="+amount+...;
String signature = signWithHmacSHA256(raw, secretKey);

Json payload = {
  partnerCode, orderId, amount, signature, ...
};
http.post(momoUrl, payload);
```

IPN handling pseudo-code (callback verification):

```java
// IPN POST handler
public void handleIpn(Map<String,Object> ipn) {
    String ipnSignature = (String) ipn.get("signature");
    String raw = buildRawStringFromIpn(ipn);
    String expected = signWithHmacSHA256(raw, secretKey);
    if (!expected.equals(ipnSignature)) {
        // reject -> possible forgery
        return; 
    }
    // idempotency: check if transaction already processed
    if (isAlreadyProcessed(ipn.get("transId"))) return;
    // update payroll/invoice status to PAID
}
```

Lưu ý bảo mật:
- `secretKey` phải nằm ngoài repo (env var / secret manager).
- Luôn verify signature cả ở request trả về và callback.
- Áp idempotency token để tránh xử lý trùng lặp.

### 3.2 AIService — gửi prompt tới Gemini và xử lý JSON trả về

Luồng cơ bản:
1. Xây dựng prompt dựa trên context (task, users, worklogs) — prompt engineering quan trọng.
2. Gọi HTTP POST tới endpoint Gemini kèm API key (Authorization: Bearer <key>)
3. Nhận response (thường text hoặc JSON string). Do LLM có thể trả văn bản kèm markdown, cần "clean" response.
4. Parse JSON sạch thành DTO và apply logic (gợi ý assignee).

Pseudo-code:

```java
String prompt = buildPrompt(task, candidateUsers);
String body = json({"model":"gemini-xyz","input":prompt});
String rawResp = http.post(geminiUrl, body, headers={"Authorization":"Bearer "+apiKey});
String cleaned = cleanJsonResponse(rawResp); // remove markdown fences, extract JSON
AISuggestion sug = objectMapper.readValue(cleaned, AISuggestion.class);
return sug;
```

`cleanJsonResponse` cần thực hiện:
- Loại bỏ ```json fences, bất kỳ tiền tố/suffix mô tả
- Tìm cặp ngoặc JSON đầu tiên và parse substring
- Nếu parse fail -> fallback heuristic (ví dụ chọn user có totalHours lớn nhất)

Lưu ý:
- Không gửi dữ liệu nhạy cảm (PII, salary) lên model.
- Giới hạn rate và cache kết quả để giảm chi phí.

---

# 4. Lập trình hướng đối tượng (OOP)

### 4.1 Đa hình (Polymorphism)

Các ví dụ trong dự án:
- **Override `paintComponent(Graphics g)`**: nhiều Panel kế thừa `JPanel` và override `paintComponent` để custom rendering (cards, gradients). Đây là runtime polymorphism — framework gọi phương thức phù hợp của lớp con.

Pseudo-code:
```java
public class DashboardPanel extends JPanel {
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // draw custom cards, rounded rectangles
    }
}
```

- **TableCellRenderer / ActionListener / ValueEventListener**: các class khác nhau implement cùng interface và JVM gọi thực thể cụ thể tại runtime.

### 4.2 Đóng gói (Encapsulation)

- **Model classes (`User`, `Project`, `Task`, `Payroll`)**: các field private, truy cập qua getters/setters (thường sinh bởi Lombok `@Data`). Service layer kiểm soát validation—UI không trực tiếp set các thuộc tính nhạy cảm.

Pseudo-code:
```java
public class User {
    private String id;
    private Double baseSalary; // private

    public Double getBaseSalary() { return baseSalary; }
    public void setBaseSalary(Double s) {
        if (s == null || s < 0) throw new IllegalArgumentException("invalid");
        this.baseSalary = s;
    }
}
```

Lợi ích:
- Bảo vệ invariants của đối tượng (không cho baseSalary âm).
- Giảm vùng lỗi khi nhiều module tương tác.

## 4.3 Kế thừa (Inheritance)

**Phân tích (Văn xuôi):**
Trong lớp giao diện Desktop của TechForge, kế thừa là cơ chế nền tảng giúp tái sử dụng hành vi và giao diện chung giữa các màn hình. Các lớp cụ thể như `LoginFrame`, `MainDashboardFrame` kế thừa từ `JFrame`; các panel như `DashboardPanel`, `PayrollPanel`, `ManagerPanel` kế thừa từ `JPanel`. Ngoài ra, trong dự án có thể (và nên) tồn tại các lớp cha tùy biến do nhóm định nghĩa như `BaseFrame` hoặc `BasePanel` để gom các thành phần chung (header, footer, toolbar, menu, common listeners). Việc này giảm lượng mã lặp, đồng nhất trải nghiệm người dùng và tạo extension points cho các mở rộng sau này (ví dụ thêm theme, multi-language, logging UI). Kế thừa ở tầng UI cũng giúp áp dụng template method pattern: lớp cha định nghĩa khung khởi tạo (`initializeUI()`) và các lớp con chỉ triển khai các phần đặc thù của mình.

**Bảng minh chứng (Evidence table):**

| Lớp con (Child) | Lớp cha (Parent) | Mục đích kế thừa |
| :--- | :--- | :--- |
| `LoginFrame` | `BaseFrame` / `JFrame` | Tái sử dụng header, menu, cấu hình chung cửa sổ |
| `DashboardPanel` | `BasePanel` / `JPanel` | Dùng chung layout cards, theme, tooltip helpers |
| `MomoPaymentDialog` | `JDialog` | Modal behaviour và lifecycle xử lý thanh toán |

**Pseudo-code (minh họa kế thừa & template):**

```java
// BasePanel đóng gói các thành phần chung
public abstract class BasePanel extends JPanel {
    protected void initializeCommonHeader() {
        // add title, breadcrumb, common buttons
    }
    protected abstract void initializeContent(); // template method

    public final void initializeUI() {
        initializeCommonHeader();
        initializeContent(); // lớp con triển khai
        applyTheme();
    }
}

// DashboardPanel kế thừa
public class DashboardPanel extends BasePanel {
    @Override
    protected void initializeContent() {
        // tạo các cards, chart, table
    }
}
```

Như pseudo-code trên cho thấy, `BasePanel` cung cấp khung khởi tạo và các tiện ích chung, trong khi `DashboardPanel` chỉ tập trung vào nội dung riêng, tận dụng kế thừa để giảm trùng lắp.


## 4.4 Trừu tượng (Abstraction)

**Phân tích (Văn xuôi):**
Trừu tượng (abstraction) được áp dụng mạnh mẽ ở tầng backend để tách biệt phần định nghĩa hành vi (interface) và phần cài đặt cụ thể (implementation). Pattern này thể hiện qua việc định nghĩa các interface như `UserService`, `ProjectService`, `FinanceService` và cung cấp các lớp cài đặt `UserServiceImpl`, `ProjectServiceImpl`,… Việc tách biệt này mang lại lợi ích lớn: khi muốn đổi nguồn lưu trữ (ví dụ từ Firebase sang MySQL/Postgres), chỉ cần viết một implementation mới của interface (ví dụ `UserServiceJdbcImpl`) mà không phải sửa controller hay tầng UI. Ngoài ra, abstraction giúp việc mock service trong unit test trở nên đơn giản, tăng khả năng test isolation.

Tại tầng UI, `AbstractTableModel` là một ví dụ trừu tượng hóa cho việc hiển thị dữ liệu lên `JTable`: model cung cấp API trừu tượng (`getRowCount()`, `getColumnCount()`, `getValueAt(...)`) để JTable gọi mà không biết dữ liệu thực tế được lưu ở đâu. Nhờ vậy, đổi nguồn dữ liệu (list, DB, API cache) không ảnh hưởng đến cách JTable render.

**Bảng minh chứng (Evidence table):**

| Interface / Abstract | Implementation (có trong project) | Vai trò/ Lợi ích |
| :--- | :--- | :--- |
| `UserService` (interface) | `UserServiceImpl` | Tách định nghĩa nghiệp vụ (create/get/update) và cài đặt Firebase; dễ thay thế impl khác |
| `FinanceService` (interface) | `FinanceServiceImpl` | Ẩn chi tiết tính toán lương, facilitate mocking in tests |
| `AbstractTableModel` (Swing API) | `PayrollTableModel` (project) | Abstraction cho JTable, cung cấp view-model separation |

**Pseudo-code (minh họa abstraction backend và AbstractTableModel):**

```java
// Interface Service
public interface UserService {
    User createUser(UserDto dto);
    Optional<User> findById(String id);
}

// Firebase implementation
public class UserServiceImpl implements UserService {
    private final FirebaseRepository repo; // phụ thuộc vào Firebase
    public User createUser(UserDto dto) {
        // validate, map, repo.save(...)
    }
    public Optional<User> findById(String id) { /* ... */ }
}

// Nếu chuyển DB: ta chỉ cần viết
public class UserServiceJdbcImpl implements UserService {
    private final JdbcTemplate jdbc;
    // implement createUser, findById dùng SQL
}

// AbstractTableModel cho Payroll
public class PayrollTableModel extends AbstractTableModel {
    private final List<Payroll> rows;
    private final String[] cols = {"ID","Employee","Amount","Status"};

    @Override public int getRowCount() { return rows.size(); }
    @Override public int getColumnCount() { return cols.length; }
    @Override public Object getValueAt(int row, int col) {
        Payroll p = rows.get(row);
        switch(col) {
            case 0: return p.getId();
            case 1: return p.getEmployeeName();
            case 2: return p.getTotalPay();
            case 3: return p.isPaid() ? "PAID" : "PENDING";
        }
        return null;
    }
}
```

Như pseudo-code trên minh hoạ: controller hoặc UI chỉ tương tác với `UserService` (interface) và `PayrollTableModel` (AbstractTableModel) mà không cần biết chi tiết lưu trữ hoặc cấu trúc dữ liệu nội bộ. Điều này tăng tính linh hoạt, khả năng mở rộng và dễ dàng thay đổi công nghệ lưu trữ khi cần.

---

# Kết luận ngắn
- Dùng `CompletableFuture` ở backend giúp tận dụng concurrency cho I/O-bound tasks (Firebase, MoMo), cải thiện latency và throughput.
- Dùng `SwingWorker` ở frontend để tránh block EDT và giữ UI luôn phản hồi.
- Dự án áp dụng nhiều Design Patterns hữu dụng (Singleton, Adapter, Factory, Interceptor) để giữ code có cấu trúc, dễ bảo trì.
- Tích hợp MoMo yêu cầu chữ ký HMAC và xử lý IPN an toàn; tích hợp AI cần prompt engineering, làm sạch response và cơ chế fallback.
- OOP (Polymorphism, Encapsulation) hiện diện rõ rệt trong UI và model, giúp mã nguồn linh hoạt và an toàn.

---

## Các bước tiếp theo (gợi ý)
- Thêm unit test cho `MomoService` (signature + IPN verification + idempotency).
- Thêm integration test cho `AIService.cleanJsonResponse()` với edge cases (markdown fences, extra text).
- Kiểm tra timeout và fallback logic trong `RoleInterceptor` (hiện có orTimeout / join pattern).
