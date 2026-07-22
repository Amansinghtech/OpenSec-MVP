# OPENSEC Web UI (Phase 16)

Next.js operator console for the OPENSEC Command & Control platform.

## Stack

- **Next.js 15** (App Router) + React 19 + TypeScript
- Proxies API calls to the Spring Boot backend (`OPENSEC_API_URL`)

## Run locally

```bash
# Terminal 1 — backend
./gradlew bootRun

# Terminal 2 — UI
cd ui
npm install
OPENSEC_API_URL=http://localhost:8001/api/v1 npm run dev
```

Open http://localhost:3000 — login with your admin credentials, then browse Alerts and Agents.

## Pages

| Route | Purpose |
|-------|---------|
| `/` | Dashboard + login |
| `/alerts` | Alert list (from `GET /api/v1/alerts`) |
| `/agents` | Agent fleet (from `GET /api/v1/agents`) |
