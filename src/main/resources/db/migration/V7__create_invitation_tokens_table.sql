CREATE TABLE IF NOT EXISTS invitation_tokens
(
    id                  BIGSERIAL PRIMARY KEY,
    token               VARCHAR(255) NOT NULL UNIQUE,
    invited_email       VARCHAR(255) NOT NULL,
    tenant_id           BIGINT       NOT NULL REFERENCES tenants (id),
    role_id             BIGINT       NOT NULL REFERENCES roles (id),
    invited_by_user_id  BIGINT       NOT NULL REFERENCES users (id),
    expires_at          TIMESTAMP    NOT NULL,
    accepted            BOOLEAN      NOT NULL DEFAULT FALSE
);
