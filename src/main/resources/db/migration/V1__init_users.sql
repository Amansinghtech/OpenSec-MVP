CREATE TABLE users
(
    id         UUID                        NOT NULL,
    username   VARCHAR(255)                NOT NULL,
    email      VARCHAR(255),
    phone      VARCHAR(15)                 NOT NULL,
    full_name  VARCHAR(255),
    password   VARCHAR(255)                NOT NULL,
    enabled    BOOLEAN                     NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uc_users_username UNIQUE (username)
);

CREATE INDEX idx_user_username ON users (username);
CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_phone ON users (phone);
