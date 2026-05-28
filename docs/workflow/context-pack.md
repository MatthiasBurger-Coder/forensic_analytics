# Workflow Context Pack

## Workflow

- Workflow version: `fa-deploy-0001-docker-compose-services-20260528-v1`
- Branch: `feature/workflow-docker-compose-deployment-20260528`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`
- Docker network: `forensic_analytics`

## Affected Areas

- Docker Compose local deployment
- Docker build context and `.dockerignore`
- Service-owned Dockerfiles and runtime configuration
- GUI deployment through `forensic-ui`
- Public API routing through `query-report-api-service`
- Target and transitional service boundary documentation
- Deployment runbook and arc42 deployment view

## Guardrails

- Build-context guard: `.dockerignore` must allow service boot jars copied by
  root-context Dockerfiles.
- GUI API guard: `forensic-ui` must not keep the hardcoded `/api` 502 response
  when claiming deployed GUI/API integration.

## Forbidden Areas

- Production readiness claims without executed evidence
- Docker Swarm or Kubernetes manifests
- Shared Java implementation modules
- Cross-service private database or filesystem access
- Fabricated runtime, graph, replay, report, or observability evidence
- LLM provider integration

## Required Roles

- Senior Requirement Engineer
- Senior System Architect
- Senior Java Backend Developer
- Senior React Frontend Developer
- Senior Tester
- Senior DevOps Engineer
- Senior Workflow Architect
- Senior Documentation Engineer

## Conditional Roles

- Senior Joern CPG Specialist
- Source Analysis Reviewer
- Replay Graph LLM Reviewer
- Senior Analysis Storage Architect
- Ingestion Handoff Reviewer
- Observability Runtime Diagnostics

## Governing File Hashes

| File | SHA-256 |
|---|---|
| `AGENTS.md` | `318d6bb0c2b54d3ff42827692f0c8afa9c7a8846356a57eb8d4e98947c435c7a` |
| `QUALITY.md` | `9df68c96dbdd9bf36e139cfdb2cc91c341a5c08c928f03d8fe24392281fcb848` |
| `.codex/AGENTS.md` | `d43bc1ea9ca10deb8a6553c7c21208e8020d751f674a8cb824959b666bd01aec` |
| `.codex/workflow/workflow-execution-rules.md` | `6d2ea74943f8ff4187a371d6e263fcf7f179d7250868086f4ba294e6f5fa122a` |
| `.agents/orchestrator/routing-rules.md` | `30cd2a044746ab97f798425dd8f8125a98c6ed50d2d70a1b0778dca353c325bf` |
| `.agents/orchestrator/swarm-orchestrator.md` | `ae501a9e61ec0a9cf4acaad7fb7fd5d6167309b722370ed5d21d1991e49c09fc` |
| `.agents/skills/workflow-authoring/SKILL.md` | `d87950d6d9ca831a4201b660c6bef373cb85be829f21694a323dbb9b8544d801` |
| `.agents/skills/workflow-slice/SKILL.md` | `f58db9f89a32d6312c767d3d954aaf374a7bbe12c25915c0101489c990a54976` |
| `.agents/skills/devops-docker/SKILL.md` | `1f1677209469a9d6b47e0bdfd45b427b0ebcb40bb4c8d94d07bb9d8a36dab6c1` |
| `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md` | `95c04f47127f5149bb39a7e1b82b2690803cc765cad5d18274a82d415931e9ad` |
| `docs/adr/ADR-0017-target-microservices-service-landscape.md` | `1c6be5f82ea08e38d669d335c3594eeb8fc4484d119e70c4eb8a443c47437b4b` |
| `docs/arc42/07-deployment-view.md` | `1ab5073866a2d46ee3e6b722efe09d63cd28ad8c75e549e6ca9e33e2a3960fb3` |
| `docs/architecture/service-roots.md` | `cafab5de927f426c859ca99387ec09b222a65c9bb9cc35279a5ef40477acc2b7` |
| `docs/architecture/service-communication-matrix.md` | `246084bdc487d190bd5658210ce476e930ee116d345b627d44d138812963f37b` |
| `docs/architecture/data-ownership.md` | `3e46f719cc7859c4f81e4317fd8d25c1fc0c20fb8bc70a55707cedd66be8ddb8` |
| `settings.gradle.kts` | `b588d616561e9dd902961ec9f53b15f519ec1f0f6f189cba60e5396691840fcb` |

## Staleness Rules

This context pack is stale when any governing hash changes, when
`docs/workflow/workflow.md` is regenerated, or when `workflow execute` discovers
an architecture, quality, service-boundary, or deployment blocker not recorded
in the workflow.
