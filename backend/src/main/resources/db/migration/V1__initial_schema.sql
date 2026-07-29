CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL
);

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE role_permissions (
    role_id UUID NOT NULL REFERENCES roles(id),
    permission_id UUID NOT NULL REFERENCES permissions(id),
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID,
    login VARCHAR(180) NOT NULL UNIQUE,
    display_name VARCHAR(180) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locked_until TIMESTAMPTZ,
    failed_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    password_changed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES user_accounts(id),
    role_id UUID NOT NULL REFERENCES roles(id),
    granted_by UUID REFERENCES user_accounts(id),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE refresh_token_sessions (
    id UUID PRIMARY KEY,
    family_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES user_accounts(id),
    token_hash CHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ,
    user_agent VARCHAR(512),
    ip_address VARCHAR(64)
);
CREATE INDEX idx_refresh_user_family ON refresh_token_sessions(user_id, family_id);

CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    city VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID NOT NULL REFERENCES branches(id),
    parent_id UUID REFERENCES departments(id),
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (parent_id IS NULL OR parent_id <> id)
);
CREATE INDEX idx_department_branch ON departments(branch_id, active);

CREATE TABLE designations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    department_id UUID REFERENCES departments(id),
    name VARCHAR(160) NOT NULL,
    level INTEGER NOT NULL DEFAULT 1 CHECK (level > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (department_id, name)
);

CREATE SEQUENCE employee_business_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_number VARCHAR(40) NOT NULL UNIQUE,
    first_name VARCHAR(80) NOT NULL,
    middle_name VARCHAR(80),
    last_name VARCHAR(80) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    official_email VARCHAR(180) NOT NULL UNIQUE,
    personal_email VARCHAR(180),
    phone VARCHAR(30),
    department_id UUID NOT NULL REFERENCES departments(id),
    designation_id UUID NOT NULL REFERENCES designations(id),
    branch_id UUID NOT NULL REFERENCES branches(id),
    manager_id UUID REFERENCES employees(id),
    employment_type VARCHAR(30) NOT NULL,
    joining_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    work_location VARCHAR(160),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (manager_id IS NULL OR manager_id <> id),
    CHECK (status IN ('DRAFT','ONBOARDING','ACTIVE','ON_LEAVE','NOTICE_PERIOD','SUSPENDED','RESIGNED','TERMINATED','INACTIVE'))
);
ALTER TABLE user_accounts
    ADD CONSTRAINT fk_user_employee FOREIGN KEY (employee_id) REFERENCES employees(id);
CREATE UNIQUE INDEX uq_user_employee ON user_accounts(employee_id) WHERE employee_id IS NOT NULL;
CREATE INDEX idx_employee_directory ON employees(department_id, status, display_name);

CREATE TABLE availability_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    starts_at TIME NOT NULL,
    ends_at TIME NOT NULL,
    slot_minutes INTEGER NOT NULL CHECK (slot_minutes BETWEEN 10 AND 240),
    buffer_minutes INTEGER NOT NULL DEFAULT 0 CHECK (buffer_minutes BETWEEN 0 AND 120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (starts_at < ends_at),
    UNIQUE (employee_id, day_of_week, starts_at)
);

CREATE TABLE visitors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    email VARCHAR(180) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    company VARCHAR(180),
    verification_status VARCHAR(30) NOT NULL DEFAULT 'UNVERIFIED',
    restricted BOOLEAN NOT NULL DEFAULT FALSE,
    restriction_reason VARCHAR(500),
    consent_version VARCHAR(40) NOT NULL,
    consented_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (verification_status IN ('UNVERIFIED','OTP_VERIFIED','IDENTITY_VERIFIED','REJECTED'))
);
CREATE INDEX idx_visitor_lookup ON visitors(email, phone);

CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_number VARCHAR(40) NOT NULL UNIQUE,
    idempotency_key VARCHAR(100) UNIQUE,
    visitor_id UUID NOT NULL REFERENCES visitors(id),
    host_employee_id UUID NOT NULL REFERENCES employees(id),
    type VARCHAR(40) NOT NULL,
    purpose VARCHAR(500) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(40) NOT NULL,
    verification_hash CHAR(64),
    qr_token_hash CHAR(64),
    decision_by UUID REFERENCES user_accounts(id),
    decision_at TIMESTAMPTZ,
    decision_remarks VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (starts_at < ends_at),
    CHECK (status IN ('DRAFT','PENDING_VERIFICATION','PENDING_APPROVAL','APPROVED','REJECTED','RESCHEDULE_REQUESTED','RESCHEDULED','CANCELLED','CHECKED_IN','IN_MEETING','CHECKED_OUT','COMPLETED','NO_SHOW','EXPIRED'))
);
CREATE INDEX idx_appointment_host_queue ON appointments(host_employee_id, status, starts_at);
CREATE UNIQUE INDEX uq_active_host_slot ON appointments(host_employee_id, starts_at)
    WHERE status IN ('PENDING_VERIFICATION','PENDING_APPROVAL','APPROVED','CHECKED_IN','IN_MEETING');

