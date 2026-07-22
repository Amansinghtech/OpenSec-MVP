CREATE TABLE tenants
(
    id         UUID         NOT NULL,
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(255) NOT NULL,
    enabled    BOOLEAN      NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT uc_tenants_slug UNIQUE (slug)
);

CREATE TABLE roles
(
    id          UUID        NOT NULL,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uc_roles_name UNIQUE (name)
);

CREATE TABLE permissions
(
    id          UUID         NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uc_permissions_name UNIQUE (name)
);

CREATE TABLE role_permissions
(
    role_id       UUID NOT NULL,
    permission_id UUID NOT NULL,
    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

ALTER TABLE users
    ADD tenant_id UUID;

ALTER TABLE users
    ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants (id);

CREATE TABLE user_roles
(
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens
(
    id         UUID         NOT NULL,
    token      VARCHAR(255) NOT NULL,
    user_id    UUID         NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    revoked    BOOLEAN      NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uc_refresh_tokens_token UNIQUE (token),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_user ON refresh_tokens (user_id);

CREATE TABLE revoked_access_tokens
(
    jti        UUID NOT NULL,
    expires_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_revoked_access_tokens PRIMARY KEY (jti)
);

CREATE TABLE auth_audit_log
(
    id         UUID        NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    username   VARCHAR(255),
    tenant_id  UUID,
    ip_address VARCHAR(45),
    user_agent VARCHAR(512),
    detail     VARCHAR(512),
    created_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_auth_audit_log PRIMARY KEY (id)
);

CREATE INDEX idx_audit_username ON auth_audit_log (username);
CREATE INDEX idx_audit_event_type ON auth_audit_log (event_type);
