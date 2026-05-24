# Workflow Context Pack

## Identity

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-final-legacy-source-retirement-20260523-v2` |
| Requirement ID | `FA-MSA-001-LMR-FINAL` |
| Parent requirement | `FA-MSA-001` |
| Branch | `architecture/workflow-legacy-module-retirement-20260522` |
| Process strand | `workflow execute` |
| Execution profile | `FULL_PATH` |
| Created | `2026-05-23` |

## Purpose

This context pack is a navigation aid for final legacy source-tree retirement.
It does not replace root `AGENTS.md`, `QUALITY.md`, ADRs, arc42, routing rules,
workflow files, role files or skill files.

## Affected Areas

- Retired legacy `forensic-analytics-*` source trees and S05 deletion evidence.
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
| `docs/adr/ADR-0017-target-microservices-service-landscape.md` | `1c6be5f82ea08e38d669d335c3594eeb8fc4484d119e70c4eb8a443c47437b4b` |
| `docs/adr/ADR-0022-final-modular-monolith-source-tree-retirement.md` | `0cdcf9db5526348ad2d726b4db6932b49deb3f72c6b422a04151bb6b83b3ec2b` |
| `docs/arc42/05-building-block-view.md` | `05e0f0b0f30672b170fd6dc63839ff565bde1ec7ab031fa6e6c3e240fc556906` |
| `docs/arc42/07-deployment-view.md` | `407c1b433668e1fb92ef9ed0dccfff2b4fd4717d999fff0f2ce3d93b7afe7cc0` |
| `docs/arc42/08-crosscutting-concepts.md` | `f30883219a9f00ce54b439bf0143c47bd47a6fdd9bcadb0866efe9c2e44de7d7` |
| `docs/architecture/current-state.md` | `5a0a1eb76e2b00a5e9018be5ffeb271dff120ec199cd68ec4df5cb706582e816` |
| `docs/architecture/current-build-and-test-map.md` | `6510a04a7ee6befeb9ef47d3d9233213497321986385fdfa0a929e26376d50b6` |
| `docs/architecture/current-coupling-map.md` | `9208c980b2a78201fb42059bd2f3b8bbfc5ee1532c81cff1b93d7663d1fce1a4` |
| `docs/architecture/service-migration-map.md` | `ebe81c324d77570354c3146efcdd8405c998c63f220a1e1e637771d98e595908` |
| `docs/architecture/service-boundaries.md` | `88ea1fb0533811cb69ff72fecbeb047dd999ef24671f139e1efdf52f4a011224` |

## Staleness Rules

Re-read governing files before execution when:

- any hash above changes;
- the active branch changes;
- `docs/workflow/workflow.md` is edited;
- `settings.gradle.kts`, root build logic, contracts, ADRs or arc42 files
  change;
- a slice touches governance files, quality commands or service-boundary
  authority.
