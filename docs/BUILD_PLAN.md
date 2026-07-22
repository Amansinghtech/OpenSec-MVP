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

## ADR-001: Wazuh as the endpoint + collection layer

**Decision:** Adopt [Wazuh](https://wazuh.com) (open-source SIEM/XDR, GPLv2) as the endpoint
agent and initial log-collection layer, and position OPENSEC as the **intelligence & response
layer on top** of it. Wazuh is complementary — it provides rich, already-normalized telemetry and
rule-based detections; OPENSEC adds behavioral session reconstruction, RAG intelligence,
cross-source attack-chain detection, multi-tenancy, and the plugin ecosystem.

**Why:** Wazuh already ships the endpoint agents (Win/Linux/macOS: log collection, FIM, SCA,
vulnerability detection, MITRE ATT&CK mapping) and a manager that decodes raw telemetry into
structured JSON alerts. Reusing it dramatically shrinks Phases 5, 7, 10, and 13.

**Integration approach (increasing scalability):**
1. **Integrator webhook (push)** — `wazuh-integratord` `<integration>` block POSTs JSON alerts to
   OPENSEC's ingestion API (`POST /api/v1/ingest/wazuh`). Used for the Phase 5 MVP.
2. **`alerts.json` → Fluent Bit → Kafka (stream)** — decoupled, horizontally scalable. Phase 6
   transport target. (Note: Wazuh 5.0 removed Filebeat in favor of a native `indexer-connector`,
   so use Fluent Bit / Logstash for the Kafka hop, or the webhook.)
3. **Query the Wazuh Indexer (OpenSearch fork)** — for search/analytics (relevant to Phase 10).

**Consequences / notes:**
- Ingestion is designed **source-agnostic** (a `source` discriminator + per-source mappers), so
  Wazuh is one source among the PRD's list (agents / FluentBit / webhooks / Kafka).
- **Multi-tenancy:** Wazuh isn't natively multi-tenant; map Wazuh agent groups (or manager
  clusters) → OPENSEC tenants and stamp `tenant_id` at ingestion. Agents authenticate with an
  `AGENT`-role token (Phase 2 RBAC).
- **Phase 13** shifts from "build agents from scratch" → "deploy & manage Wazuh agents".
- **Phase 10** may reuse the bundled Wazuh Indexer instead of a separate OpenSearch cluster.

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

- [x] **Add `@RequestBody` to `AuthController.login` / `signup`** — without it, Spring MVC
  binds from query params instead of the JSON body, so both endpoints are effectively broken.
  Also added `@Valid` + Bean Validation constraints on the auth DTOs.
- [x] **Fix `UserRepository` ID type mismatch** — repository is `JpaRepository<User, Long>`
  but `User.id` is a `UUID`. Change to `JpaRepository<User, UUID>` and update
  `UserService.getUser`/`deleteUser` signatures (`Long` → `UUID`).
- [x] **Fix `docker-compose.dev.yaml` healthcheck** — it checked `-U testuser -d testdb`;
  now aligned to `pg_isready -U postgres -d opensec`. Also fixed the app's default datasource
  URL to point at the `opensec` DB so it works with compose out of the box.
- [x] **Consolidate/clean Flyway migrations** — squashed `V1`/`V2` into a single clean
  `V1__init_users.sql` matching the current `User` entity (verified via Hibernate
  `ddl-auto=validate` on boot).
- [x] **Remove/repurpose dead code** — deleted the unused `UserRequest` DTO. `UserEntityListener`
  is actually wired in via `@EntityListeners` on `User`, so it was kept.
- [x] **Add a global exception handler** (`@RestControllerAdvice`) returning a structured
  `ApiError` JSON (`{ status, error, message, path, timestamp, fieldErrors }`). Maps
  validation → 400, bad credentials → 401, duplicate user → 409, generic → 500.
- [x] **Standardize a base API path** — added `/api/v1` prefix for all `@RestController`s via
  a `WebConfig` path-match prefix; updated security matchers accordingly.
- [x] **(bonus) Stop leaking the password hash** — `/users/me` was returning the bcrypt hash;
  added `@JsonIgnore` on `User.password`.

**Exit criteria:** ✅ app boots (verified against H2 in PostgreSQL mode + Flyway); `signup`
→ `login` → authenticated `/users/me` works end-to-end with a bearer token; error cases
(409 duplicate, 400 validation, 401 bad credentials) return structured JSON. Full Postgres
verification is covered once Docker/DB is available (Phase 1 adds Testcontainers).

---

## Phase 1 — Testing & CI foundation (BLOCKING)

Set up the safety net early so every later phase can be verified.

- [x] **Add Testcontainers** (PostgreSQL) for integration tests — `PostgresContainerIntegrationTest`
  runs the migrations + auth flow against a real `postgres:16-alpine`. Marked
  `@Testcontainers(disabledWithoutDocker = true)` so it runs in CI and auto-skips where Docker
  is unavailable.
