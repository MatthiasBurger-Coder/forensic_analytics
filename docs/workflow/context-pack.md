# Workflow Context Pack

## Identity

| Field | Value |
|---|---|
| Workflow version | `fa-mvp-0001-workspaces-management-extension-20260525-v1` |
| Workflow branch | `feature/workflow-workspaces-management-20260525` |
| Process strand | `workflow execute` in progress; refined after S02 |
| Execution profile | `FULL_PATH` |
| Requirement ID | `FA-MVP-0001-EXT-01` |

## Affected Areas

- `repository-source-service` workspace application, ports, H2/memory adapters
  and gRPC inbound adapter.
- `query-report-api-service` public workspace facade.
- `contracts/openapi/gateway-api.yaml`.
- `contracts/grpc/repository-analysis.proto`.
- `forensic-ui` routing, API adapter, workspace list and tests.
- `docs/workflow` and arc42 synchronization notes.

## Forbidden Areas

- Platform workspace membership, project membership, asset and retention
  lifecycle.
- New `workspace-service`.
- JavaParser, Joern, BTM generation, replay, graph, reports, vector storage,
  LLM and plugin behavior.
- Direct H2, filesystem, Git, gRPC or internal-service access from browser/UI.
- Query-report direct reads of repository-source H2 files or workspace paths.
- Hard deletion of repository-source H2 workspace provenance.

## Required Roles

- Senior Requirement Engineer.
- Senior System Architect.
- Senior Java Backend Developer.
- Senior gRPC/Protobuf Specialist.
- Senior React Frontend Developer.
- Senior Tester.

## Conditional Roles

- Senior UX Designer for list/action experience.
- Security reviewer or Senior Security Sandbox Engineer for path, remote and
  secret leakage risks.
- Senior Documentation Engineer for S07 documentation closure.

## Quality Commands

Minimum:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Frontend:

```bash
cd forensic-ui && npm ci
cd forensic-ui && npm run test
cd forensic-ui && npm run build
```

Targeted backend:

```bash
./gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace
```

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
| `.agents/skills/execution-profile-router/SKILL.md` | `40b7a5c9a2d8896b3e2f8c384300979a13d7d35986a5bd4bc4d3b5760a7d52b7` |
| `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md` | `95c04f47127f5149bb39a7e1b82b2690803cc765cad5d18274a82d415931e9ad` |
| `contracts/openapi/gateway-api.yaml` | `553e5a459389c4c0f08b9f40701b6a490c5c3e634881fc2b80dc20ad9f78127c` |
| `contracts/grpc/repository-analysis.proto` | `9213e97743736dc808e5ee50e7a26849c7e01a3001f02e1606b9aba82ba79cc1` |

The context pack is stale when any recorded hash changes, when the workflow
branch changes, or when contract/data ownership conflicts are found.

## Refinement Notes

During `workflow execute`, S03 was refined after S02 because the
repository-source gRPC endpoint did not yet expose the S01 list and
cleanup-by-id owner API methods. The refined workflow inserts a
repository-source owner gRPC endpoint slice before the query-report public
facade slice and renumbers the remaining slices.
