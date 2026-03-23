-- Team table with self-referential parent_id
CREATE TABLE team (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    parent_id UUID REFERENCES team(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(name, parent_id)
);

-- Employee table
CREATE TABLE employee (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Team assignment table
CREATE TABLE team_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES team(id),
    employee_id UUID NOT NULL REFERENCES employee(id),
    allocation_pct DECIMAL(5,2) NOT NULL CHECK (allocation_pct >= 1 AND allocation_pct <= 100),
    start_date DATE NOT NULL,
    end_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(team_id, employee_id, start_date)
);

-- Role type config table
CREATE TABLE role_type_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_type VARCHAR(50) NOT NULL UNIQUE,
    default_weight DECIMAL(3,2) NOT NULL CHECK (default_weight >= 0 AND default_weight <= 1),
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Role history table
CREATE TABLE role_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES team_assignment(id) ON DELETE CASCADE,
    role_type VARCHAR(50) NOT NULL,
    role_weight DECIMAL(3,2) NOT NULL CHECK (role_weight >= 0 AND role_weight <= 1),
    effective_from DATE NOT NULL,
    effective_to DATE
);

-- Category allocation table
CREATE TABLE category_allocation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES team(id) ON DELETE CASCADE,
    category_name VARCHAR(255) NOT NULL,
    allocation_pct DECIMAL(5,2) NOT NULL CHECK (allocation_pct >= 0 AND allocation_pct <= 100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(team_id, category_name)
);

-- Working days config table
CREATE TABLE working_days_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    month VARCHAR(7) NOT NULL UNIQUE,
    avg_days_worked DECIMAL(5,2) NOT NULL CHECK (avg_days_worked > 0),
    imported_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Capacity snapshot table
CREATE TABLE capacity_snapshot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES team(id) ON DELETE CASCADE,
    snapshot_month VARCHAR(7) NOT NULL,
    category_name VARCHAR(255) NOT NULL,
    capacity_man_days DECIMAL(10,3) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(team_id, snapshot_month, category_name)
);

-- Public holiday table
CREATE TABLE public_holiday (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    name VARCHAR(255) NOT NULL,
    locale VARCHAR(5) NOT NULL DEFAULT 'FR',
    recurring BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(date, locale)
);

-- Capacity alert table
CREATE TABLE capacity_alert (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    team_id UUID NOT NULL REFERENCES team(id) ON DELETE CASCADE UNIQUE,
    threshold_man_days DECIMAL(10,3) NOT NULL CHECK (threshold_man_days > 0),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Audit log table
CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(50) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(10) NOT NULL,
    changes JSONB,
    actor VARCHAR(100) NOT NULL DEFAULT 'manager',
    timestamp TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_timestamp ON audit_log(timestamp);
CREATE INDEX idx_audit_actor ON audit_log(actor);