- [x] **Write auth flow integration tests** — `AuthFlowIntegrationTest` (11 tests, H2 in
  PostgreSQL mode, always runs): signup/duplicate/invalid, login/bad-credentials, protected
  route with/without token, **expired token**, viewer-forbidden vs admin-allowed (RBAC),
  refresh rotation + old-token rejection, and logout revocation.
- [x] **Add a GitHub Actions CI workflow** — `.github/workflows/ci.yml` runs `./gradlew build`
  (compile + test + coverage gate + format check) on every push to `main` and every PR, on
  JDK 24; uploads test + coverage reports as artifacts.
- [x] **Add code formatting/linting** — Spotless + ktlint (`spotlessCheck` wired into `check`),
  with an `.editorconfig`. Existing code reformatted once to comply.
- [x] **Add test coverage reporting** (JaCoCo `0.8.13`, Java-24 compatible) with a baseline
  `jacocoTestCoverageVerification` gate (min 30% instruction; currently ~86%).

**Exit criteria:** ✅ `./gradlew check` is green locally (13 tests: 12 run + 1 Testcontainers
skipped without Docker); CI workflow runs build + tests + lint + coverage on every PR.

> **Note:** implemented after Phase 2 (deferred at the user's request), so the tests exercise
> the full Phase 0 + Phase 2 auth/RBAC surface. Raise the coverage threshold as the codebase grows.

---

## Phase 2 — Auth & RBAC (README TODO: "Auth & RBAC mechanism")

Turn the basic JWT auth into a real multi-tenant RBAC system (PRD §1).

- [x] **Role & Permission model** — `Role`, `Permission` entities + join tables
  (`role_permissions`, `user_roles`); seeded default roles (`ADMIN`, `ANALYST`, `AGENT`,
  `VIEWER`) and a permission set via an idempotent `DataInitializer`.
- [x] **Wire authorities into `AuthenticatedUser`** — `getAuthorities()` now emits
  `ROLE_*` plus permission authorities derived from the user's roles.
- [x] **Method/route-level authorization** — `@EnableMethodSecurity` on; `@PreAuthorize`
  guards on admin user endpoints (`GET /users`, `GET /users/{id}`, `DELETE /users/{id}`).
- [x] **Refresh tokens** — short-lived access JWTs (jti + roles + tenant claims) + opaque
  DB-stored refresh tokens with rotation; added `POST /auth/refresh`.
- [x] **Token revocation / logout** — `POST /auth/logout` revokes the refresh token and adds
  the access token's `jti` to a revocation list checked by `JWTAuthFilter`. Implemented behind
  a `TokenRevocationService` interface with a DB-backed impl (Redis-backed impl in Phase 3).
- [x] **Tenant model** — `Tenant` entity; users belong to a tenant (default tenant seeded);
  `tenantId` is carried in the JWT and exposed on the security principal (`AuthenticatedUser.tenantId`).
  Full request-scoped tenant filter is Phase 4.
- [x] **Auth audit logging** — `auth_audit_log` table + `AuthAuditService`; records SIGNUP,
  LOGIN_SUCCESS, LOGIN_FAILURE, TOKEN_REFRESH, LOGOUT with IP/user-agent.
- [x] **(bonus) 401 vs 403 handlers** — custom `AuthenticationEntryPoint` (401 JSON) and
  `AccessDeniedException` handling (403 JSON) replace the previous blanket 403.
- [ ] (Later) **SSO readiness** — structure config for OIDC/SAML (implementation deferred).

**Exit criteria:** ✅ RBAC enforced on endpoints (verified 403 for viewer, 200 for admin);
refresh rotation + access/refresh revocation working; every issued token + principal carries a
tenant context. Verified end-to-end against H2 (PostgreSQL mode) with Hibernate schema validation.

> **Note on ordering:** token revocation is DB-backed for now behind `TokenRevocationService`;
> **Phase 3** swaps in a Redis implementation. This was the one Phase 2 item that logically
> depends on Redis, so it was stubbed with a persistent DB store rather than deferred.

---

## Phase 3 — Introduce Redis (shared infra dependency)

Redis underpins rate limiting, revocation, and config caching across later phases (PRD §Redis).

- [x] Add Redis to `docker-compose.dev.yaml` (with healthcheck) and Spring config
  (`spring.data.redis.*` in local/prod properties, env-overridable).
- [x] Add `spring-boot-starter-data-redis`; `RedisConfig` wires the Redis beans on top of Boot's
  auto-configured `StringRedisTemplate` (activated via conditionals so the app/tests run without Redis).
- [x] Move JWT revocation list to Redis — `RedisTokenRevocationService` (TTL = token's remaining
  lifetime, self-expiring). Selected via `opensec.revocation.store=redis` (DB impl remains the
  default fallback via `@ConditionalOnProperty`). **Fails open** on Redis outage.
- [x] Add a reusable rate-limiting component — `RateLimiter`/`RedisRateLimiter` (fixed-window
  `INCR`+TTL) applied by `RateLimitingFilter` to configured path prefixes (auth endpoints by
  default), returning `429` structured JSON. Enabled via `opensec.rate-limit.enabled`. **Fails open**.

**Exit criteria:** ✅ app boots with the Redis starter and lazily connects (verified even with Redis
unreachable — it degrades gracefully, failing open). Redis-backed revocation + rate-limiting (`429`)
are verified by `RedisIntegrationTest` (Testcontainers `redis:7`, runs in CI, skipped without Docker).

> **Resilience note:** both Redis components fail open (allow the request / treat token as not
> revoked) if Redis is unavailable, so an infra outage can't lock everyone out. The short access-token
> TTL bounds the revocation risk window.

---

## Phase 4 — API Gateway concerns (PRD §2)

Harden the REST surface as the single entry point.

- [x] **Rate limiting** on public/auth endpoints (Redis-backed) — delivered in Phase 3
  (`RateLimitingFilter` + `RedisRateLimiter`).
- [x] **Tenant context injection** filter — `TenantContextFilter` binds the authenticated user's
  tenant into a request-scoped `RequestContext` (+ MDC) after authentication.
- [x] **Request/response logging + correlation ID** — `CorrelationIdFilter` (runs first) reads or
  generates `X-Correlation-Id`, echoes it on the response, and exposes it via MDC so every log line
  carries `correlationId`/`tenantId`. Logging pattern updated.
- [x] **Consistent error envelope + validation** — `ApiError` envelope (Phase 0) + Bean Validation
  on DTOs; the global handler maps validation → 400.
- [x] **API versioning** finalized (`/api/v1` via `WebConfig` path prefix, Phase 0).

**Exit criteria:** ✅ all requests are rate-limited (Phase 3), tenant-scoped (`RequestContext`), and
carry a correlation id (verified by `GatewayIntegrationTest`).

---

## Phase 5 — Log Ingestion Module (README TODO: "Log Ingestion Module" + "REST APIs for DataIngestion")

Accept logs from agents and other sources (PRD §3). **Wazuh is the first real source** (see ADR-001).

- [ ] Define the **source-agnostic raw log ingestion contract** (DTO/schema: source, host,
  timestamp, payload, tenant_id, correlation_id).
- [ ] **Ingestion REST API** — `POST /api/v1/ingest/logs` (single + batch), authenticated
  via an `AGENT`-role token.
- [ ] **Wazuh ingestion endpoint** — `POST /api/v1/ingest/wazuh` that accepts the Wazuh
  Integrator webhook payload and maps it (rule.id/level/mitre, agent, decoder, data.*) into the
  raw ingestion contract.
- [ ] **Validation & buffering** — validate incoming logs; buffer before forwarding.
- [ ] **Persistence of raw logs** (Postgres table) for replay/audit.
- [ ] **Backpressure / size limits** and idempotency (dedupe by event/correlation id).
- [ ] Integration tests for ingestion happy-path + rejection cases (incl. a sample Wazuh alert).

**Exit criteria:** an authenticated client (and a Wazuh Integrator webhook) can POST single/batch
logs; they are validated, deduped, stored, and ready to forward to normalization.

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

- [ ] Add OpenSearch to `docker-compose.dev.yaml` **or reuse the bundled Wazuh Indexer** (an
  OpenSearch fork) — decide and document (see ADR-001).
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

## Phase 13 — Client-Side Agents via Wazuh (README TODO: "Create Client Side Agents for Logs ingestion")

Endpoint telemetry + response execution for Windows/Linux (PRD §3 Agents). Per ADR-001, this is
**"deploy & manage Wazuh agents"** rather than building agents from scratch.

- [ ] Wazuh manager deployment + agent enrollment guidance (map agent groups → tenants).
- [ ] Configure the Wazuh Integrator to ship alerts to the OPENSEC ingestion API (Phase 5) with an
  `AGENT` token; or `alerts.json` → Fluent Bit → Kafka (Phase 6).
- [ ] Agent/manager health surfaced in OPENSEC (heartbeat / fleet status).
- [ ] Command channel for defense actions — leverage Wazuh **active-response** (sets up Phase 14).
- [ ] Windows + Linux agent rollout docs.

**Exit criteria:** Wazuh agents on endpoints ship telemetry into OPENSEC ingestion, and fleet
health is visible.

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

Progress: **Phase 0** ✅, **Phase 1** ✅ (CI + tests + lint + coverage), **Phase 2** ✅ (Auth &
RBAC), **Phase 3** ✅ (Redis: revocation + rate limiting). Next: **Phase 4** (API Gateway concerns —
tenant-context filter, correlation IDs, validation) builds directly on the Redis rate limiter.
Tackle one checkbox group per PR.
