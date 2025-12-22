# TechForge ERP - Comprehensive Test Cases

> Generated: December 20, 2025
> Version: 1.0
> Author: System Architect

---

## 1. SECURITY TEST CASES

### 1.1 Role-Based Access Control (RBAC)

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| SEC-001 | Staff accessing Admin routes | 1. Login as EMPLOYEE role<br>2. Try to access `/api/v1/users` (Admin only)<br>3. Check response | HTTP 403 Forbidden | Tests `User.hasPermission()` and `RoleInterceptor` |
| SEC-002 | Client accessing Finance routes | 1. Login as CLIENT role<br>2. Try to access `/api/v1/finance/payroll`<br>3. Check response | HTTP 403 Forbidden | CLIENT should only access project monitoring |
| SEC-003 | Manager accessing AI routes | 1. Login as MANAGER role<br>2. Access `/api/v1/ai/suggest`<br>3. Check response | HTTP 200 OK | MANAGER should have AI access |
| SEC-004 | Missing X-Requester-ID header | 1. Call any `/api/v1/*` endpoint<br>2. Do NOT include X-Requester-ID header | HTTP 401 Unauthorized | Tests auth header requirement |
| SEC-005 | Invalid User ID in header | 1. Call endpoint with X-Requester-ID: "invalid-id-12345"<br>2. Check response | HTTP 401 Unauthorized | User not found in database |

### 1.2 SQL/NoSQL Injection Protection

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| SEC-010 | SQL Injection on Login - Username | 1. Enter username: `admin' OR '1'='1`<br>2. Enter any password<br>3. Click Login | Login fails with "Invalid credentials" | Firebase should escape special chars |
| SEC-011 | SQL Injection on Login - Password | 1. Enter valid username<br>2. Enter password: `' OR '1'='1' --`<br>3. Click Login | Login fails | Password field injection |
| SEC-012 | NoSQL Injection - JSON payload | 1. POST to `/auth/login`<br>2. Body: `{"email": {"$gt": ""}, "password": "123"}` | HTTP 400 Bad Request | JSON object instead of string |
| SEC-013 | XSS in Project Name | 1. Create project with name: `<script>alert('XSS')</script>`<br>2. View project list | Script tags are escaped/displayed as text | HTML sanitization |

### 1.3 Authentication Edge Cases

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| SEC-020 | Expired OTP for password reset | 1. Request OTP for email<br>2. Wait 15+ minutes<br>3. Try to reset with old OTP | Error: "Lỗi OTP rồi BẠN ỚI!" | OTP expiry enforcement |
| SEC-021 | Reusing same OTP twice | 1. Reset password with valid OTP<br>2. Try to reset again with same OTP | Error: OTP invalid/already used | One-time use enforcement |
| SEC-022 | Password change - wrong old password | 1. Call change-password API<br>2. Provide incorrect old password | HTTP 400 Bad Request | Old password verification |

---

## 2. REPORT LOGIC TEST CASES

### 2.1 Division by Zero Protection

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| RPT-001 | Project Report with ZERO tasks | 1. Create a project with no tasks<br>2. Call `GET /reports/project/{id}` | `progress: 0.0` (not NaN or error) | Division by zero in percentage |
| RPT-002 | Monthly Report with ZERO worklogs | 1. Select a month with no logged work<br>2. Generate monthly report | `totalPayroll: 0.0`, `profit: 0.0` | Empty data handling |
| RPT-003 | Progress Report - no end date | 1. Create project without endDate<br>2. Get progress report | `daysRemaining: null`, `riskLevel: "LOW"` | Null date handling |

### 2.2 Date Range Validation

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| RPT-010 | Export with invalid date range | 1. Set Start Date: 2025-12-31<br>2. Set End Date: 2025-01-01<br>3. Click Export | Validation error: "Start date must be before end date" | End before Start |
| RPT-011 | Export for date range with no data | 1. Select date range in the distant future<br>2. Export report | Report generates with empty/zero values | No matching records |
| RPT-012 | Export daily report for today | 1. Select "Daily" type<br>2. Verify dates auto-populate to today | Start = End = Today's date | Daily report logic |

### 2.3 File Export

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| RPT-020 | Export to CSV | 1. Click Export<br>2. Choose CSV format<br>3. Save file | File downloads, opens in Excel | CSV format correctness |
| RPT-021 | Export to PDF | 1. Choose PDF format<br>2. Save file | File downloads, contains report data | PDF generation |
| RPT-022 | Export with special characters | 1. Project name contains "Café & Bar"<br>2. Export report | Special chars preserved in file | Unicode handling |

---

