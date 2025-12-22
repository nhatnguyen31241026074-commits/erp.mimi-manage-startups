-- ============================================================================
-- TechForge ERP - Database Schema
-- STRICT COMPLIANCE with Class Diagram (No Extra Fields)
--
-- Database: Firebase Realtime Database (NoSQL)
-- This SQL is provided for reference/documentation and for potential
-- migration to SQL databases like PostgreSQL/MySQL.
--
-- Generated: December 20, 2025
-- ============================================================================

-- ============================================================================
-- LAYER 1: AUTHENTICATION & AUTHORIZATION
-- ============================================================================

-- User Table (per Class Diagram)
-- Fields: id, username, email, passwordHash, fullName, phone, role, baseSalary, hourlyRateOT, salaryType
-- NOTE: "skills" field was intentionally REMOVED (not in authorized diagram)
CREATE TABLE IF NOT EXISTS users (
    id              VARCHAR(64) PRIMARY KEY,
    username        VARCHAR(100) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,  -- "passwordHash" in diagram
    full_name       VARCHAR(200),
    phone           VARCHAR(20),
    role            VARCHAR(50) NOT NULL,   -- ADMIN, MANAGER, EMPLOYEE, CLIENT, FINANCE
    base_salary     DECIMAL(12, 2),
    hourly_rate_ot  DECIMAL(10, 2),
    salary_type     VARCHAR(20),            -- "monthly" or "hourly"

    -- OTP fields for password reset (operational requirement)
    otp             VARCHAR(10),
    otp_expiry      TIMESTAMP,

    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Role Table (per Class Diagram)
CREATE TABLE IF NOT EXISTS roles (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(50) NOT NULL UNIQUE,  -- ADMIN, MANAGER, EMPLOYEE, CLIENT, FINANCE
    description     TEXT
);

-- Permission Table (per Class Diagram)
CREATE TABLE IF NOT EXISTS permissions (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    resource        VARCHAR(100) NOT NULL,        -- e.g., "projects", "tasks", "finance"
    action          VARCHAR(50) NOT NULL          -- e.g., "read", "write", "delete"
);

-- Role-Permission Junction Table
CREATE TABLE IF NOT EXISTS role_permissions (
    role_id         VARCHAR(64) NOT NULL,
    permission_id   VARCHAR(64) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

-- ============================================================================
-- LAYER 2: CLIENT & PROJECT MANAGEMENT
-- ============================================================================

-- Client Table (per Class Diagram)
CREATE TABLE IF NOT EXISTS clients (
    id              VARCHAR(64) PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    email           VARCHAR(255),
    phone           VARCHAR(20),
    company         VARCHAR(200)
);

-- Project Table (per Class Diagram)
CREATE TABLE IF NOT EXISTS projects (
    id              VARCHAR(64) PRIMARY KEY,
    client_id       VARCHAR(64),
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    budget          DECIMAL(15, 2),
    start_date      DATE,
    end_date        DATE,
    status          VARCHAR(50),                  -- PLANNING, ACTIVE, COMPLETED, CANCELLED

    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL
);

-- Project-Members Junction Table (for memberUserIds)
CREATE TABLE IF NOT EXISTS project_members (
    project_id      VARCHAR(64) NOT NULL,
    user_id         VARCHAR(64) NOT NULL,
    PRIMARY KEY (project_id, user_id),
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ============================================================================
-- LAYER 3: TASK & WORK TRACKING
-- ============================================================================

-- Task Table (per Class Diagram)
CREATE TABLE IF NOT EXISTS tasks (
    id              VARCHAR(64) PRIMARY KEY,
    project_id      VARCHAR(64),
    assigned_user_id VARCHAR(64),
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    priority        VARCHAR(20),                  -- LOW, MEDIUM, HIGH, CRITICAL
    status          VARCHAR(50),                  -- TODO, DOING, DONE
    estimated_hours DECIMAL(8, 2),

    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- WorkLog Table (per Class Diagram)
CREATE TABLE IF NOT EXISTS worklogs (
    id                      VARCHAR(64) PRIMARY KEY,
    task_id                 VARCHAR(64),
    user_id                 VARCHAR(64),
    project_id              VARCHAR(64),
    hours                   DECIMAL(8, 2),
    regular_hours           DECIMAL(8, 2),
    overtime_hours          DECIMAL(8, 2),
    work_date               DATE,
    base_salary_snapshot    DECIMAL(12, 2),       -- Frozen salary at time of log
    hourly_rate_ot_snapshot DECIMAL(10, 2),       -- Frozen OT rate at time of log

    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
);

-- ============================================================================
-- LAYER 4: FINANCE & PAYROLL
-- ============================================================================

-- Invoice Table (per Class Diagram)
-- Fields: id, projectId, clientId, amount, issueDate, status
-- NOTE: paidDate, transactionId, locked fields were intentionally REMOVED (not in authorized diagram)
CREATE TABLE IF NOT EXISTS invoices (
    id              VARCHAR(64) PRIMARY KEY,
    project_id      VARCHAR(64),
    client_id       VARCHAR(64),
    amount          DECIMAL(15, 2),
    issue_date      DATE,
    status          VARCHAR(50),                  -- PENDING, PAID, OVERDUE, CANCELLED

    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL,
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE SET NULL
);

-- Expense Table (per Class Diagram)
CREATE TABLE IF NOT EXISTS expenses (
    id              VARCHAR(64) PRIMARY KEY,
    project_id      VARCHAR(64),
    category        VARCHAR(100),
    amount          DECIMAL(12, 2),
    expense_date    DATE,

    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE SET NULL
);

-- Payroll Table (per Class Diagram)
CREATE TABLE IF NOT EXISTS payrolls (
    id              VARCHAR(64) PRIMARY KEY,
    user_id         VARCHAR(64),
    month           INTEGER NOT NULL,
    year            INTEGER NOT NULL,
    base_salary     DECIMAL(12, 2),
    overtime_pay    DECIMAL(12, 2),
    total_pay       DECIMAL(12, 2),
    is_paid         BOOLEAN DEFAULT FALSE,
    transaction_id  VARCHAR(100),                 -- External payment reference (for Payroll, not Invoice)

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ============================================================================
-- LAYER 5: AI & RISK MANAGEMENT
-- ============================================================================

-- RiskAlert Table (per Class Diagram)
CREATE TABLE IF NOT EXISTS risk_alerts (
    id              VARCHAR(64) PRIMARY KEY,
    project_id      VARCHAR(64),
    type            VARCHAR(100),                 -- BUDGET, DEADLINE, SCOPE, RESOURCE
    severity        VARCHAR(20),                  -- LOW, MEDIUM, HIGH, CRITICAL
    message         TEXT,

    FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_projects_client ON projects(client_id);
CREATE INDEX idx_projects_status ON projects(status);
CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_assignee ON tasks(assigned_user_id);
CREATE INDEX idx_tasks_status ON tasks(status);
CREATE INDEX idx_worklogs_user ON worklogs(user_id);
CREATE INDEX idx_worklogs_project ON worklogs(project_id);
CREATE INDEX idx_worklogs_date ON worklogs(work_date);
CREATE INDEX idx_invoices_project ON invoices(project_id);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_payrolls_user ON payrolls(user_id);
CREATE INDEX idx_payrolls_period ON payrolls(year, month);

-- ============================================================================
-- DEFAULT DATA (Roles & Permissions)
-- ============================================================================

INSERT INTO roles (id, name, description) VALUES
    ('role_admin', 'ADMIN', 'Full system access - CEO/Admin'),
    ('role_manager', 'MANAGER', 'Project and team management'),
    ('role_employee', 'EMPLOYEE', 'Task execution and time logging'),
    ('role_finance', 'FINANCE', 'Financial reports and payroll'),
    ('role_client', 'CLIENT', 'Project monitoring and reports');

INSERT INTO permissions (id, name, resource, action) VALUES
    ('perm_read_all', 'Read All', '*', 'read'),
    ('perm_write_all', 'Write All', '*', 'write'),
    ('perm_delete_all', 'Delete All', '*', 'delete'),
    ('perm_read_projects', 'Read Projects', 'projects', 'read'),
    ('perm_write_projects', 'Write Projects', 'projects', 'write'),
    ('perm_read_tasks', 'Read Tasks', 'tasks', 'read'),
    ('perm_write_tasks', 'Write Tasks', 'tasks', 'write'),
    ('perm_read_finance', 'Read Finance', 'finance', 'read'),
    ('perm_write_finance', 'Write Finance', 'finance', 'write');

-- ============================================================================
-- VERIFICATION QUERY
-- Confirms no unauthorized columns exist
-- ============================================================================

-- This query should return ONLY the authorized columns per Class Diagram:
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'users';
-- Expected: id, username, email, password_hash, full_name, phone, role, base_salary, hourly_rate_ot, salary_type, otp, otp_expiry, created_at, updated_at

-- SELECT column_name FROM information_schema.columns WHERE table_name = 'invoices';
-- Expected: id, project_id, client_id, amount, issue_date, status
-- NOT expected (removed): paid_date, transaction_id, locked, payment_method

-- ============================================================================
-- END OF SCHEMA
-- ============================================================================

