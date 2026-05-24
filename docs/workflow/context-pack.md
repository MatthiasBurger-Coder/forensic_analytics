# Workflow Context Pack: FA-MVP-0001

## Purpose

This context pack is a navigation aid for the active workflow. It does not
replace `AGENTS.md`, `QUALITY.md`, ADRs, arc42, routing rules, active workflow
files or skill files.

## Active Workflow

| Field | Value |
|---|---|
| Workflow version | `fa-mvp-0001-repository-workspace-checkout-h2-persistence-20260524-v1` |
| Requirement ID | `FA-MVP-0001` |
| Workflow branch | `feature/workflow-repository-workspace-checkout-h2-persistence-20260524` |
| Process strand | `workflow create` now; `workflow execute` later |
| Execution profile | `FULL_PATH` |
| Decision | `PROCEED_WITH_ACCEPTED_ASSUMPTIONS` |

## Affected Areas

- `services/repository-source-service`
- `services/query-report-api-service`
- `forensic-ui`
- `contracts/openapi/gateway-api.yaml`
- `contracts/grpc/repository-analysis.proto`
- `deployment/docker-compose/repository-to-btm.local.yml`
- `services/repository-source-service/Dockerfile`
- `gradle/libs.versions.toml`
- `gradle/verification-metadata.xml`
- `docs/architecture/**`
- `docs/arc42/**`
- `docs/contracts/**`

## Forbidden Areas

- No new `workspace-service`.
- No JavaParser, Joern, BTM, replay, report generation or LLM implementation.
- No PostgreSQL, Neo4j, vector database, Kafka, RabbitMQ, Docker Swarm or
  Kubernetes implementation.
- No shared Java implementation, domain, DTO, repository, utility, fixture,
  persistence or internal error-model modules between services.
- No browser calls to Git remotes, gRPC or internal service endpoints.
- No public exposure of private filesystem paths, H2 paths, raw stdout, raw
  stderr, credentials or tokens.

## Required Roles

- Senior Requirement Engineer
- Senior System Architect
- Senior Java Backend Developer
- Senior React Frontend Developer
- Senior Tester
- Contract Governance Expert
- Senior gRPC/Proto Specialist
- Data Ownership & Persistence Steward
- Senior Analysis Storage Architect
- Senior Git Workspace Specialist
- Senior DevOps Engineer
- Security / Sandbox Reviewer
- Resilience Reviewer
- Senior UX Designer
- Senior Documentation Engineer
- ADR Steward

## Quality Commands

Minimum:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Full local quality gate:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

Targeted backend gates:

```bash
./gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace
./gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace
```

Frontend gates:

```bash
cd forensic-ui
npm ci
npm run test
npm run build
```

Docker-local model gate:

```bash
docker compose -f deployment/docker-compose/repository-to-btm.local.yml config
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
| `.agents/skills/workflow-slice/SKILL.md` | `f58db9f89a32d6312c767d3d954aaf374a7bbe12c25915c0101489c990a54976` |
| `contracts/grpc/repository-analysis.proto` | `452a5defb7110624beec86aa55313bbba9b0be6da062d9d96c8d5075af9339d4` |
| `contracts/openapi/gateway-api.yaml` | `083494fb29700e5e034e4b58630648d0ec5ad499f87ea4add0c90629fdbc0436` |
| `gradle/libs.versions.toml` | `6dda5d048c065e48e2e94c40a18f5ce6e6de9929261b6a791338ac7ecc1a68bb` |
| `deployment/docker-compose/repository-to-btm.local.yml` | `775f7ca20a42c85280375d6ac8e03cc9ae8da5ed9f6e92a07512c8b95eb46061` |
| `services/repository-source-service/Dockerfile` | `51c3a5a5bc4f15b24948ae29ed4d30b27377d026990a0ad11f666a8eb08d513e` |
| `services/repository-source-service/build.gradle.kts` | `6b2658d938e8f696590b1cdcf2f4504a0debe8a590aaebe0ac870172b6896e92` |
| `services/query-report-api-service/build.gradle.kts` | `805d4c1a9855e9d68cf2f7348d731e32a6781f019013c1600c1e3380872ae368` |
| `forensic-ui/package.json` | `cdb23841e48ee73a4dbddac5e2fafb7f134f3be77631e5ece5931c58d884f57b` |

## Staleness Rules

This context pack is stale when any recorded hash changes, when the active
branch changes, when `docs/workflow/workflow.md` changes without updating the
context pack, or when a slice discovers a contract, quality command,
architecture rule or ownership conflict not recorded here.
