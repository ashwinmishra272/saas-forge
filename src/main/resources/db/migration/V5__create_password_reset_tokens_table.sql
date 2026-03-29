CREATE TABLE IF NOT EXISTS password_reset_tokens (
                                                     id          BIGSERIAL PRIMARY KEY,
                                                     token       VARCHAR(255) NOT NULL UNIQUE,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    expires_at  TIMESTAMP NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE
    );
