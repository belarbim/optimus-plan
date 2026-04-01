CREATE TABLE team_type (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE team_type_category (
    id                          UUID PRIMARY KEY,
    team_type_id                UUID NOT NULL REFERENCES team_type(id) ON DELETE CASCADE,
    name                        VARCHAR(255) NOT NULL,
    is_part_of_total_capacity   BOOLEAN NOT NULL DEFAULT FALSE,
    is_part_of_remaining_capacity BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE(team_type_id, name),
    CONSTRAINT chk_category_flag CHECK (
        (is_part_of_total_capacity AND NOT is_part_of_remaining_capacity)
        OR (is_part_of_remaining_capacity AND NOT is_part_of_total_capacity)
    )
);

ALTER TABLE team ADD COLUMN team_type_id UUID REFERENCES team_type(id);
