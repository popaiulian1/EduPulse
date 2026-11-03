-- Runs in the "auth" schema (spring.flyway.default-schema).
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(50)  NOT NULL,
    org_unit      VARCHAR(100) NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT ck_users_role CHECK (role IN ('ROLE_ADMIN', 'ROLE_USER'))
);

CREATE INDEX idx_users_org_unit ON users (org_unit);
