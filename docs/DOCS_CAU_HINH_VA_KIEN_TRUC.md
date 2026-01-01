    DOCS_CAU_HINH_VA_KIEN_TRUC.md

Tiêu đề: Phân tích cấu hình & kiến trúc dự án TechForge ERP
Ngày: 2025-12-21
Người thực hiện: Senior Java Architect (tài liệu cho buổi rà soát/bảo vệ đồ án)

Mục lục
1. Tổng quan
2. Phân tích `pom.xml` (Dependencies + Plugins)
   - Bảng chi tiết dependency
   - Phân tích các plugin build
3. Kiến trúc dự án (Project Architecture)
   - Cây thư mục chính
   - Mô hình kiến trúc (Layers)
   - Luồng dữ liệu (Data Flow Text Diagram)
4. Lý do chọn các công nghệ & rủi ro
5. Câu hỏi giảng viên có thể hỏi (Architecture & Config)

---

## 1. Tổng quan
Tài liệu này giải phẫu file cấu hình build `pom.xml` và mô tả kiến trúc mã nguồn của dự án TechForge ERP (backend Spring Boot + Firebase và desktop client Java Swing). Mục tiêu: giúp sinh viên hiểu tường tận tại sao từng dependency, plugin, và cấu trúc thư mục được chọn, đồng thời chuẩn bị các câu hỏi phỏng vấn/bảo vệ.

---

## 2. Phân tích `pom.xml` (Dependencies + Plugins)
File `pom.xml` của project (Spring Boot parent 3.2.3, Java 21) chứa các dependency và plugin sau đây.

### 2.1 Bảng phân tích dependency

| GroupID:ArtifactID | Phiên bản | Công dụng chính | Tại sao dự án cần | Câu hỏi thầy có thể hỏi |
|---|---:|---|---|---|
| org.springframework.boot:spring-boot-starter-web | (managed by parent) | Framework web Spring Boot (MVC, embedded Tomcat) | Cung cấp REST controllers, dependency injection, embedded server cho backend | "Tại sao dùng Spring Boot thay vì framework khác?" |
| org.springframework.boot:spring-boot-starter-security | (managed) | Spring Security core (filter chain, auth) | Cần để bật bảo mật / interceptor tùy chỉnh; RoleInterceptor sử dụng cùng ý tưởng | "Em đã cấu hình Spring Security thế nào? Nếu interceptors đã có, cần starter này để làm gì?" |
| jakarta.annotation:jakarta.annotation-api | 2.1.1 | Annotation API (e.g., @PostConstruct) | Để dùng các annotation Jakarta (tương thích với Spring Boot 3) | "Tại sao cần Jakarta annotations?" |
| org.springframework.boot:spring-boot-starter-websocket | (managed) | Hỗ trợ WebSocket cho realtime nếu cần | Dự án có thể dùng WebSocket cho UI real-time (không bắt buộc) | "Em có dùng WebSocket thực sự không?" |
| com.google.firebase:firebase-admin | 9.2.0 | Firebase Admin SDK (Realtime DB / Auth) | Kết nối tới Firebase Realtime Database, quản lý service account | "Em lưu serviceAccountKey ở đâu? Làm sao đảm bảo an toàn?" |
| org.projectlombok:lombok | 1.18.30 (managed) | Giảm boilerplate getters/setters/constructors | Dùng @Data, @NoArgsConstructor, v.v. giúp code ngắn gọn | "Lombok có vấn đề tương thích với JDK21? Em đã xử lý thế nào?" |
| org.jfree:jfreechart | 1.5.3 | Vẽ biểu đồ (charts) cho dashboard | Dùng trong `ClientPanel` để hiển thị Pie/Bar charts | "Tại sao dùng JFreeChart thay cho JavaFX charts?" |
| com.squareup.okhttp3:okhttp | 4.12.0 | HTTP client (desktop app) | Desktop client dùng OkHttp để gọi API backend | "Tại sao không dùng HttpURLConnection hoặc RestTemplate?" |
| com.google.code.gson:gson | 2.10.1 | JSON parsing cho Desktop (Gson) | Client sử dụng Gson để parse JSON responses | "Tại sao dùng Gson cho desktop và Jackson cho server?" |
| com.formdev:flatlaf | 3.4 | Look & Feel hiện đại cho Swing | Tạo giao diện đẹp hơn (Flat Light) | "FlatLaf có ảnh hưởng gì tới portability trên Windows/Mac?" |
| org.springframework.boot:spring-boot-starter-mail | (managed) | Gửi email SMTP (JavaMailSender) | Dùng để gửi OTP/forgot-password emails | "Em đã cấu hình SMTP thế nào cho testing?" |
| org.springframework.boot:spring-boot-starter-test | (test scope) | Testing libs (JUnit, AssertJ, MockMVC) | Cho unit/integration tests | "Em đã viết test nào chưa?" |

**Ghi chú:** Lombok được quản lý qua dependencyManagement để ép phiên bản >= 1.18.30 (tương thích JDK21) nhằm tránh lỗi NoSuchFieldError trong compiler internals.

### 2.2 Phân tích plugins trong `<build>`

| Plugin | Version | Mục đích cấu hình | Giải thích chi tiết / Why important | Câu hỏi thầy có thể hỏi |
|---|---:|---|---|---|
| maven-compiler-plugin | 3.10.1 | Thiết lập `source` và `target` bằng `${java.version}` (21) và khai báo `annotationProcessorPaths` cho Lombok | Đảm bảo compile với JDK21 và Lombok annotation processor có sẵn cho compiler. `-parameters` cho reflective parameter names | "Tại sao đặt source/target là 21? Có vấn đề tương thích không?" |
| spring-boot-maven-plugin | (managed) | Tạo executable jar, hỗ trợ `spring-boot:run` | Cho phép đóng gói app Spring Boot; plugin này gói dependencies và config để chạy bằng `java -jar` | "Làm sao đóng gói backend và desktop vào cùng 1 artifact?" |

