# OPENSEC-MVP
---

This project AI-based Active Monitoring & Active Defense platform — built to detect multi-stage attacks using behavioral reconstruction + contextual threat intelligence.

## Important things to keep in mind
1. Project is a monorepo and should contain all services and business logic in one place.
2. For now we will only focus on Windows / Linux platform for Client Side Agents.

## Build Plan
See [`docs/BUILD_PLAN.md`](docs/BUILD_PLAN.md) for the detailed, step-by-step plan we tick off as work progresses.

## Architecture diagrams
See [`docs/FLOWS.md`](docs/FLOWS.md) for Mermaid diagrams covering the telemetry pipeline, auth flow, agent enrollment, alert/defense lifecycle, multi-tenancy, plugins, and the operator UI.

## TODO List
[x] - Create a boilerplate code
[] - Auth & RBAC mechanism
[] - Log Ingestion Module
[] - Implement Rest APIs for DataIngestion
[] - Create Client Side Agents for Logs ingestion
[] - Implement Event Bus with Kafka
[] - Implement Normalization Service for identifying attack chains
[] - RAG Intelligence Engine
[] - Detection / Policy Engine
[] - Create Web-UI for Command and Control of this project.
[] - Implement Active Defence and Policy Enforcement mechanism.
[] - Implement OpenSearch for faster logs searching.
[] - Convert this project to a plugin based system where detection and active defence is modular.