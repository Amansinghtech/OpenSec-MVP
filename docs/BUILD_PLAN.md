# OPENSEC — Build Plan

A step-by-step, incremental plan to build the **Autonomous Security Operations Engine**.

This document is the single source of truth for _what to build next_. We tick items off
one by one as they are completed. Keep changes small and reviewable — prefer one PR per
sub-section (or per checkbox group) so progress stays visible.

> Reference documents:
> - Product/architecture vision: [`docs/PRD.md`](./PRD.md)
> - High-level roadmap: [`README.md`](../README.md)

## How to use this document

- `[ ]` = not started, `[~]` = in progress, `[x]` = done.
- When you complete an item, flip its checkbox and add a short note (PR link / commit) if useful.
- Each phase has an **Exit criteria** — the phase is "done" only when those are met.
- Phases are ordered by dependency. Do not start a later phase until earlier **blocking**
  phases are complete, but non-blocking polish items can be deferred.

## Current state (baseline)

Implemented so far (Kotlin + Spring Boot 4, Gradle, PostgreSQL/H2, Flyway, JWT, Swagger):

- `AuthController` with `/auth/login` and `/auth/signup`
- `User` entity, `UserRepository`, `UserService` (implements `UserDetailsService`)
- `JWTService`, `JWTAuthFilter`, `SecurityConfig` (stateless JWT filter chain)
- `JWTConfig` / `AppConfig` for secret + expiration config
- Flyway migrations `V1`/`V2` for the `users` table
- OpenAPI / Swagger UI with bearer auth
- `docker-compose.dev.yaml` for local PostgreSQL

---

## Phase 0 — Foundation hardening & bug fixes (BLOCKING)

Fix correctness issues in the existing scaffold before building on top of it. These are
small but block reliable auth.

- [ ] **Add `@RequestBody` to `AuthController.login` / `signup`** — without it, Spring MVC
  binds from query params instead of the JSON body, so both endpoints are effectively broken.
- [ ] **Fix `UserRepository` ID type mismatch** — repository is `JpaRepository<User, Long>`
  but `User.id` is a `UUID`. Change to `JpaRepository<User, UUID>` and update
  `UserService.getUser`/`deleteUser` signatures (`Long` → `UUID`).
- [ ] **Fix `docker-compose.dev.yaml` healthcheck** — it checks `-U testuser -d testdb`
  but the container is `postgres`/`opensec`. Align to `pg_isready -U postgres -d opensec`.
- [ ] **Consolidate/clean Flyway migrations** — `V1`/`V2` create-then-heavily-alter the
  `users` table. Since there is no production data yet, squash into a single clean
  `V1__init_users.sql` (or leave as-is and just document — decide and note the choice).
- [ ] **Remove/repurpose dead code** — `UserRequest` DTO and `UserEntityListener` appear
  unused/underused; either wire them in or delete.
- [ ] **Add a global exception handler** (`@RestControllerAdvice`) returning structured
  error JSON (e.g. `{ code, message, timestamp, path }`) instead of raw `RuntimeException`.