CREATE TABLE appointment_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL REFERENCES appointments(id),
    from_status VARCHAR(40),
    to_status VARCHAR(40) NOT NULL,
    actor_id UUID REFERENCES user_accounts(id),
    remarks VARCHAR(500),
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_appointment_history ON appointment_status_history(appointment_id, changed_at);

CREATE TABLE visitor_badges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    badge_number VARCHAR(40) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    allocated BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE visit_access_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL UNIQUE REFERENCES appointments(id),
    visitor_id UUID NOT NULL REFERENCES visitors(id),
    badge_id UUID REFERENCES visitor_badges(id),
    entry_gate VARCHAR(80),
    exit_gate VARCHAR(80),
    checked_in_at TIMESTAMPTZ,
    checked_out_at TIMESTAMPTZ,
    checked_in_by UUID REFERENCES user_accounts(id),
    checked_out_by UUID REFERENCES user_accounts(id),
    override_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (checked_out_at IS NULL OR checked_in_at IS NOT NULL),
    CHECK (checked_out_at IS NULL OR checked_out_at >= checked_in_at)
);
CREATE INDEX idx_access_inside ON visit_access_records(checked_in_at, checked_out_at);

CREATE TABLE compensation_packages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id),
    basic NUMERIC(19,2) NOT NULL CHECK (basic >= 0),
    hra NUMERIC(19,2) NOT NULL CHECK (hra >= 0),
    allowances NUMERIC(19,2) NOT NULL CHECK (allowances >= 0),
    deductions NUMERIC(19,2) NOT NULL CHECK (deductions >= 0),
    gross NUMERIC(19,2) NOT NULL CHECK (gross >= 0),
    net NUMERIC(19,2) NOT NULL CHECK (net >= 0),
    annual_ctc NUMERIC(19,2) NOT NULL CHECK (annual_ctc >= 0),
    currency CHAR(3) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(20) NOT NULL,
    proposed_by UUID NOT NULL REFERENCES user_accounts(id),
    approved_by UUID REFERENCES user_accounts(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (effective_to IS NULL OR effective_to >= effective_from),
    CHECK (status IN ('PROPOSED','APPROVED','REJECTED'))
);
CREATE INDEX idx_compensation_employee_dates ON compensation_packages(employee_id, effective_from, effective_to);

CREATE TABLE audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    actor_user_id UUID,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100),
    reason VARCHAR(500),
    correlation_id VARCHAR(100),
    sensitivity VARCHAR(30) NOT NULL,
    ip_address VARCHAR(64),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_entity ON audit_events(entity_type, entity_id, occurred_at DESC);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    payload_json JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (status IN ('PENDING','PROCESSING','SENT','DEAD'))
);
CREATE INDEX idx_outbox_dispatch ON outbox_events(status, available_at);

