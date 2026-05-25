# Workflow Context Pack

## Active Workflow

| Field | Value |
|---|---|
| Workflow version | `fa-mvp-0001-workspace-branch-selection-20260525-v1` |
| Process strand | `workflow create` completed; `workflow execute` pending |
| Active branch | `feature/workflow-branch-selection-20260525` |
| Execution profile | `FULL_PATH` |
| Requirement ID | `FA-MVP-0001-EXT-02` |

## Affected Areas

- `forensic-ui` Workspaces list.
- Public workspace branch DTO consumption.
- Branch refresh target selection.
- Workflow and arc42 documentation.

## Forbidden Areas

- Remote Git branch discovery.
- Browser Git, browser gRPC or direct internal service access.
- New REST/gRPC methods.
- Repository-source H2, filesystem, Git adapter, Docker, CI or deployment
  changes.
- Analysis, JavaParser, Joern, BTM, replay, report, graph, vector, LLM or
  plugin behavior.

## Required Roles

- Senior Requirement Engineer
- Senior System Architect
- Senior Java Backend Developer
- Senior React Frontend Developer
- Senior Tester

## Conditional Roles

- Senior UX Designer for selector accessibility.
- Senior Documentation Engineer for S04 documentation closure.
- Contract governance reviewer only if S01 stops for contract-first recut.

## Quality Commands

```bash
cd forensic-ui && npm run test -- src/pages/workspaces/WorkspaceListPage.test.tsx
cd forensic-ui && npm run build
./gradlew test --dependency-verification strict --console=plain --stacktrace
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
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
| `contracts/openapi/gateway-api.yaml` | `553e5a459389c4c0f08b9f40701b6a490c5c3e634881fc2b80dc20ad9f78127c` |
| `contracts/grpc/repository-analysis.proto` | `9213e97743736dc808e5ee50e7a26849c7e01a3001f02e1606b9aba82ba79cc1` |
| `forensic-ui/package.json` | `cdb23841e48ee73a4dbddac5e2fafb7f134f3be77631e5ece5931c58d884f57b` |
| `docs/arc42/08-crosscutting-concepts.md` | `2cb180c0132d0f3148433cc6066623d18a3406baedcfda02acfc857d439ef41f` |
| `docs/arc42/06-runtime-view.md` | `904f93d560d3f5ebf4466c675c6f7a55d1a5f75a3a3035c266a0f5d65580ca45` |
| `docs/adr/ADR-0010-contract-first-rest-and-grpc.md` | `9d75a1b758095b6fc40fe013d7a8af8db85441de0b5ff0401b8cc23dcf71976f` |
| `docs/adr/ADR-0016-branch-first-workflow-creation.md` | `13e8e922831c0a7ffe95ee8fae9247bf39b9f47ecf30dd9f685e9fe447fb95e2` |
| `docs/adr/ADR-0023-h2-for-repository-source-mvp-persistence.md` | `57055ae1b371d5c229dd8d64450a23869dc89849808fa68bdff45901a5123a69` |

## Staleness Rules

This context pack is stale when any recorded hash changes, when S01 changes
branch semantics, when backend or contract work becomes necessary, or when the
active branch is not `feature/workflow-branch-selection-20260525`.
