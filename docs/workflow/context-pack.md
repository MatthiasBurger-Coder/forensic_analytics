# Workflow Context Pack

## Identity

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-final-legacy-source-retirement-20260523-v1` |
| Requirement ID | `FA-MSA-001-LMR-FINAL` |
| Parent requirement | `FA-MSA-001` |
| Branch | `architecture/workflow-legacy-module-retirement-20260522` |
| Process strand | `workflow create` |
| Execution profile | `FULL_PATH` |
| Created | `2026-05-23` |

## Purpose

This context pack is a navigation aid for final legacy source-tree retirement.
It does not replace root `AGENTS.md`, `QUALITY.md`, ADRs, arc42, routing rules,
workflow files, role files or skill files.

## Affected Areas

- Tracked legacy `forensic-analytics-*` source trees.
- Service-only Gradle project model.
- Docker and boot-app deployment documentation.
- Public REST/OpenAPI, CLI and gRPC compatibility vocabulary.
- Regression ownership after deleting module-local legacy tests.
- arc42, ADR, README, testing docs and architecture maps.
- Workflow execution reports and context hashes.

## Forbidden Areas

- Re-registering any `forensic-analytics-*` Gradle project.
- Shared Java implementation, domain, DTO, repository, service, utility,
  fixture, logging, persistence or error-model modules between services.
- Direct service-to-service Gradle project dependencies.
- Direct cross-service database, private filesystem or workspace access.
- Treating static facts as runtime execution evidence.
- Treating LLM output as verified evidence.
- Claiming runtime, Docker, healthcheck, Swarm or Kubernetes readiness from
  source-tree deletion alone.

## Required Roles And Specialist Reviewers

- Senior Requirement Engineer.
- Senior System Architect.
- Senior Execution Orchestrator.
- Senior Java Backend Developer.
- Microservice Senior Expert.
- Contract-First API Steward.
- Contract Governance Expert.
- Senior DevOps Engineer.
- Senior Tester.
- Senior Documentation Engineer.
- ADR Steward.
- Microservice Runtime Readiness Expert.
- Senior React Frontend Developer for public API impact checks.

## Quality Commands

Minimum quality command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Workflow creation verification:

```bash
python3 -m json.tool docs/workflow/context-pack.json
git diff --check
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
| `.agents/skills/workflow-slice/SKILL.md` | `f58db9f89a32d6312c767d3d954aaf374a7bbe12c25915c0101489c990a54976` |
| `.agents/skills/execution-profile-router/SKILL.md` | `40b7a5c9a2d8896b3e2f8c384300979a13d7d35986a5bd4bc4d3b5760a7d52b7` |
| `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md` | `95c04f47127f5149bb39a7e1b82b2690803cc765cad5d18274a82d415931e9ad` |
| `.agents/skills/microservice-migration-safety-gate/SKILL.md` | `8579030acf72d513a3386d1325e73f146c5185106d633adc07c138eaad175f15` |
| `.agents/skills/engineering-governance/SKILL.md` | `9854c03e71c499701aa18315d3d1281d6fc00e74c53e022d35fc4d5119f2b9b0` |
| `.agents/skills/arc42-architecture-governance/SKILL.md` | `abd6939fce486ef7d80caf2f40206a5a59541c3c0b8306682142b8492091b3fc` |
| `settings.gradle.kts` | `1f0e681f1286f377e4671dc461e76c7449ceac81b1351c6e65dab139584c7122` |
| `build.gradle.kts` | `c9f1866871a500675f725c47795606a8ab19c9e64ba65c39e907b0cc19b8a7c1` |
| `docs/adr/ADR-0017-target-microservices-service-landscape.md` | `ddf2d281e8bb8d8924f4622e532da29e8f94038a66ae57d3d06a4ff85e72e95f` |
| `docs/arc42/05-building-block-view.md` | `ffe560fe889fbcd51147688c98b5103b6f05a2aa72ba3d581a172580a1074bde` |
| `docs/arc42/07-deployment-view.md` | `01faa935ffac09d7fd83c4ce04fe45934346abd529cd02c2e04ef7c3445b4ea9` |
| `docs/arc42/08-crosscutting-concepts.md` | `bf22a6a559b07b56c4d6617f6c3c61d3684daf3973f7badcdf5dc067b8bef0e7` |

## Staleness Rules

Re-read governing files before execution when:

- any hash above changes;
- the active branch changes;
- `docs/workflow/workflow.md` is edited;
- `settings.gradle.kts`, root build logic, contracts, ADRs or arc42 files
  change;
- a slice touches governance files, quality commands or service-boundary
  authority.
