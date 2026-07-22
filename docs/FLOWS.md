# OPENSEC — Architecture & Flow Diagrams

Visual reference for how data, auth, and control flow through the platform.
All API paths are prefixed with `/api/v1`.

> Related: [`BUILD_PLAN.md`](./BUILD_PLAN.md) · [`PRD.md`](./PRD.md) · [`AGENT_ENROLLMENT.md`](./AGENT_ENROLLMENT.md) · [`PLUGINS.md`](./PLUGINS.md)

---

## 1. System overview

OPENSEC is a Spring Boot monolith with an event-driven pipeline. Wazuh agents collect endpoint telemetry; OPENSEC adds intelligence, alerting, and automated response on top.

```mermaid
flowchart TB
    subgraph Endpoints["Endpoint layer (Wazuh)"]
        WA[Wazuh agents<br/>Linux / Windows / macOS]
        WM[Wazuh manager]
        WA --> WM
    end

    subgraph OPENSEC["OPENSEC platform"]
        GW[API Gateway<br/>JWT + API key + tenant context]
        ING[Ingestion]
        NORM[Normalization]
        SESS[Session reconstruction]
        DET[Detection engine]
        RAG[RAG enrichment]
        ALT[Alert service]
        DEF[Defense engine]
        SRCH[Search indexer]
        CFG[Config service]
        UI[Web UI<br/>Next.js]
    end

    subgraph Infra["Infrastructure"]
        PG[(PostgreSQL)]
        RD[(Redis)]
        KF{{Kafka}}
        OS[(OpenSearch)]
    end

    WM -->|webhook / integrator| GW
    UI -->|REST + JWT| GW
    GW --> ING
    ING --> KF
    KF --> NORM --> SESS --> DET
    DET --> RAG --> ALT --> DEF
    KF -.->|async index| SRCH
    SRCH --> OS
    ING & NORM & SESS & DET & ALT & DEF --> PG
    GW --> RD
    CFG --> PG
    CFG --> RD
```

---

## 2. End-to-end telemetry pipeline

Every ingested log travels through Kafka topics. Each stage is a separate consumer group, so stages scale independently.

```mermaid
flowchart LR
    subgraph Ingest["Phase 5–6"]
        A1[POST /ingest/logs<br/>POST /ingest/wazuh] --> A2[IngestionService]
        A2 --> A3[(raw_logs<br/>Postgres)]
        A2 -->|raw_log.ingested| T1[[raw_logs]]
    end

    subgraph Normalize["Phase 7"]
        T1 --> B1[RawLogKafkaListener]
        B1 --> B2[NormalizationService<br/>WazuhLogParser / GenericLogParser]
        B2 --> B3[(normalized_events)]
        B2 -->|normalized event| T2[[normalized_events]]
    end

    subgraph Sessions["Phase 8"]
        T2 --> C1[NormalizedEventKafkaListener]
        C1 --> C2[SessionReconstructionService<br/>sliding window + rules]
        C2 --> C3[(sessions)]
        C2 -->|session_updated<br/>suspicious_session_detected| T3[[session_events]]
    end

    subgraph Detection["Phase 9"]
        T2 --> D1[DetectionKafkaListener]
        T3 --> D1
        D1 --> D2[DetectionEngine<br/>Signature / Anomaly / Behavioral]
        D2 --> D3[(detection_signals)]
        D2 -->|detection_signal| T4[[detection_signals]]
    end

    subgraph Enrich["Phase 11"]
        T4 --> E1[RagKafkaListener]
        E1 --> E2[RagEnrichmentService<br/>vector search + LLM]
        E2 -->|contextualized_detection| T5[[contextualized_detections]]
    end

    subgraph Alerts["Phase 12"]
        T4 --> F1[AlertKafkaListener]
        T5 --> F1
        F1 --> F2[AlertService<br/>aggregate + risk score]
        F2 --> F3[(alerts)]
        F2 -->|alert_created| T6[[alert_events]]
    end

    subgraph Defense["Phase 14"]
        T6 --> G1[DefenseKafkaListener]
        G1 --> G2[DefenseEngine<br/>dry-run + executors]
        G2 --> G3[(defense_actions)]
        G2 -->|defense_action_executed| T7[[defense_events]]
    end

    subgraph Search["Phase 10"]
        T2 -.-> H1[SearchIndexKafkaListener]
        T3 -.-> H1
        T4 -.-> H1
        H1 --> H2[(OpenSearch)]
    end
```

### Kafka topics summary

| Topic | Producer | Key consumers |
|-------|----------|---------------|
| `raw_logs` | IngestionService | Normalization |
| `normalized_events` | NormalizationService | Sessions, Detection, Search |
| `session_events` | SessionReconstructionService | Detection, Search |
| `detection_signals` | DetectionEngine | RAG, Alerts, Search |
| `contextualized_detections` | RagEnrichmentService | Alerts |
| `alert_events` | AlertService | Defense |
| `defense_events` | DefenseEngine | (audit / integrations) |