**Lưu ý về LLVM/JPMS/Module System:** Project dùng classpath truyền thống; Java 21 có module system nhưng hiện không cấu hình `module-info.java` — phù hợp cho ứng dụng monolith Spring + Swing.

---

## 3. Kiến trúc dự án (Project Architecture)

### 3.1 Cây thư mục chính (phần đã scan)
```
src/main/java/
├─ com.techforge (main Spring Boot app)
├─ com.techforge.erp.controller (REST controllers)
├─ com.techforge.erp.service (business services)
├─ com.techforge.erp.model (domain DTOs/POJOs)
├─ com.techforge.erp.config (security, firebase)
├─ com.techforge.desktop (Java Swing client UI)
└─ techforge.ui (MomoPaymentDialog etc.)
```

### 3.2 Mô hình kiến trúc: Layered (không hoàn toàn strict MVC)
- **Presentation (View):** `com.techforge.desktop` — Java Swing UI (desktop). Mỗi màn hình là JFrame/JPanel.
- **API Layer (Controller):** `com.techforge.erp.controller` — REST endpoints cho Desktop gọi (Auth, Users, Projects, Tasks, Finance, Reports, AI).
- **Service Layer:** `com.techforge.erp.service` — chứa business logic, tính toán (Payroll, Report), và orchestration giữa Firebase và other services.
- **Data Layer:** Firebase Realtime Database accessed via `firebase-admin` SDK. No separate DAO package; services access Firebase directly.

**Đánh giá mô hình:**
- Đây là dạng Layered / Onion architecture: Controllers -> Services -> Firebase. Desktop client gọi REST controllers (api). Tuy nhiên, trong codebase desktop có `ApiClient` gọi REST trực tiếp; desktop và backend nằm trong cùng repo nhưng tách rời.
- **Ưu điểm:** Tách concerns rõ ràng, service testable.
- **Nhược điểm:** Một số services trực tiếp thao tác Firebase (coupling to RTDB); thiếu repository/DAO abstraction.

### 3.3 Data Flow (Text Diagram)
User (Desktop UI) -> ApiClient (OkHttp/Gson) -> Spring Boot Controllers -> Services -> Firebase Admin SDK -> Firebase Realtime DB

Payment Flow: AdminPanel (click Pay) -> Backend MomoService -> external MoMo sandbox -> payUrl returned -> Desktop opens MomoPaymentDialog

AI Flow: ManagerPanel (request AI) -> AIController -> AIService -> Gemini API -> parse results -> UI displays suggestion

---

## 4. Lý do chọn các công nghệ & rủi ro

### 4.1 Tại sao Spring Boot?
- Rapid development, embedded servlet container, dependency injection, mature ecosystem (Security, Mail, WebSocket).
- Giảng viên có thể hỏi: "Em tối ưu cấu hình để production chưa?" -> trả lời: cần thêm profiles, externalized config và secrets management.

### 4.2 Tại sao Firebase Admin SDK?
- Firebase Realtime DB dễ dùng cho prototyping; không cần quản lý DB server.
- Rủi ro: vendor-lock-in, thiếu relational queries; security rules và serviceAccountKey cần bảo mật.

### 4.3 Tại sao Swing + FlatLaf cho Desktop?
- Sinh viên thực hành ứng dụng desktop; Swing vẫn phổ biến và FlatLaf mang lại look hiện đại.
- Rủi ro: Swing UI code dễ bị 'spaghetti' nếu không tổ chức; cross-platform font/glyph issues (Windows squares) cần xử lý.

### 4.4 Tại sao OkHttp + Gson cho Desktop?
- OkHttp: lightweight, modern HTTP client.
- Gson: lightweight JSON parsing on desktop; backend dùng Jackson (Spring Boot) vì tích hợp tốt.
- Câu hỏi: "Tại sao không dùng same JSON lib trên client và server?" -> có thể, nhưng hiện dùng công cụ phù hợp mỗi bên; không bắt buộc phải giống.

---

## 5. Câu hỏi giảng viên có thể hỏi (Architecture & Config)
- Tại sao em chọn Firebase thay vì PostgreSQL/MySQL? (trả lời: nhanh prototyping, realtime features).
- Em xử lý secrets (serviceAccountKey.json, momo keys) như thế nào để an toàn? (trả lời: environment vars hoặc secret manager, không commit vào VCS).
- Làm sao hệ thống scale nếu số lượng users/tasks tăng lên? (trả lời: database rules, queries tối ưu, hoặc migrate sang Firestore / RDBMS nếu cần).
- Tại sao dùng Java Swing cho client thay vì JavaFX? (trả lời: thói quen/học thuật, FlatLaf hỗ trợ Look & Feel; JavaFX hiện đại hơn nhưng có learning curve).

---

## 6. Kết luận & Hướng dẫn tiếp theo
Tài liệu này đã phân tích `pom.xml`, giải thích lý do chọn từng dependency, phác thảo kiến trúc dự án và đưa ra các câu hỏi giảng viên có thể hỏi. Bước tiếp theo: per-file deep-dive (DOCS_GIAI_PHAU_CODE_CHI_TIET.md). Tôi sẽ bắt đầu tạo file đó nếu bạn xác nhận.


---

*Ghi chú:* Tôi đã đọc `pom.xml` và cấu trúc thư mục trong repo. Nếu bạn muốn, tôi sẽ bổ sung phần `Environment variables` (application.properties keys cần set: firebase.database-url, gemini.api-key, mail.smtp settings, momo secret) trong phần cấu hình; hoặc tiếp tục tạo file phân tích từng file Java.

