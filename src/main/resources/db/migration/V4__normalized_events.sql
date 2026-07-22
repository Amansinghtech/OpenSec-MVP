CREATE TABLE normalized_events
(
    id             UUID            NOT NULL,
    tenant_id      UUID            NOT NULL,
    correlation_id VARCHAR(255),
    source         VARCHAR(50)     NOT NULL,
    event_type     VARCHAR(40)     NOT NULL,
    category       VARCHAR(255),
    severity       INTEGER,
    host           VARCHAR(255),
    entity_type    VARCHAR(20)     NOT NULL,
    entity_id      VARCHAR(255)    NOT NULL,
    occurred_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    attributes     VARCHAR(1000000) NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_normalized_events PRIMARY KEY (id)
);

CREATE INDEX idx_norm_tenant_entity ON normalized_events (tenant_id, entity_type, entity_id);
CREATE INDEX idx_norm_event_type ON normalized_events (event_type);