CREATE TABLE system_configuration (
    config_key VARCHAR(120) PRIMARY KEY,
    value_json JSONB NOT NULL,
    value_type VARCHAR(30) NOT NULL,
    public_value BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    updated_by UUID REFERENCES user_accounts(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO permissions(code, description) VALUES
('EMPLOYEE_CREATE','Create employees'),
('EMPLOYEE_READ','Read employee profiles'),
('EMPLOYEE_UPDATE','Update employee profiles'),
('EMPLOYEE_STATUS_CHANGE','Change employment status'),
('SALARY_READ','Read compensation'),
('SALARY_WRITE','Propose compensation'),
('SALARY_APPROVE','Approve compensation'),
('DEPARTMENT_MANAGE','Manage organization structure'),
('DESIGNATION_MANAGE','Manage designations'),
('ROLE_MANAGE','Manage roles and permissions'),
('APPOINTMENT_REQUEST','Request appointments'),
('APPOINTMENT_APPROVE','Approve assigned appointments'),
('APPOINTMENT_REJECT','Reject assigned appointments'),
('APPOINTMENT_RESCHEDULE','Reschedule appointments'),
('CEO_APPOINTMENT_APPROVE','Approve CEO appointments'),
('VISITOR_REGISTER','Register visitors and walk-ins'),
('VISITOR_VERIFY','Verify visitor identity'),
('VISITOR_CHECK_IN','Check visitors in'),
('VISITOR_CHECK_OUT','Check visitors out'),
('REPORT_VIEW','View reports'),
('AUDIT_VIEW','View audit records'),
('SYSTEM_CONFIGURE','Manage system configuration');

INSERT INTO roles(code, name, system_role) VALUES
('ROLE_CEO','Chief Executive Officer',TRUE),
('ROLE_HR_ADMIN','HR Administrator',TRUE),
('ROLE_HR_EXECUTIVE','HR Executive',TRUE),
('ROLE_EMPLOYEE','Employee',TRUE),
('ROLE_RECEPTIONIST','Receptionist',TRUE),
('ROLE_SECURITY','Security',TRUE),
('ROLE_SYSTEM_ADMIN','System Administrator',TRUE);

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ROLE_CEO' AND p.code <> 'SYSTEM_CONFIGURE';

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN
('EMPLOYEE_CREATE','EMPLOYEE_READ','EMPLOYEE_UPDATE','EMPLOYEE_STATUS_CHANGE','SALARY_READ','SALARY_WRITE',
 'SALARY_APPROVE','DEPARTMENT_MANAGE','DESIGNATION_MANAGE','APPOINTMENT_APPROVE','APPOINTMENT_REJECT',
 'APPOINTMENT_RESCHEDULE','VISITOR_REGISTER','VISITOR_VERIFY','REPORT_VIEW','AUDIT_VIEW')
WHERE r.code = 'ROLE_HR_ADMIN';

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN
('EMPLOYEE_CREATE','EMPLOYEE_READ','EMPLOYEE_UPDATE','APPOINTMENT_APPROVE','APPOINTMENT_REJECT',
 'APPOINTMENT_RESCHEDULE','VISITOR_REGISTER','VISITOR_VERIFY','REPORT_VIEW')
WHERE r.code = 'ROLE_HR_EXECUTIVE';

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN
('EMPLOYEE_READ','APPOINTMENT_REQUEST','APPOINTMENT_APPROVE','APPOINTMENT_REJECT','APPOINTMENT_RESCHEDULE')
WHERE r.code = 'ROLE_EMPLOYEE';

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN
('EMPLOYEE_READ','VISITOR_REGISTER','VISITOR_VERIFY','VISITOR_CHECK_IN','VISITOR_CHECK_OUT','REPORT_VIEW')
WHERE r.code = 'ROLE_RECEPTIONIST';

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN
('VISITOR_VERIFY','VISITOR_CHECK_IN','VISITOR_CHECK_OUT','REPORT_VIEW')
WHERE r.code = 'ROLE_SECURITY';

INSERT INTO role_permissions(role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN
('ROLE_MANAGE','SYSTEM_CONFIGURE','AUDIT_VIEW')
WHERE r.code = 'ROLE_SYSTEM_ADMIN';

INSERT INTO branches(code, name, city) VALUES ('HYD', 'BrainServe Hyderabad', 'Hyderabad');
INSERT INTO departments(branch_id, code, name)
SELECT id, 'EXEC', 'Executive Office' FROM branches WHERE code = 'HYD';
INSERT INTO departments(branch_id, code, name)
SELECT id, 'HR', 'Human Resources' FROM branches WHERE code = 'HYD';
INSERT INTO departments(branch_id, code, name)
SELECT id, 'ENG', 'Engineering' FROM branches WHERE code = 'HYD';
INSERT INTO designations(department_id, name, level)
SELECT id, 'Chief Executive Officer', 10 FROM departments WHERE code = 'EXEC';
INSERT INTO designations(department_id, name, level)
SELECT id, 'HR Administrator', 6 FROM departments WHERE code = 'HR';
INSERT INTO designations(department_id, name, level)
SELECT id, 'Software Engineer', 3 FROM departments WHERE code = 'ENG';

INSERT INTO visitor_badges(badge_number) VALUES ('BS-001'), ('BS-002'), ('BS-003'), ('BS-004'), ('BS-005');

INSERT INTO system_configuration(config_key, value_json, value_type, public_value) VALUES
('office.timezone','"Asia/Kolkata"','STRING',TRUE),
('appointment.slotMinutes','30','INTEGER',TRUE),
('appointment.bufferMinutes','10','INTEGER',FALSE),
('visitor.consentVersion','"2026-07"','STRING',TRUE),
('salary.makerChecker','true','BOOLEAN',FALSE);
