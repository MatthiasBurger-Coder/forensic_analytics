# Workflow Context Pack

## Identity

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-microservice-decomposition-20260521-v1` |
| Requirement ID | `FA-MSA-001` |
| Branch | `architecture/workflow-microservice-decomposition-20260521` |
| Process strand | `workflow create`; later `workflow execute` |
| Execution profile | `FULL_PATH` |
| Created | `2026-05-21` |

## Purpose

This context pack is a navigation aid for executing the FA-MSA-001 workflow. It
does not replace root `AGENTS.md`, `QUALITY.md`, ADRs, arc42 documents,
routing rules, active workflow files or role/skill files.

## Affected Areas

- Microservice service landscape and naming.
- Gradle module registration and old shared module retirement.
- REST, gRPC, messaging and file contracts.
- Data ownership and persistence split.
- Repository checkout, ingestion, JavaParser and Joern analysis boundaries.
- Analysis orchestration, query/report API, CLI, observability and testbed.
- Dockerfile, runtime start and architecture-test readiness.

## Forbidden Areas

- Shared Java domain, application, DTO, repository, utility, fixture, logging or
  error-model modules between services.
- Direct Gradle project dependencies between services.
- Direct cross-service database, table, filesystem or private workspace access.
- Treating static facts as runtime execution evidence.
- Treating LLM output as verified evidence.
- Claiming Docker Swarm or Kubernetes readiness without verified files and
  commands.
- Removing legacy modules without caller-free proof and replacement tests.

## Required Roles

- Senior Requirement Engineer.
- Senior System Architect.
- Microservice Senior Expert.
- Senior Java Backend Developer.
- Senior gRPC/Proto Specialist.
- Contract-First API Steward.
- Data Ownership and Persistence Steward.
- Senior DevOps Engineer.
- Senior Security/Sandbox Engineer.
- Senior Tester.
- Senior Documentation Engineer.
- Senior React Frontend Developer as an impact check unless public API changes
  affect frontend code.

## Quality Commands

Minimum quality command:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Documentation-only workflow creation verification:

```bash
git diff --check
```

## Governing File Hashes

The companion JSON file records the same hashes in machine-readable form. The
context is stale when any recorded hash changes without review.

| File | SHA-256 |
|---|---|
| `AGENTS.md` | `318d6bb0c2b54d3ff42827692f0c8afa9c7a8846356a57eb8d4e98947c435c7a` |
| `QUALITY.md` | `9df68c96dbdd9bf36e139cfdb2cc91c341a5c08c928f03d8fe24392281fcb848` |
| `.codex/AGENTS.md` | `d43bc1ea9ca10deb8a6553c7c21208e8020d751f674a8cb824959b666bd01aec` |
| `.codex/workflow/workflow-execution-rules.md` | `6d2ea74943f8ff4187a371d6e263fcf7f179d7250868086f4ba294e6f5fa122a` |
| `.agents/orchestrator/routing-rules.md` | `30cd2a044746ab97f798425dd8f8125a98c6ed50d2d70a1b0778dca353c325bf` |
| `.agents/orchestrator/swarm-orchestrator.md` | `ae501a9e61ec0a9cf4acaad7fb7fd5d6167309b722370ed5d21d1991e49c09fc` |
| `.agents/skills/workflow-authoring/SKILL.md` | `d87950d6d9ca831a4201b660c6bef373cb85be829f21694a323dbb9b8544d801` |
| `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md` | `95c04f47127f5149bb39a7e1b82b2690803cc765cad5d18274a82d415931e9ad` |
| `.agents/skills/execution-profile-router/SKILL.md` | `40b7a5c9a2d8896b3e2f8c384300979a13d7d35986a5bd4bc4d3b5760a7d52b7` |
| `.agents/skills/microservice-migration-safety-gate/SKILL.md` | `8579030acf72d513a3386d1325e73f146c5185106d633adc07c138eaad175f15` |
| `docs/adr/ADR-0017-target-microservices-service-landscape.md` | `18ce4805a028d443b68140139b93345c3d9b720986738ef95c0669580bcca50c` |
| `docs/architecture/target-microservices-architecture.md` | `540914eed2bf9fce84b02b22fa6445a32e7618fe94120f1756a92caea74fb677` |
| `settings.gradle.kts` | `493933db7fafc6f79c0958492ab31fa652f2898537a5ff9267ac31f710d8a6c4` |
| `build.gradle.kts` | `c9f1866871a500675f725c47795606a8ab19c9e64ba65c39e907b0cc19b8a7c1` |

## Staleness Rules

Re-read governing files before execution when:

- any hash above changes;
- the active branch changes;
- `docs/workflow/workflow.md` is edited;
- `settings.gradle.kts`, root build logic, contracts, ADRs or arc42 files
  change;
- a slice touches governance files, quality commands or service-boundary
  authority.
