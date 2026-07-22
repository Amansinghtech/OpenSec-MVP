CREATE TABLE defense_actions
(
    id              UUID                        NOT NULL,
    tenant_id       UUID                        NOT NULL,
    alert_id        UUID                        NOT NULL,
    correlation_id  VARCHAR(255),
    action_type     VARCHAR(40)                 NOT NULL,
    status          VARCHAR(20)                 NOT NULL,
    target          VARCHAR(255)                NOT NULL,
    detail          VARCHAR(2000)               NOT NULL,
    dry_run         BOOLEAN                     NOT NULL,
    executed_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_defense_actions PRIMARY KEY (id)
);

CREATE INDEX idx_defense_actions_tenant ON defense_actions (tenant_id);
