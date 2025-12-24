# TechForge ERP
TechForge ERP — Hệ thống ERP nội bộ gồm Desktop Client (Java Swing) và Backend (Spring Boot) sử dụng Firebase Realtime Database.

## Tóm tắt
- Desktop client bằng Java Swing + FlatLaf.
- Backend bằng Spring Boot (REST API).
- Dữ liệu lưu trên Firebase Realtime Database (node `LTUD10`).
- Tích hợp thanh toán MoMo (HMAC, idempotency) và AI (Gemini) cho gợi ý nghiệp vụ.

## Tính năng chính
- Quản lý User, Project, Task, WorkLog, Payroll, Invoice, Expense, Client.
- Giao diện desktop native, phân quyền theo role (ADMIN / MANAGER / EMPLOYEE).
- Luồng thanh toán an toàn: HMAC-SHA256, xác minh IPN, idempotency, audit log.
- Tích hợp AI cho gợi ý phân công (non-authoritative) và Gap Analysis hỗ trợ kiểm thử.
- Asynchronous processing: `CompletableFuture` (backend) và `SwingWorker` (frontend).

## Công nghệ
- Java 21, Maven
- Spring Boot 3.2.x (backend)
- OkHttp + Gson (desktop client)
- Jackson (server)
- Firebase Admin SDK
- Lombok (models)
- AI tools: Google Gemini, Claude, GitHub Copilot (hỗ trợ phát triển)

## Cách chạy nhanh (local)
1. Cài JDK 21, Maven.
2. Thiết lập biến môi trường cần thiết (xem bên dưới).
3. Backend:
   - Build: `mvn -f backend/pom.xml clean package`
   - Chạy: `java -jar backend/target/techforge-erp.jar`
   - API base: `http://localhost:8080/api/v1`
4. Desktop:
   - Build: `mvn -f desktop/pom.xml clean package`
   - Chạy: `java -jar desktop/target/techforge-desktop.jar`

## Biến môi trường / Secrets (KHÔNG commit)
- `FIREBASE_SERVICE_ACCOUNT_JSON` (path hoặc content)
- `MOMO_SECRET_KEY`
- `SMTP_USERNAME`, `SMTP_PASSWORD`
- Các biến cấu hình khác theo `application.properties` (inject via env/secret manager)

## Ghi chú bảo mật quan trọng
- Client phải gửi Firebase ID Token bằng header `Authorization: Bearer <idToken>`. Backend phải verify token bằng Firebase Admin SDK và chỉ sau đó mới tin `X-Requester-ID`. Tuyệt đối không tin `X-Requester-ID` nếu không có token đã xác thực.
- Không lưu mật khẩu plaintext; dùng hashing.
- OTP: sinh bằng `SecureRandom`, lưu dưới dạng hash, TTL ngắn, single-use.
- MoMo IPN: luôn verify HMAC, áp dụng idempotency và audit logging.
- Không commit secrets hoặc cấu hình nhạy cảm vào repo.

## Kiểm thử & CI
- Chạy unit tests: `mvn test`
- Mọi thay đổi phải có test tương ứng và review trước khi merge.
- CI phải chạy test + static analysis.

## Contributing
- Tuân thủ kiến trúc phân lớp (Controller → Service → Model).
- Viết unit tests cho logic nghiệp vụ.
- Không commit secrets; dùng branch, PR và code review.

## License
- Thêm thông tin license tại `README.md` (ví dụ MIT) hoặc file `LICENSE`.

