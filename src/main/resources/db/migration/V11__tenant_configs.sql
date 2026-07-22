CREATE TABLE tenant_configs
(
    id          UUID                        NOT NULL,
    tenant_id   UUID                        NOT NULL,
    config_key  VARCHAR(100)                NOT NULL,
    config_json VARCHAR(10000)              NOT NULL,
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_tenant_configs PRIMARY KEY (id)
);

CREATE UNIQUE INDEX uq_tenant_configs_key ON tenant_configs (tenant_id, config_key);
