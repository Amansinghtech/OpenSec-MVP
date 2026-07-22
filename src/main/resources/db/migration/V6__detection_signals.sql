CREATE TABLE detection_signals
(
    id             UUID         NOT NULL,
    tenant_id      UUID         NOT NULL,
    correlation_id VARCHAR(255),
    detector       VARCHAR(40)  NOT NULL,
    signal_type    VARCHAR(60)  NOT NULL,
    entity_type    VARCHAR(20)  NOT NULL,
    entity_id      VARCHAR(255) NOT NULL,
    risk_score     INTEGER      NOT NULL,
    severity       VARCHAR(10)  NOT NULL,
    reason         VARCHAR(512) NOT NULL,
    source_ref     VARCHAR(255),
    occurred_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_detection_signals PRIMARY KEY (id)
);

CREATE INDEX idx_signals_tenant_entity ON detection_signals (tenant_id, entity_type, entity_id);
CREATE INDEX idx_signals_severity ON detection_signals (severity);
