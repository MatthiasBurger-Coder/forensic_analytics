# Forensics Platform Documentation

This directory contains the architecture and product documentation for the Forensics Platform.

## Documentation Structure

- [arc42/](arc42/) - Architecture documentation based on the arc42 template
- [epics/](epics/) - Versioned product and requirement epics
- [adr/](adr/) - Architecture Decision Records, if present
- [governance/](governance/) - Reusable engineering governance flow for EPIC, arc42, workflows, skills and roles
- [process/](process/) - Command and publication governance for `skills update`, `workflow create`, `workflow execute`, slice checkpoint push, `push` and guarded `push auto` for skills, agents, process governance and governance-only workflow documentation
- [agents/](agents/) - Agent organigramm, [agent governance](agents/agent-governance.md) and skill registry for process-strand ownership
- [workflow/](workflow/) - Active governed workflow and execution slices for the engineering governance system

## Future Microservice Boundary Direction

Future service-split work follows strict microservice autonomy. Services must not share Java implementation modules, domain models, DTO modules, service modules, repository modules, utility modules, internal error models, event implementation classes or test fixtures. Integration between services is allowed only through REST/OpenAPI, gRPC/protobuf or RabbitMQ/message contracts.

Contracts may be centrally documented under `contracts/`, but they must not become shared Java implementation modules. Each future service must be independently buildable, runnable, testable, configurable, observable, health-checkable and container-ready before it is called a microservice.

Microservice governance is documented in [architecture/microservice-governance.md](architecture/microservice-governance.md). Contract-first service communication governance is documented in [governance/contract-governance.md](governance/contract-governance.md). Docker, Docker Swarm and Kubernetes readiness must be verified from repository tooling before deployment commands or manifests are documented.

## Current Implementation Baseline

The active Gradle build is service-root based. Current backend and operational
boundaries live under `services:*`; see [../services/README.md](../services/README.md)
for the verified service directory map and the difference between transitional
service slices, target service evidence and optional later services.

The current platform direction supports two Analytics-owned input paths:
server-side repository analysis and producer-supplied artifact package
ingestion. Plugins trigger analysis on the Forensic Analytics server or submit
producer-packaged artifacts as provenance-bearing inputs; Analytics prepares
workspaces, checks out repositories when required, runs parser, Joern and BTM
capabilities server-side where owned by the platform, and returns the artifacts
needed by clients.

Public REST, CLI and gRPC contract vocabulary remains documented under
`contracts/`. Contract vocabulary is not proof that a retired implementation
module is active. Executable service ownership is established by service-local
tests and workflow gates.

Service-local container material lives with the owning service directory or
under deployment documentation after runtime behavior is verified. The retired
Boot container documentation under `docker/boot-app` is no longer an executable
runtime target.

### React UI

The `forensic-ui` app communicates with the backend only through HTTP/REST. Browser gRPC, gRPC-Web, WebSocket and SSE are intentionally excluded from this MVP slice.

Local frontend commands:

```bash
cd forensic-ui
npm ci
npm run dev
npm run test
npm run build
```

The default API base URL is `/api`. For local Vite development against the default backend port, run:

```bash
VITE_API_BASE_URL=http://127.0.0.1:8080/api npm run dev
```

The nginx container serves the built Vite assets with SPA fallback. It does not proxy `/api` because the repository has no root compose file or verified backend service name.

### Server-Side Repository Analysis

Repository analysis is server-bound. Analytics resolves repository source roots from server-side workspaces, scans Java source facts with the JavaParser adapter and can enrich the application result with Docker-based Joern artifacts. Joern analysis currently preserves call graph nodes and edges, methods, static call relations, control-flow relations, data-flow paths and semantic anchors as explicit semantic graph facts in the application result.

When runtime debugging requires instrumentation, Analytics owns BTM generation from the server-side analysis and instrumentation plan. The plugin may receive server-generated BTM files and bind them to the target implementation through the runtime agent so runtime information can be collected during debugging. The plugin does not generate BTM files and does not become the analysis platform.

## Current Architecture Baseline

The current architecture baseline is derived from:

- EPIC: Forensics Platform - Exception-centered Runtime Replay and LLM-assisted Error Analysis
- Version: 0.2
- Date: 2026-05-17

## Core Vision

The Forensics Platform combines static code analysis, semantic graph analysis, runtime tracing, exception replay and LLM-supported diagnosis into a controlled analysis and repair flow.

The long-term product vision is:

```text
Observe -> Replay -> Understand -> Fix -> Test -> Verify -> Deploy
```
