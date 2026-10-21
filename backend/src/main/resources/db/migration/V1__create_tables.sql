-- V1: create all tables.
-- Flyway runs this ONCE and records it in a table called flyway_schema_history.
-- Rule: never edit a migration that has already run. To change the schema, add V3, V4...

CREATE TABLE departments (
    id    BIGSERIAL PRIMARY KEY,           -- auto-incrementing id: 1, 2, 3...
    name  VARCHAR(100) NOT NULL UNIQUE     -- no two departments with the same name
);

CREATE TABLE employees (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,         -- used to log in, so must be unique
    password_hash  VARCHAR(100) NOT NULL,                -- BCrypt hash, never the real password
    role           VARCHAR(20)  NOT NULL
                   CHECK (role IN ('ADMIN', 'MANAGER', 'EMPLOYEE')),
    department_id  BIGINT REFERENCES departments(id),    -- foreign key
    manager_id     BIGINT REFERENCES employees(id),      -- self-reference: manager is an employee too
    leave_balance  INT     NOT NULL DEFAULT 20 CHECK (leave_balance >= 0),
    active         BOOLEAN NOT NULL DEFAULT TRUE,        -- "soft delete": deactivate, never erase
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE leave_requests (
    id           BIGSERIAL PRIMARY KEY,
    employee_id  BIGINT NOT NULL REFERENCES employees(id),
    start_date   DATE   NOT NULL,
    end_date     DATE   NOT NULL,
    reason       VARCHAR(500),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                 CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    decided_by   BIGINT REFERENCES employees(id),        -- NULL until someone decides
    decided_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (end_date >= start_date)                       -- the database itself blocks impossible dates
);

CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    actor_id    BIGINT REFERENCES employees(id),         -- who did it (NULL = system)
    action      VARCHAR(50)  NOT NULL,                   -- e.g. LEAVE_APPROVED
    entity      VARCHAR(50)  NOT NULL,                   -- e.g. LeaveRequest
    entity_id   BIGINT,                                  -- e.g. 5
    details     TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes make lookups by these columns fast (like a book's index).
CREATE INDEX idx_employees_manager      ON employees(manager_id);
CREATE INDEX idx_employees_department   ON employees(department_id);
CREATE INDEX idx_leave_requests_employee ON leave_requests(employee_id);
CREATE INDEX idx_leave_requests_status   ON leave_requests(status);
CREATE INDEX idx_audit_logs_created      ON audit_logs(created_at);