All records are keyed by `tenant_id` for per-tenant ordering.

---

## 3. HTTP request & auth flow

Every authenticated request passes through a filter chain before reaching controllers.

```mermaid
sequenceDiagram
    participant Client
    participant Corr as CorrelationIdFilter
    participant Agent as AgentApiKeyAuthFilter
    participant JWT as JWTAuthFilter
    participant Tenant as TenantContextFilter
    participant Rate as RateLimitingFilter
    participant Ctrl as Controller / Service

    Client->>Corr: HTTP request
    Corr->>Corr: Set X-Correlation-Id + MDC

    alt API key present (X-Api-Key or Authorization: ApiKey)
        Corr->>Agent: forward
        Agent->>Agent: Lookup agent by prefix + bcrypt verify
        Agent->>Agent: Set AgentPrincipal (ROLE_AGENT)
    else Bearer JWT present
        Corr->>Agent: forward (skip if already authed)
        Agent->>JWT: forward
        JWT->>JWT: Validate token + check revocation
        JWT->>JWT: Set AuthenticatedUser (roles + permissions)
    end

    JWT->>Tenant: forward
    Tenant->>Tenant: Bind tenantId → RequestContext + MDC
    Tenant->>Rate: forward
    Rate->>Ctrl: forward (if within rate limit)
    Ctrl->>Ctrl: @PreAuthorize check
    Ctrl-->>Client: JSON response
```

### Auth methods

| Caller | Header | Principal | Typical endpoints |
|--------|--------|-----------|-----------------|
| Human (UI / admin) | `Authorization: Bearer <jwt>` | `AuthenticatedUser` | `/alerts`, `/agents`, `/admin/*` |
| Enrolled agent | `X-Api-Key: opsk_…` or `Authorization: ApiKey opsk_…` | `AgentPrincipal` | `/ingest/*`, `/agents/heartbeat` |

---

## 4. Agent enrollment & telemetry flow

