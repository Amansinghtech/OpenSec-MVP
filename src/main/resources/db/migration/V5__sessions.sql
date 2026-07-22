CREATE TABLE sessions
(
    id                 UUID            NOT NULL,
    tenant_id          UUID            NOT NULL,
    entity_type        VARCHAR(20)     NOT NULL,
    entity_id          VARCHAR(255)    NOT NULL,
    status             VARCHAR(10)     NOT NULL,
    started_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_event_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    event_count        INTEGER         NOT NULL,
    auth_failure_count INTEGER         NOT NULL,
    suspicious         BOOLEAN         NOT NULL,
    risk_reason        VARCHAR(512),
    sequence_json      VARCHAR(1000000) NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE,
    updated_at         TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_sessions PRIMARY KEY (id)
);

CREATE INDEX idx_sessions_lookup ON sessions (tenant_id, entity_type, entity_id, status);
