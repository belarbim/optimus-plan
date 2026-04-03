-- Employee type history
CREATE TABLE employee_type_history (
    id             UUID PRIMARY KEY,
    employee_id    UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    type           VARCHAR(20) NOT NULL,
    effective_from DATE NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    UNIQUE(employee_id, effective_from)
);

-- Seed from existing employee.type
INSERT INTO employee_type_history (id, employee_id, type, effective_from, created_at)
SELECT gen_random_uuid(), id, COALESCE(type, 'INTERNAL'), '2020-01-01', NOW()
FROM employee;

-- Grade cost history
CREATE TABLE grade_cost_history (
    id             UUID PRIMARY KEY,
    grade_id       UUID NOT NULL REFERENCES grade(id) ON DELETE CASCADE,
    daily_cost     DECIMAL(10,2) NOT NULL,
    effective_from DATE NOT NULL,
    created_at     TIMESTAMP NOT NULL,
    UNIQUE(grade_id, effective_from)
);

-- Seed from existing grade.daily_cost
INSERT INTO grade_cost_history (id, grade_id, daily_cost, effective_from, created_at)
SELECT gen_random_uuid(), id, daily_cost, '2020-01-01', NOW()
FROM grade;