Per [ADR-001](./BUILD_PLAN.md#adr-001-wazuh-as-the-endpoint--collection-layer), Wazuh collects endpoint data; OPENSEC manages fleet identity and API credentials.

```mermaid
sequenceDiagram
    participant Admin
    participant OPENSEC
    participant Wazuh as Wazuh manager
    participant EP as Endpoint agent

    Note over Admin,OPENSEC: Enrollment (one-time)
    Admin->>OPENSEC: POST /agents (JWT, AGENT_WRITE)
    OPENSEC->>OPENSEC: Generate opsk_ API key (bcrypt hash stored)
    OPENSEC-->>Admin: agent record + apiKey (shown once)

    Note over EP,Wazuh: Wazuh rollout
    EP->>Wazuh: agent-auth + connect
    Wazuh->>Wazuh: Assign agent group → tenant mapping

    Note over EP,OPENSEC: Ongoing telemetry
    EP->>Wazuh: logs / FIM / alerts
    Wazuh->>OPENSEC: POST /ingest/wazuh (ApiKey header)
    OPENSEC->>OPENSEC: Stamp tenant_id from AgentPrincipal
    OPENSEC->>OPENSEC: Dedupe → raw_logs → Kafka

    loop Every N minutes
        OPENSEC->>OPENSEC: POST /agents/heartbeat (ApiKey)
        OPENSEC->>OPENSEC: status = ACTIVE, update lastHeartbeatAt
    end

    Note over Admin,OPENSEC: Active response (Phase 13–14)
    Admin->>OPENSEC: POST /agents/{id}/commands
    OPENSEC->>Wazuh: WazuhCommandAdapter (active-response)
    Wazuh->>EP: execute script / isolate
```

---

## 5. Detection → alert → defense flow

How a suspicious event becomes an operator-visible alert and optionally triggers automated response.

```mermaid
flowchart TD
    NE[Normalized event<br/>or suspicious session] --> DE{DetectionEngine}
    DE --> SIG{Detector plugins}
    SIG --> S1[SignatureDetector]
    SIG --> S2[AnomalyDetector]
    SIG --> S3[BehavioralDetector]
    S1 & S2 & S3 --> SCORE{riskScore ≥ emitThreshold?}
    SCORE -->|no| DROP[Suppressed]
    SCORE -->|yes| DS[(detection_signal<br/>Postgres + Kafka)]

    DS --> RAG{RAG enabled?}
    RAG -->|yes| CVE[Vector search + CVE mapping<br/>contextualized_detection]
    RAG -->|no| SKIP[Skip enrichment]
    CVE --> AS[AlertService]
    SKIP --> AS
    DS --> AS

    AS --> AL[(alert<br/>NEW status)]
    AL --> AE{alert_created<br/>Kafka}

    AE --> DF{DefenseEngine<br/>riskScore ≥ minRiskScore?}
    DF -->|no| END1[No action]
    DF -->|yes| POL{Select actions by severity}
    POL --> A1[BLOCK_IP]
    POL --> A2[REVOKE_TOKENS]
    POL --> A3[DISABLE_USER]
    A1 & A2 & A3 --> DRY{dryRun?}
    DRY -->|yes| LOG[Log + audit<br/>status = DRY_RUN]
    DRY -->|no| EXEC[Execute + audit<br/>status = EXECUTED]
```

### Alert lifecycle

```mermaid
stateDiagram-v2
    [*] --> NEW: detection_signal aggregated
    NEW --> ACKNOWLEDGED: PATCH /alerts/{id}/status
    ACKNOWLEDGED --> RESOLVED: analyst closes
    NEW --> SUPPRESSED: noise / false positive
    ACKNOWLEDGED --> SUPPRESSED
    RESOLVED --> [*]
    SUPPRESSED --> [*]
```

---

## 6. Multi-tenancy model

Tenant isolation is enforced at every layer — never from client-supplied payload fields.

```mermaid
flowchart TB
  subgraph Sources["Data sources"]
    T1[Tenant A<br/>Wazuh group: production]
    T2[Tenant B<br/>Wazuh group: staging]
  end

  subgraph Auth["Identity"]
  T1 --> A1[Admin user JWT<br/>tenantId in claims]
  T2 --> A2[Admin user JWT<br/>tenantId in claims]
  T1 --> K1[Agent API key<br/>tenantId on agent row]
  end

  subgraph Pipeline["Pipeline"]
  A1 & K1 --> CTX[RequestContext.tenantId]
  CTX --> ALL[All DB writes + Kafka keys<br/>scoped to tenant]
  end

  subgraph Storage["Storage"]
  ALL --> PG[(Postgres rows<br/>tenant_id column)]
  ALL --> KF[Kafka partition key<br/>= tenant_id]
  ALL --> OS[OpenSearch query filter<br/>tenantId term]
  end
```

---

## 7. Plugin architecture

Detection rules and defense actions are pluggable `@Component` beans discovered at startup.

```mermaid
flowchart LR
    subgraph Author["Plugin author"]
        P1[@Component<br/>MyDetector]
        P2[@Component<br/>MyDefenseAction]
    end

    subgraph Core["OPENSEC core"]
        REG[PluginRegistry]
        DE[DetectionEngine]
        DF[DefenseEngine]
    end

    P1 -->|implements Detector| DE
    P2 -->|implements DefenseExecutor| DF
    P1 & P2 --> REG
    REG --> API[GET /admin/plugins]
```

See [`PLUGINS.md`](./PLUGINS.md) for authoring instructions.

---

## 8. Operator UI flow

The Next.js console (`ui/`) talks to the same REST API as integrations.

```mermaid
sequenceDiagram
    participant Op as Operator browser
    participant UI as Next.js UI<br/>localhost:3000
    participant API as OPENSEC API<br/>localhost:8001

    Op->>UI: Open dashboard
    Op->>UI: Enter credentials
    UI->>API: POST /auth/login
    API-->>UI: accessToken (JWT)
    UI->>UI: Store token in localStorage

    Op->>UI: View Alerts
    UI->>API: GET /alerts (Bearer JWT)
    API-->>UI: Alert list (tenant-scoped)

    Op->>UI: View Agent fleet
    UI->>API: GET /agents (Bearer JWT)
    API-->>UI: Agent list + status

    Op->>UI: Search timeline
    UI->>API: GET /search?query=... (Bearer JWT)
    API->>API: OpenSearch query (tenant filter)
    API-->>UI: Search hits
```

---

## 9. Config hot-reload flow

Tenant-specific thresholds and policies are stored in Postgres and cached in Redis.

```mermaid
sequenceDiagram
    participant Admin
    participant API as ConfigController
    participant PG as PostgreSQL
    participant RD as Redis
    participant Svc as DetectionEngine / DefenseEngine

    Admin->>API: PUT /admin/config/detection
    API->>PG: Upsert tenant_configs row
    API->>RD: SET config:tenant:{id}:detection (TTL 1h)
    API-->>Admin: 200 OK

    Note over Svc: Services read config on next request<br/>(or via @ConfigurationProperties today)
    Svc->>RD: GET config key (optional)
    Svc->>PG: Fallback read if cache miss
```

---

## 10. Local development topology

```mermaid
flowchart LR
    subgraph compose["docker-compose.dev.yaml"]
        PG[(PostgreSQL :5432)]
        RD[(Redis :6379)]
        KF{{Kafka :9092}}
        OS[(OpenSearch :9200)]
    end

    APP[Spring Boot :8001] --> PG & RD & KF & OS
    UI[Next.js :3000] -->|CORS| APP
```

### Quick start

```bash
docker compose -f docker-compose.dev.yaml up -d
./gradlew bootRun
cd ui && npm install && npm run dev
```
