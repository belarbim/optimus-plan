ALTER TABLE employee ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'INTERNAL';

CREATE TABLE grade (
    id          UUID PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    daily_cost  DECIMAL(10,2) NOT NULL,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE employee_grade_history (
    id             UUID PRIMARY KEY,
    employee_id    UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    grade_id       UUID NOT NULL REFERENCES grade(id),
    effective_from DATE NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    UNIQUE(employee_id, effective_from)
);

CREATE TABLE employee_cost_history (
    id             UUID PRIMARY KEY,
    employee_id    UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    daily_cost     DECIMAL(10,2) NOT NULL,
    effective_from DATE NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    UNIQUE(employee_id, effective_from)
);
