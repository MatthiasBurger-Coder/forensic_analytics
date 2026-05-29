# Workflow Context Pack

## Identity

- Workflow: Move Repository Workspace Metadata to PostgreSQL
- Version: 2026-05-29
- Branch: `feature/workflow-workspace-postgres-20260529`
- Process strand: `workflow create`
- Execution profile: `FULL_PATH`

## Affected Areas

- `repository-source-service`
- Repository-source persistence ports and outbound adapters
- Repository-source Spring bootstrap wiring
- Liquibase changelog resources
- Local Docker Compose runtime with `forensic-postgres`
- ADR, arc42 and architecture ownership documentation

## Forbidden Areas

- Platform workspace membership, project, asset, audit or retention features
- Public REST/gRPC contract changes without contract governance
- Cross-service database access
- Checkout byte storage in PostgreSQL
- Graph, vector, replay, report or LLM persistence decisions
- Hidden H2 compatibility fallback

## Required Roles

- Senior Requirement Engineer
- Senior System Architect
- Senior Java Backend Developer
- Senior React Frontend Developer
- Senior Tester

## Conditional Roles

- Data Ownership And Persistence Steward
- Senior Analysis Storage Architect
- Senior DevOps Engineer
- Security And Threat Modeling
- Observability And Runtime Diagnostics
- ADR Steward
- Quality Gate Orchestrator

## Required Quality Commands

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
git diff --check
```

Docker Compose model checks are required for slices that change Compose files.

## Governing File Hashes

| File | SHA-256 |
|---|---|
| `AGENTS.md` | `318d6bb0c2b54d3ff42827692f0c8afa9c7a8846356a57eb8d4e98947c435c7a` |
| `QUALITY.md` | `9df68c96dbdd9bf36e139cfdb2cc91c341a5c08c928f03d8fe24392281fcb848` |
| `.agents/orchestrator/routing-rules.md` | `30cd2a044746ab97f798425dd8f8125a98c6ed50d2d70a1b0778dca353c325bf` |
| `.agents/orchestrator/swarm-orchestrator.md` | `ae501a9e61ec0a9cf4acaad7fb7fd5d6167309b722370ed5d21d1991e49c09fc` |
| `.agents/skills/workflow-authoring/SKILL.md` | `d87950d6d9ca831a4201b660c6bef373cb85be829f21694a323dbb9b8544d801` |
| `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md` | `95c04f47127f5149bb39a7e1b82b2690803cc765cad5d18274a82d415931e9ad` |
| `.agents/skills/execution-profile-router/SKILL.md` | `40b7a5c9a2d8896b3e2f8c384300979a13d7d35986a5bd4bc4d3b5760a7d52b7` |
| `docs/epics/forensics-platform-runtime-replay-llm-analysis-v0.2.md` | `54ff246b4359e1eb92c7e80058db42faa079ff5ffd3db0d71170cfaa3dbb68fe` |
| `docs/adr/ADR-0013-data-ownership-per-service.md` | `4114bd8f39a60539bba18bcc32de481aff5a91cec34d556ca1149747b04879ab` |
| `docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md` | `57055ae1b371d5c229dd8d64450a23869dc89849808fa68bdff45901a5123a69` |
| `docs/arc42/05-building-block-view.md` | `6a6d027e0a5722fca72e4148cc53d052508259be6430c52b6b1b2edacc8148ef` |
| `docs/arc42/07-deployment-view.md` | `85d944e7cb0a574284c59db35885d273d2a5959f64737440facc2d40eebcd026` |
| `docs/arc42/08-crosscutting-concepts.md` | `7b27cf1afdd1438ba7b8ad680d3137e590a59a76358493520d2feab73d172452` |
| `docs/arc42/09-architecture-decisions.md` | `90c63b43a7c3a94f92cfaa82004729437abd08b04283e3a2014e01dbf2792532` |
| `docs/architecture/data-ownership.md` | `3e46f719cc7859c4f81e4317fd8d25c1fc0c20fb8bc70a55707cedd66be8ddb8` |
| `docs/architecture/service-boundaries.md` | `5b64534514fdea4cec2e8e6953252cd416b51fe515a56c630ec32ba992070f9f` |

The context pack is stale when any recorded hash changes before workflow
execution.
