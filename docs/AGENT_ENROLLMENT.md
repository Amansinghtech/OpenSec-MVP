# Agent enrollment & Wazuh integration

OPENSEC agents are **enrolled credentials** that authenticate telemetry ingestion and heartbeats.
Per [ADR-001](./BUILD_PLAN.md#adr-001-wazuh-as-the-endpoint--collection-layer), endpoint
telemetry is collected by **Wazuh agents**; OPENSEC manages fleet identity, API keys, and tenant
mapping.

## 1. Enroll an agent (admin)

```bash
curl -s -X POST "$OPENSEC_URL/api/v1/agents" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "web-server-1",
    "hostname": "web-server-1.corp.local",
    "platform": "LINUX",
    "wazuhAgentGroup": "production",
    "wazuhAgentId": "001"
  }'
```

Response includes `apiKey` (**shown once**). Store it securely — it cannot be retrieved later.

| Field | Purpose |
|-------|---------|
| `wazuhAgentGroup` | Maps to a Wazuh agent group → OPENSEC tenant |
| `wazuhAgentId` | Optional link to the Wazuh agent id (`agent.id` in alerts) |

## 2. Agent authentication

Agents authenticate with the issued API key:

```bash
# Option A — dedicated header
curl -H "X-Api-Key: opsk_..." ...

# Option B — Authorization scheme
curl -H "Authorization: ApiKey opsk_..." ...
```

API keys grant the `AGENT` role for ingestion and heartbeat endpoints.

## 3. Heartbeat

Agents (or a sidecar on the Wazuh manager) should POST heartbeats periodically:

```bash
curl -X POST "$OPENSEC_URL/api/v1/agents/heartbeat" \
  -H "X-Api-Key: $AGENT_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"hostname":"web-server-1","wazuhAgentId":"001"}'
```

Fleet health is visible via `GET /api/v1/agents` and `GET /api/v1/agents/fleet/summary`.
Agents without a heartbeat for 15 minutes are reported as `OFFLINE`.

## 4. Ship Wazuh alerts to OPENSEC

Configure the Wazuh Integrator on the manager (`/var/ossec/etc/ossec.conf`):

```xml
<integration>
  <name>custom-webhook</name>
  <hook_url>https://opensec.example.com/api/v1/ingest/wazuh</hook_url>
  <level>3</level>
  <alert_format>json</alert_format>
  <api_key>opsk_YOUR_AGENT_API_KEY</api_key>
</integration>
```

> Wazuh's integrator sends a plain HTTP POST. Configure your reverse proxy or a small relay to
> translate the integrator payload into `Authorization: ApiKey ...` or `X-Api-Key` if needed.

Alternatively, stream `alerts.json` through Fluent Bit → Kafka (see Phase 6).

## 5. Wazuh agent rollout (Linux)

On the Wazuh manager:

```bash
/var/ossec/bin/agent-auth -m wazuh-manager.example.com
```

Assign the agent to the group that maps to your OPENSEC tenant (`wazuhAgentGroup` at enrollment).

On Windows, use the Wazuh MSI installer and point the agent at the same manager.

## 6. Revoke an agent

```bash
curl -X DELETE "$OPENSEC_URL/api/v1/agents/{id}" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Revoked API keys are rejected immediately.
