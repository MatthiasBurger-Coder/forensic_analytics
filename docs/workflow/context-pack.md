# Workflow Context Pack

## Identity

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-legacy-module-retirement-20260522-v2` |
| Requirement ID | `FA-MSA-001-LMR` |
| Parent requirement | `FA-MSA-001` |
| Branch | `architecture/workflow-legacy-module-retirement-20260522` |
| Process strand | `workflow execute` |
| Execution profile | `FULL_PATH` |
| Created | `2026-05-22` |

## Purpose

This context pack is a navigation aid for the legacy module retirement
workflow. It does not replace root `AGENTS.md`, `QUALITY.md`, ADRs, arc42,
routing rules, workflow files, role files or skill files.

## Affected Areas

- Legacy `forensic-analytics-*` Gradle module registration.
- Service-local migration and caller-free proof.
- REST, gRPC, CLI, events and file contracts.
- Persistence ownership and stored evidence boundaries.
- Runtime boot, bootstrap and Docker/readiness evidence.
- Observability/logging decoupling.
- Testbed regression parity and legacy dependency exit.
- S14 retirement readiness reconciliation.
- S19 final Gradle deregistration and source-tree deletion.

## Forbidden Areas

- Shared Java domain, application, DTO, repository, utility, fixture, logging,
  persistence or error-model modules between productive services.
- Direct Gradle project dependencies between services.
- Direct cross-service database, table, private filesystem or workspace access.
- Treating static facts as runtime execution evidence.
- Treating LLM output as verified evidence.
- Removing legacy modules without caller-free proof, replacement tests,
  rollback or deprecation notes and required quality-gate success.

## Required Roles And Specialist Reviewers

- Senior Requirement Engineer.
- Senior System Architect.
- Senior Execution Orchestrator.
- Senior Java Backend Developer.
- Microservice Senior Expert.
- Contract-First API Steward.
- Senior gRPC/Proto Specialist.
- Senior Analysis Storage Architect.
- Data Ownership and Persistence Steward.
- Senior DevOps Engineer.
- Senior Security/Sandbox Engineer.
- Senior Git Workspace Specialist.
- Senior Joern CPG Specialist.
- Ingestion Handoff Review.
- Source Analysis Pipeline.
- Distributed Systems Architect.
- Contract Governance Expert.
- Observability Runtime Diagnostics.
- Security Threat Modeling.
- ArchUnit Review.
- Senior UX Designer.
- Microservice Runtime Readiness Expert.
- Senior Tester.
- Senior Documentation Engineer.
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
| `.agents/skills/three-amigos-requirement-gatekeeper/SKILL.md` | `95c04f47127f5149bb39a7e1b82b2690803cc765cad5d18274a82d415931e9ad` |
| `.agents/skills/execution-profile-router/SKILL.md` | `40b7a5c9a2d8896b3e2f8c384300979a13d7d35986a5bd4bc4d3b5760a7d52b7` |
| `.agents/skills/microservice-migration-safety-gate/SKILL.md` | `8579030acf72d513a3386d1325e73f146c5185106d633adc07c138eaad175f15` |
| `docs/workflow/workflow.md` | `0187aec839959dba002e123b820ecf3b442ed38fd4799cd960ca1c453f9ad7dd` |
| `docs/workflow/slice-dependency-map.md` | `625546d35a0774cc33b7f958d6b70d538275a10535a4975935fc2e10511b2cdf` |
| `docs/workflow/quality-and-leakage-gates.md` | `25f99e669868f23c6bb704d88c74e6d1e3428bdc4749f97dd7dd6d5090b6b5f4` |
| `docs/workflow/three-amigos-decision-record.md` | `34639d46c625f9daf2eca3d74556625144e2d75bfe5c5c3f266f44ccf279e14c` |
| `docs/workflow/execution-report.md` | `4c4f326d46b5bc97242cfdc59eb760bd8ebf250ebaddb7dde05bb04793fc1832` |
| `docs/workflow/role-ownership.md` | `d0d1ccdbd149dbdbe77097d40f4b0bfa1c4c7923a6aea8634313865135c15b7e` |
| `docs/workflow/arc42-check-status.md` | `ea164adde7b8ac459d5b94fdae952e64635c8d4a327267dde4fe8c416a29bdae` |
| `docs/adr/ADR-0017-target-microservices-service-landscape.md` | `ddf2d281e8bb8d8924f4622e532da29e8f94038a66ae57d3d06a4ff85e72e95f` |
| `docs/architecture/target-microservices-architecture.md` | `f78ae9aa0e7f1ee7446f07b564408a6ff5610f7e1b9f6702823e9d38363a3550` |
| `docs/architecture/current-coupling-map.md` | `d8ac37e4304bbce12ecf6ec1ea438b8f9636497a18174faa9079cade58040050` |
| `docs/architecture/service-migration-map.md` | `9c5a34d1acf9b6e11098a6a57ebecd979350dde6f1b5584170312319c3e6e66d` |
| `docs/architecture/service-boundaries.md` | `8f8596a7678738726f92fdf8cd27ffe5fb7b9cbc792042289e0dab415e8691e2` |
| `docs/architecture/service-communication-matrix.md` | `718050f8c6fead496896677df4ac5e76dd3423979292a9c1fd5cb4cd9250cd7d` |
| `docs/architecture/monolith-runtime-isolation.md` | `6da99439a89aa42a9e1c8e5190e76bbb008792f595e4374aa2ac768656cdf338` |
| `docs/architecture/current-build-and-test-map.md` | `c6508d5e584d2be93d1757b667643ac8dc7d83cfc3a81040cddf283ab23259de` |
| `docs/arc42/05-building-block-view.md` | `ffe560fe889fbcd51147688c98b5103b6f05a2aa72ba3d581a172580a1074bde` |
| `docs/arc42/06-runtime-view.md` | `8fa3a04d05e3bb18239b6ae363b638b65c4ebe6958799b0c0a0fbfd85309ac7e` |
| `docs/arc42/07-deployment-view.md` | `01faa935ffac09d7fd83c4ce04fe45934346abd529cd02c2e04ef7c3445b4ea9` |
| `docs/arc42/08-crosscutting-concepts.md` | `bf22a6a559b07b56c4d6617f6c3c61d3684daf3973f7badcdf5dc067b8bef0e7` |
| `docs/testing/wildfly-hardening.md` | `53f3ad4465507873bd6fb4eada92e41aedda4550c0ae53e952a95c4919c0bd69` |
| `contracts/cli/gateway-cli-contract.md` | `dfbef3d8f57c15f18fd2eca5b161393380c71bf607b25c385a95701a24fb1cdd` |
| `services/cli-client/README.md` | `bb5c87baeee17544f9e7812f8786c62e42924de32cfb895d4c3af4854770e11c` |
| `services/query-report-api-service/README.md` | `4e3dabc131398c1b30db50ad049ce123916dbb60e4333cca9cec00af26468dab` |
| `services/java-parser-analysis-service/README.md` | `3c9de6fda08c5d1961e9d92f37a531173b81d6c9d4f52971ea18d634ec664b4d` |
| `services/joern-analysis-service/README.md` | `bca7d1707e10b06f30ed95fb51f0b0dc3e2c7ea06f998b86847d6e7e935eec7a` |
| `services/testbed/README.md` | `679fa3b3076a46ff852ba4ec57e7b7abbb7d3479c3f1267d7caf6599207e2983` |
| `settings.gradle.kts` | `f5be0d269f3a0ec5d36a9e7787a1802bebc777ab44c7544fb5759d09f53eb6a7` |
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
