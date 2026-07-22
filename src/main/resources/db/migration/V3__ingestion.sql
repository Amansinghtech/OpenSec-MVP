CREATE TABLE raw_logs
(
    id             UUID            NOT NULL,
    source         VARCHAR(50)     NOT NULL,
    event_id       VARCHAR(255),
    host           VARCHAR(255),
    tenant_id      UUID            NOT NULL,
    correlation_id VARCHAR(255),
    severity       INTEGER,
    category       VARCHAR(255),
    occurred_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    payload        VARCHAR(1000000) NOT NULL,
    received_at    TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_raw_logs PRIMARY KEY (id)
);

CREATE INDEX idx_raw_logs_tenant ON raw_logs (tenant_id);
CREATE INDEX idx_raw_logs_source ON raw_logs (source);

-- Idempotency: a given (tenant, source, event_id) is ingested once. NULL event_ids are treated as
-- distinct, so events without an external id are never deduped.
CREATE UNIQUE INDEX uq_raw_logs_dedupe ON raw_logs (tenant_id, source, event_id);
