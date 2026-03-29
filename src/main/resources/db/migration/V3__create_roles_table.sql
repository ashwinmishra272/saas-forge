CREATE TABLE IF NOT EXISTS roles (
                                     id          BIGSERIAL PRIMARY KEY,
                                     name        VARCHAR(255) NOT NULL,
    role_key    VARCHAR(255),
    tenant_id   BIGINT NOT NULL REFERENCES tenants(id),
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(255),
    updated_by  VARCHAR(255)
    );