## 3. TERMS & CONDITIONS TEST CASES

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| TNC-001 | Signup without T&C checkbox | 1. Fill all registration fields<br>2. Leave T&C checkbox UNCHECKED<br>3. Click Create Account | Error: "You must agree to the Terms & Privacy Policy." | Required checkbox validation |
| TNC-002 | Signup with T&C checked | 1. Fill all fields correctly<br>2. CHECK the T&C checkbox<br>3. Click Create Account | Registration succeeds | Normal flow |
| TNC-003 | View T&C before agreeing | 1. Click "Terms & Privacy Policy" link<br>2. Read dialog content | Full terms displayed in scrollable dialog | T&C dialog opens |
| TNC-004 | T&C link on Login page | 1. Go to Login page<br>2. Click Terms link | Summary dialog or info displayed | Login page T&C access |

---

## 4. BUSINESS LOGIC TEST CASES

### 4.1 Invoice Payment Flow

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| BIZ-001 | Mark invoice as paid | 1. Have unpaid invoice<br>2. Call `markAsPaid("TXN123", "MOMO")` | Status = "PAID", locked = true | Normal payment flow |
| BIZ-002 | Edit paid invoice | 1. Mark invoice as paid<br>2. Try to modify amount | Operation fails, invoice is locked | Immutability after payment |
| BIZ-003 | Pay already-paid invoice | 1. Mark invoice as paid<br>2. Call markAsPaid again | Returns false, no changes | Duplicate payment prevention |

### 4.2 Salary Calculation

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| BIZ-010 | Monthly salary calculation | 1. User salaryType = "monthly"<br>2. Log 40 regular hours<br>3. Calculate payroll | `hourlyRate = baseSalary / 160` | Monthly to hourly conversion |
| BIZ-011 | Hourly salary calculation | 1. User salaryType = "hourly"<br>2. Log 40 hours<br>3. Calculate payroll | `hourlyRate = baseSalary` (no division) | Direct hourly rate |
| BIZ-012 | Overtime calculation | 1. Log 10 overtime hours<br>2. hourlyRateOT = $50<br>3. Calculate | `overtimePay = 10 * 50 = $500` | OT rate application |

### 4.3 Risk Alert Integration

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| BIZ-020 | Overdue project shows HIGH risk | 1. Project deadline = yesterday<br>2. View project list | Risk column shows "⚠ HIGH" (red) | Overdue detection |
| BIZ-021 | Project due in 7 days | 1. Project deadline = 7 days from now<br>2. View project list | Risk column shows "⚡ MED" (orange) | Near deadline |
| BIZ-022 | Project due in 60 days | 1. Project deadline = 60 days from now | Risk column shows "✓ LOW" (green) | Low risk threshold |

---

## 5. UI/UX TEST CASES

### 5.1 Form Validation

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| UI-001 | Empty email on login | 1. Leave email empty<br>2. Click Login | Error message, field highlighted | Required field |
| UI-002 | Invalid email format | 1. Enter "notanemail"<br>2. Click Login | Error: "Please enter a valid email" | Email format validation |
| UI-003 | Password mismatch on register | 1. Password: "abc123"<br>2. Confirm: "abc456"<br>3. Submit | Error: "Passwords do not match" | Password confirmation |

### 5.2 Dialog Overflow

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| UI-010 | Reset Password dialog on small screen | 1. Resize window to 800x600<br>2. Open Reset Password dialog | Dialog scrollable, all buttons visible | Small screen handling |
| UI-011 | Registration form overflow | 1. Open Register page<br>2. All fields visible | Scrollable form, Register button always accessible | Form length |

---

## 6. AI SERVICE TEST CASES

| Test ID | Test Case | Steps | Expected Result | Edge Case |
|---------|-----------|-------|-----------------|-----------|
| AI-001 | Recommend user with matching skills | 1. Task requires "Java"<br>2. User has skill "Java: Expert"<br>3. Call recommendUser | User recommended with high confidence | Skill matching |
| AI-002 | Recommend user - no matching skills | 1. Task requires "Rust"<br>2. No users have Rust skill<br>3. Call recommendUser | Lower confidence score, still returns suggestion | Fallback logic |
| AI-003 | AI response with markdown | 1. Gemini returns: \`\`\`json {...}\`\`\`<br>2. Parse response | Markdown stripped, JSON parsed correctly | Response cleaning |

---

## Test Execution Summary

| Category | Total Tests | Priority |
|----------|-------------|----------|
| Security | 15 | HIGH |
| Report Logic | 9 | HIGH |
| T&C | 4 | MEDIUM |
| Business Logic | 9 | HIGH |
| UI/UX | 5 | MEDIUM |
| AI Service | 3 | LOW |
| **TOTAL** | **45** | - |

---

## Notes for QA Team

1. **Security tests should be run on every deployment**
2. **Report tests require sample data in Firebase**
3. **T&C checkbox is MANDATORY for production release**
4. **Division by zero bugs have been fixed in ReportService**
5. **All dates should be tested in different timezones**


