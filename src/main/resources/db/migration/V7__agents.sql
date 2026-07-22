CREATE TABLE agents
(
    id                 UUID                        NOT NULL,
    tenant_id          UUID                        NOT NULL,
    name               VARCHAR(255)                NOT NULL,
    hostname           VARCHAR(255),
    platform           VARCHAR(20)                 NOT NULL,
    status             VARCHAR(20)                 NOT NULL,
    api_key_prefix     VARCHAR(16)                 NOT NULL,
    api_key_hash       VARCHAR(255)                NOT NULL,
    wazuh_agent_group  VARCHAR(255),
    wazuh_agent_id     VARCHAR(64),
    last_heartbeat_at  TIMESTAMP WITHOUT TIME ZONE,
    created_at         TIMESTAMP WITHOUT TIME ZONE,
    updated_at         TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_agents PRIMARY KEY (id)
);

CREATE INDEX idx_agents_tenant ON agents (tenant_id);
CREATE INDEX idx_agents_api_key_prefix ON agents (api_key_prefix);
CREATE UNIQUE INDEX uq_agents_tenant_name ON agents (tenant_id, name);
