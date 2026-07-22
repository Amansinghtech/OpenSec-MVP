CREATE TABLE alerts
(
    id              UUID                        NOT NULL,
    tenant_id       UUID                        NOT NULL,
    correlation_id  VARCHAR(255),
    title           VARCHAR(255)                NOT NULL,
    description     VARCHAR(2000)               NOT NULL,
    entity_type     VARCHAR(20)                 NOT NULL,
    entity_id       VARCHAR(255)                NOT NULL,
    risk_score      INTEGER                     NOT NULL,
    severity        VARCHAR(20)                 NOT NULL,
    status          VARCHAR(20)                 NOT NULL,
    signal_ids      VARCHAR(4000)               NOT NULL,
    cve_ids         VARCHAR(2000),
    created_at      TIMESTAMP WITHOUT TIME ZONE,
    updated_at      TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_alerts PRIMARY KEY (id)
);

CREATE INDEX idx_alerts_tenant ON alerts (tenant_id);
CREATE INDEX idx_alerts_status ON alerts (status);
