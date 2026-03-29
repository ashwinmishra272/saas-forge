CREATE TABLE IF NOT EXISTS tenants
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    name
    VARCHAR
(
    255
) NOT NULL,
    tenant_key VARCHAR
(
    255
) NOT NULL UNIQUE,
    status VARCHAR
(
    50
),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by VARCHAR
(
    255
),
    updated_by VARCHAR
(
    255
)
    );
