CREATE TABLE agent_commands
(
    id          UUID                        NOT NULL,
    agent_id    UUID                        NOT NULL,
    tenant_id   UUID                        NOT NULL,
    type        VARCHAR(40)                 NOT NULL,
    status      VARCHAR(20)                 NOT NULL,
    script      VARCHAR(255),
    parameters  VARCHAR(2000),
    message     VARCHAR(2000),
    created_at  TIMESTAMP WITHOUT TIME ZONE,
    updated_at  TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_agent_commands PRIMARY KEY (id)
);

CREATE INDEX idx_agent_commands_agent ON agent_commands (agent_id);