- [ ] **Standardize a base API path** — decide on `/api/v1` prefix now (matches PRD "API
  versioning") and apply consistently.

**Exit criteria:** app boots against Postgres, `signup` → `login` → call an authenticated
endpoint (`/users/me`) works end-to-end via Swagger with a bearer token.

---

## Phase 1 — Testing & CI foundation (BLOCKING)

Set up the safety net early so every later phase can be verified.

- [ ] **Add Testcontainers** (PostgreSQL) for integration tests.
- [ ] **Write auth flow integration tests** — signup, login, invalid credentials, expired
  token, accessing a protected route with/without a valid token.
- [ ] **Add a GitHub Actions CI workflow** — build + test on every PR (`./gradlew build`).
- [ ] **Add code formatting/linting** — e.g. `ktlint` or `spotless`, enforced in CI.
- [ ] **Add test coverage reporting** (JaCoCo) with a baseline threshold.

**Exit criteria:** CI is green on `main`; PRs run build + tests automatically.

---

## Phase 2 — Auth & RBAC (README TODO: "Auth & RBAC mechanism")

Turn the basic JWT auth into a real multi-tenant RBAC system (PRD §1).

- [ ] **Role & Permission model** — `Role`, `Permission` entities + join tables; seed
  default roles (e.g. `ADMIN`, `ANALYST`, `AGENT`, `VIEWER`).
- [ ] **Wire authorities into `AuthenticatedUser`** — `getAuthorities()` currently returns
  empty; populate from the user's roles/permissions.
- [ ] **Method/route-level authorization** — enable `@EnableMethodSecurity`, protect
  endpoints with `@PreAuthorize` / role checks.
- [ ] **Refresh tokens** — issue short-lived access + longer-lived refresh tokens; add
  `/auth/refresh`.
- [ ] **Token revocation / logout** — Redis-backed JWT revocation list (PRD §1 store).
- [ ] **Tenant model** — `Tenant` entity; every user belongs to a tenant; inject
  `tenant_id` into the security context for downstream scoping.
- [ ] **Auth audit logging** — persist auth events (login success/failure, token refresh).
- [ ] (Later) **SSO readiness** — structure config for OIDC/SAML (implementation deferred).

**Exit criteria:** RBAC enforced on endpoints; refresh + revocation working; every request
carries a tenant context.

---

## Phase 3 — Introduce Redis (shared infra dependency)

Redis underpins rate limiting, revocation, and config caching across later phases (PRD §Redis).

- [ ] Add Redis to `docker-compose.dev.yaml` and Spring config.
- [ ] Add `spring-boot-starter-data-redis`; create a `RedisConfig` + template beans.
- [ ] Move JWT revocation list to Redis (from Phase 2).
- [ ] Add a reusable rate-limiting component (used by the gateway/ingestion later).

**Exit criteria:** app connects to Redis locally; revocation + a sample rate-limited route work.

---

## Phase 4 — API Gateway concerns (PRD §2)

Harden the REST surface as the single entry point.

- [ ] **Rate limiting** on public/auth endpoints (Redis-backed).
- [ ] **Tenant context injection** filter (reads tenant from JWT, sets request scope).
- [ ] **Request/response logging + correlation ID** — generate/propagate `correlation_id`
  on every request (PRD system property: every event carries `tenant_id` + `correlation_id`).
- [ ] **Consistent error envelope + validation** — Bean Validation on all DTOs.
- [ ] **API versioning** finalized (`/api/v1`).

**Exit criteria:** all requests are rate-limited, tenant-scoped, and carry a correlation id.

---

## Phase 5 — Log Ingestion Module (README TODO: "Log Ingestion Module" + "REST APIs for DataIngestion")

Accept logs from agents and other sources (PRD §3).

- [ ] Define the **raw log ingestion contract** (DTO/schema: source, host, timestamp,
  payload, tenant_id, correlation_id).
- [ ] **Ingestion REST API** — `POST /api/v1/ingest/logs` (single + batch), authenticated
  via agent credentials/token.
- [ ] **Validation & buffering** — validate incoming logs; buffer before forwarding.
- [ ] **Persistence of raw logs** (Postgres table or object storage) for replay/audit.
- [ ] **Backpressure / size limits** and idempotency (dedupe by correlation id).
- [ ] Integration tests for ingestion happy-path + rejection cases.

**Exit criteria:** an authenticated client can POST single/batch logs; they are validated,
stored, and ready to forward to normalization.

---

## Phase 6 — Event Bus with Kafka (README TODO: "Implement Event Bus with Kafka")

The async backbone (PRD §Event Bus). Introduce before normalization so services decouple.

- [ ] Add Kafka (+ Zookeeper/KRaft) to `docker-compose.dev.yaml`.
- [ ] Add `spring-kafka`; create producer/consumer config.
- [ ] Define **topic strategy** — `normalized_events`, `session_events`,
  `detection_signals`, `contextualized_detections`, `alert_events`, `defense_events`;
  partition by `tenant_id`.
- [ ] Define **event envelope** schema (versioned; includes `tenant_id`, `correlation_id`,
  `event_type`, `occurred_at`).
- [ ] Ingestion service **publishes** raw/accepted logs to Kafka.
- [ ] Add a schema registry decision (Avro/JSON Schema) — document choice.

**Exit criteria:** ingestion publishes to Kafka; a sample consumer reads events locally.

---

## Phase 7 — Normalization Service (README TODO: "Normalization Service for identifying attack chains")

Convert raw logs into structured security events (PRD §4).

- [ ] Consume raw logs from Kafka.
- [ ] **Parsers** for structured + unstructured logs (start with a couple of common formats,
  e.g. syslog, Windows event log JSON).
- [ ] **Metadata extraction** — assign entity identifiers (user/IP/host), classify event type.
- [ ] Produce `normalized_event` to the `normalized_events` topic.
- [ ] Pluggable parser interface (sets up the future plugin system).
- [ ] Tests with sample raw→normalized fixtures.

**Exit criteria:** raw logs flowing through ingestion → Kafka → normalization emit
structured `normalized_event`s.

---

## Phase 8 — Session Reconstruction Service (PRD §5)

Build behavioral sessions to identify attack chains (stateful).

- [ ] Consume `normalized_events`.
- [ ] **Sliding time-window state** keyed by `tenant_id + entity_id`.
- [ ] Build event sequences; detect suspicious state transitions.
- [ ] Emit `session_updated` and `suspicious_session_detected`.
- [ ] Persist sessions (Postgres) and design for horizontal partitioning.

**Exit criteria:** sessions are reconstructed from event streams and suspicious sessions
are emitted downstream.

---

## Phase 9 — Detection / Policy Engine (README TODO: "Detection / Policy Engine")

Multi-layer detection (PRD §6).

- [ ] Signature-based detection rules.
- [ ] Behavioral template matching (consumes session events).
- [ ] Anomaly scoring + configurable risk scoring.
- [ ] Policy evaluation engine with thresholds (config-driven, Redis-cached).
- [ ] Emit `detection_signal`.

**Exit criteria:** normalized + session events produce scored `detection_signal`s per policy.

---

## Phase 10 — OpenSearch integration (README TODO: "Implement OpenSearch for faster logs searching")

Fast search/indexing layer (PRD §OpenSearch). Can be introduced in parallel once events flow.

- [ ] Add OpenSearch to `docker-compose.dev.yaml`.
- [ ] Index normalized events, sessions, detections, alerts.
- [ ] Search APIs for timeline/dashboard queries.

**Exit criteria:** events/detections are searchable via OpenSearch-backed APIs.

---

## Phase 11 — RAG Intelligence Engine (README TODO: "RAG Intelligence Engine")

Contextual threat intel enrichment (PRD §7). Must be fail-safe / non-blocking.

- [ ] Choose a Vector DB; add to compose.
- [ ] Ingest exploit corpus; generate + store embeddings.
- [ ] Similarity search + CVE mapping.
- [ ] LLM-based contextual reasoning (behind a provider abstraction).
- [ ] Emit `contextualized_detection`; ensure detection pipeline still works if RAG is down.

**Exit criteria:** detections can be enriched with CVE/context; pipeline degrades gracefully
when RAG is unavailable.

---

## Phase 12 — Alert Service (PRD §8)

Create and manage alerts.

- [ ] Consume `detection_signal` + `contextualized_detection`.
- [ ] Aggregate risk signals → final risk score.
- [ ] Alert entity + lifecycle (new/acknowledged/resolved/suppressed).
- [ ] Persist to Postgres + index in OpenSearch; emit `alert_created`.
- [ ] Alert query/list APIs.

**Exit criteria:** end-to-end pipeline produces queryable alerts from logs.

---

## Phase 13 — Client-Side Agents (README TODO: "Create Client Side Agents for Logs ingestion")

Endpoint telemetry + response execution for Windows/Linux (PRD §3 Agents).

- [ ] Agent enrollment/registration + credential issuance.
- [ ] Log shipping to the ingestion API (batching, retry, secure transport).
- [ ] Heartbeat reporting.
- [ ] Command channel to receive defense actions (sets up Phase 14).
- [ ] Windows + Linux builds.

**Exit criteria:** a real agent enrolls, ships logs, and reports heartbeats.

---

## Phase 14 — Active Defense & Policy Enforcement (README TODO: "Active Defence and Policy Enforcement")

Automated response (PRD §9).

- [ ] Consume `alert_created`; evaluate defense policies.
- [ ] Actions: block IP, disable user, kill process, revoke tokens, quarantine container.
- [ ] Notifications (Slack / Email).
- [ ] Emit `defense_action_executed`; full audit trail.
- [ ] Safe-guards: dry-run mode, approval gates for destructive actions.

**Exit criteria:** alerts can trigger automated, audited defense actions (with dry-run).

---

## Phase 15 — Config Service (PRD §10)

Centralized configuration management.

- [ ] Manage tenant configs, detection thresholds, risk weights, defense policies.
- [ ] Postgres as source of truth; push updates to Redis cache.
- [ ] Admin APIs to view/update config.

**Exit criteria:** thresholds/policies/weights are editable centrally and hot-applied via Redis.

---

## Phase 16 — Command & Control Web UI (README TODO: "Create Web-UI for Command and Control")

Operator console.

- [ ] Choose stack (e.g. React/Next.js) — document decision.
- [ ] Auth (login via Auth service), tenant switching.
- [ ] Dashboards: alerts, sessions timeline, detections, agent fleet health.
- [ ] Config/policy management screens.
- [ ] Trigger/review active-defense actions.

**Exit criteria:** operators can monitor and control the platform from the UI.

---

## Phase 17 — Plugin architecture (README TODO: "Convert to a plugin based system")

Make detection + active defense modular (PRD "Plugin ecosystem expansion").

- [ ] Define plugin SPI/interfaces for detection rules and defense actions.
- [ ] Plugin discovery/loading + lifecycle + isolation.
- [ ] Migrate existing detection/defense logic to plugins.
- [ ] Document how to author a plugin.

**Exit criteria:** new detection rules / defense actions can be added as plugins without
modifying core services.

---

## Cross-cutting (ongoing, not a single phase)

- [ ] **Observability** — structured logging, metrics (Micrometer/Actuator is already
  present), tracing across services + Kafka.
- [ ] **Security hardening** — secrets management, dependency scanning, input validation,
  rate limiting everywhere.
- [ ] **Documentation** — keep `docs/` current; add per-service READMEs and an ADR log for
  key decisions (Kafka vs NATS, vector DB choice, UI stack, etc.).
- [ ] **Multi-tenancy correctness** — verify tenant isolation at every layer as it's added.
- [ ] **Performance** — track toward the PRD target of 10k–100k events/sec.

---

## Immediate next step

Start with **Phase 0** (foundation fixes) since it unblocks a working auth flow, then set up
**Phase 1** (tests + CI) so everything after it is verifiable. Tackle one checkbox group per PR.
