# Workflow: E2E Repository Hardening, CLI Gateway Contract And Monolith Caller Retirement

## Workflow Version

| Field | Value |
|---|---|
| Workflow version | `e2e-wildfly-cli-deploy-20260521-v1` |
| Workflow branch | `feature/workflow-e2e-wildfly-cli-deploy-20260521` |
| Creation status | Created by `workflow create`; execution requires a clean committed workflow package. |
| Process strand | `workflow create` now; later `workflow execute` for slices. |
| Execution profile | `FULL_PATH` |

## Executive Summary

This workflow turns the requested hardening and migration goals into executable
slices:

- run a real end-to-end repository analysis test against a deterministic real
  test repository rather than only synthetic source strings;
- prepare WildFly as an explicit opt-in large repository hardening scenario;
- define the CLI-to-Gateway contract before changing CLI runtime behavior;
- split Docker Swarm and Kubernetes deployment into a separate workflow;
- make legacy monolith paths caller-free step by step, with caller evidence,
  contract parity and rollback gates before removal.

The workflow is product-impacting and architecture-sensitive. It touches test
strategy, public contracts, CLI behavior, service migration and deployment
governance. It must therefore use `FULL_PATH` review depth during execution.

## Requirement Clarification Decision

| Field | Decision |
|---|---|
| Original request | Create a workflow for: real end-to-end test with a real test repository; prepare WildFly as hardening case; define CLI Gateway contract; start Swarm/Kubernetes deployment as a separate workflow; make old monolith paths caller-free step by step. |
| Interpreted intent | Create an executable workflow that hardens repository-to-BTM verification, adds contract-first CLI-to-Gateway migration, and safely retires legacy in-process paths only when caller evidence proves replacement parity. |
| Change type | Backend tests, contract governance, CLI adapter migration, microservice migration governance, deployment workflow handoff and documentation synchronization. |
| Affected process strand | `workflow create` now; later `workflow execute`. |
| Affected architecture area | Repository analysis, Gateway facade contract, CLI adapter boundary, service migration, large-repository checkout, legacy monolith retirement and deployment governance. |
| Explicit requirements | Real E2E repository test; WildFly hardening preparation; CLI Gateway contract; separate Swarm/Kubernetes workflow; caller-free legacy paths. |
| Implicit requirements | Deterministic tests by default; no external network in the default quality gate; contract-first migration; no shared service implementation modules; no deployment readiness claim without manifests and validation commands; no module removal without caller proof. |
| Assumptions | "Real test repository" means a deterministic repository fixture committed to the test scope or materialized locally during tests, not an external network checkout in the default test suite. Swarm/Kubernetes is started as a separate workflow handoff, not implemented inside this workflow. |
| Non-goals | No live WildFly network run in the default gate; no Swarm stack or Kubernetes manifests in this workflow; no removal of monolith modules until caller-free evidence exists; no shared Java DTO or fixture modules between services. |
| Risks | External repository size and network instability; accidental workspace path or secret leakage through Gateway/CLI output; breaking CLI users; removing legacy paths before replacement parity; deployment readiness overclaiming. |
| Open questions | Which WildFly branch or commit should be used for the opt-in hardening run during execution. This is non-blocking because the workflow requires the value before running the external test. |
| Blocking questions | None for workflow creation. |
| Confidence | 91 percent. |
| Decision | `READY_FOR_WORKFLOW`. |

## Verified Baseline

Read-only verification before workflow authoring found:

- Repository root: `/mnt/d/Projects/forensic_analytics`.
- WSL repository access: available from the Windows-hosted worktree.
- Workflow branch created and verified: `feature/workflow-e2e-wildfly-cli-deploy-20260521`.
- Working tree before workflow regeneration: clean.
- Existing workflow package was removed and regenerated on the workflow branch.
- Quality authority: `QUALITY.md`.
- Minimum quality command:
  `./gradlew test --dependency-verification strict --console=plain --stacktrace`.
- Full local quality gate:
  `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`.
- Root `checkPackageCoverage` task exists in `build.gradle.kts`.
- Current Gradle modules are registered in `settings.gradle.kts`, including
  `forensic-analytics-testbed`, `forensic-analytics-cli`,
  `forensic-analytics-rest` and the implemented `services/**` projects.
- `forensic-analytics-testbed` already contains
  `RepositoryAnalysisMiniEndToEndTest`, which uses a synthetic local Git
  repository and in-process gRPC.
- `forensic-analytics-testbed` already contains
  `WildFlyRepositoryHardeningTest`, tagged `hardening` and gated by
  `FORENSIC_ANALYTICS_WILDFLY_HARDENING=true`, branch or commit input, timeout,
  disk and report-directory environment variables.
- `contracts/openapi/gateway-api.yaml` exists and marks repository-to-BTM
  submission and status routes as current verified.
- `forensic-analytics-rest/src/test/java/.../GatewayOpenApiContractTest.java`
  checks the Gateway OpenAPI contract and public leakage rules.
- `services/forensic-gateway-service` has HTTP adapter tests for public
  Gateway facade behavior, idempotency, validation and diagnostic redaction.
- `deployment/docker-swarm/README.md` and `deployment/kubernetes/README.md`
  are planned roots only; no stack file or manifests exist.
- `deployment/docker-compose/repository-to-btm.local.yml` is the verified
  local repository-to-BTM Compose descriptor, but it does not claim Swarm or
  Kubernetes readiness.
- `docs/architecture/service-migration-map.md` records that legacy
  `forensic-analytics-*` paths remain because active callers still exist.

## Target Picture

After workflow execution, the repository should have:

```text
real local repository E2E fixture
  -> repository-to-BTM path tested with deterministic inputs

WildFly opt-in hardening runbook
  -> explicit branch or commit, timeout, disk budget, metrics and cleanup proof

CLI Gateway contract
  -> CLI commands mapped to Gateway public API without exposing workspaces

caller evidence
  -> CLI migration first, then legacy runtime paths retired only when caller-free

deployment workflow handoff
  -> separate Swarm/Kubernetes workflow request with no manifest claims here
```

## Scope

In scope:

- Testbed E2E tests and deterministic repository fixtures.
- Optional WildFly hardening documentation and test harness refinement.
- Gateway OpenAPI contract and CLI-to-Gateway contract documentation.
- CLI adapter tests and implementation only after the contract slice passes.
- Caller inventory for legacy monolith paths.
- Removal or isolation of only those legacy callers proven replacement-safe.
- Architecture and workflow documentation synchronization.
- Separate workflow handoff for Swarm/Kubernetes deployment readiness.

## Non-Goals

Out of scope:

- Running WildFly by default in `./gradlew test`.
- Executing untrusted repository build scripts as part of checkout or E2E tests.
- Adding Docker Swarm stack files or Kubernetes manifests in this workflow.
- Claiming production deployment readiness.
- Introducing shared Java implementation, DTO, fixture, utility or error-model
  modules between services.
- Removing any Gradle module before caller-free evidence and replacement parity.
- Graph, replay, report-generation or live LLM implementation.
- Push, PR creation, PR merge, branch cleanup or `push auto`.

## Architecture Constraints

- Domain and application packages stay framework-free.
- Gateway remains a facade and must not own worker orchestration, workspace
  paths, source snapshot bytes, AST facts, Joern artifacts, BTM generation or
  canonical evidence state.
- CLI-to-Gateway work must be contract-first. Do not infer routes, request
  fields or response fields from similarly named Java classes.
- CLI and Gateway output must not expose private workspace paths, raw stdout,
  raw stderr, credentials, tokens, internal service exceptions or resolved
  private repository metadata.
- Large repository handling must preserve repository URL, branch input, commit
  input, resolved commit, checkout mode, elapsed time, cleanup result and
  diagnostics without executing target repository code.
- WildFly is an opt-in hardening scenario only. It must not become a default
  unit test dependency.
- Monolith path retirement must be one path at a time and must stop when caller
  proof, parity tests, rollback strategy or quality commands are missing.
- Docker Swarm and Kubernetes deployment readiness require a separate workflow,
  separate branch, manifests or stack files, resource policies, health probes
  and validation commands.

## Backend Assessment

Backend impact is high. The workflow may touch Java tests, CLI code, contract
tests, Gateway adapter behavior and service migration documentation. Senior
Java Backend, Senior System Architect, Contract Governance, Microservice Senior
Expert, Senior Tester and Senior DevOps reviews are required for the relevant
slices.

## Frontend Assessment

The existing frontend root is `forensic-ui`. This workflow does not plan a
frontend implementation change. Senior React Frontend performs an N/A impact
check unless the CLI/Gateway contract slice changes public Gateway fields that
the frontend API adapter consumes. If that happens, frontend adapter tests under
`forensic-ui` become required for the affected slice.

## Test Strategy

Default verification follows `QUALITY.md`:

1. Run the narrowest targeted test for the changed slice.
2. Run the affected module test command.
3. Run the repository minimum quality command when production Java, contracts,
   tests or build behavior changes:
   `./gradlew test --dependency-verification strict --console=plain --stacktrace`.
4. Run the full local quality gate before commit readiness when the workflow
   reaches broad caller removal or cross-module changes:
   `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`.
5. Always run:
   `git diff --check`.

WildFly hardening has two levels:

- Required default check: prove the hardening test stays opt-in and skips when
  required environment variables are absent.
- Optional external check: run the WildFly hardening test only when branch or
  commit, network, disk and timeout prerequisites are explicitly satisfied.

## Resilience Requirements

- External Git operations must have explicit timeouts and cleanup-after-failure
  behavior.
- Large-repository metrics must be written to a deterministic report path.
- Optional external checks must report `SKIPPED` with reasons, not success.
- CLI and Gateway failures must return stable error categories and redacted
  diagnostics.
- Caller removal must preserve rollback instructions for each retired path.
- Deployment workflow handoff must state missing tooling instead of inventing
  Swarm or Kubernetes commands.

## Role And Subagent Assignment

Callable subagents were not used during this `workflow create` turn because the
active user request did not explicitly ask for delegated or parallel agent
execution. The mandatory reviews are represented as local role-review
checklists in `docs/workflow/three-amigos-decision-record.md`.

During `workflow execute`, role or callable-subagent routing is:

- Senior Workflow Architect: overall workflow execution order, metadata and
  dependency graph.
- Senior Requirement Engineer: EPIC drift, requirement clarity and non-goals.
- Senior System Architect: architecture boundaries, service ownership and
  caller-retirement safety.
- Senior Java Backend Developer: Java tests, CLI, Gateway, contracts and
  service-adapter changes.
- Senior React Frontend Developer: N/A impact check unless Gateway contract
  changes affect `forensic-ui`.
- Senior Tester: regression strategy, targeted checks, coverage and quality
  gate expectations.
- Senior DevOps Engineer: WildFly external hardening run, Docker Compose
  evidence, and separate Swarm/Kubernetes workflow handoff.
- Microservice Senior Expert: service autonomy, no shared Java implementation
  modules and caller retirement gates.
- Contract Governance Expert: CLI/Gateway contract and compatibility.
- Git Large Repository Specialist: WildFly and real repository checkout
  hardening.
- Security Sandbox Specialist: untrusted repository handling and leakage checks.

## Ordered Slices

### Slice 00 - Execution Preflight And Context Freeze

```yaml
slice_id: S00
profile: FULL_PATH
owner: senior-workflow-architect
secondary_reviewers:
  - senior-requirement-engineer
  - senior-system-architect
  - senior-tester
affected_files:
  - docs/workflow/execution-report.md
affected_modules: []
affected_contracts: []
dependencies: []
parallel_group: G00
file_locks:
  - docs/workflow/**
contract_locks: []
architecture_locks:
  - workflow-execute-preflight
quality_gates:
  targeted:
    - "git status --short --branch"
    - "git diff --check"
  required:
    - "git status --short --branch"
    - "git diff --check"
documentation:
  arc42: check
  adr: check
stop_conditions:
  - active branch does not match the workflow branch
  - working tree contains unrelated changes
  - workflow version cannot be verified
  - context pack hashes are stale without documented acceptance
```

Purpose: verify the execution branch, clean worktree, workflow version, context
pack and active file locks before any write-capable slice starts.

Done criteria:

- Workflow branch and local ref are verified.
- `docs/workflow/context-pack.json` is checked against governing-file hashes.
- The execution report records the preflight result.

### Slice 01 - Real Repository End-To-End Test

```yaml
slice_id: S01
profile: FULL_PATH
owner: senior-tester
secondary_reviewers:
  - senior-java-backend
  - senior-system-architect
  - security-sandbox-specialist
affected_files:
  - forensic-analytics-testbed/src/test/java/de/burger/forensics/analytics/testbed/RepositoryAnalysisRealRepositoryEndToEndTest.java
  - forensic-analytics-testbed/src/test/resources/repository-e2e/**
  - docs/architecture/current-build-and-test-map.md
  - docs/workflow/execution-report.md
affected_modules:
  - forensic-analytics-testbed
  - forensic-analytics-ingestion-grpc
  - forensic-analytics-adapter-repository-source
  - forensic-analytics-persistence
affected_contracts:
  - contracts/grpc/forensic-ingestion.proto
dependencies:
  - S00
parallel_group: G01
file_locks:
  - forensic-analytics-testbed/src/test/**
  - docs/architecture/current-build-and-test-map.md
contract_locks:
  - forensic-ingestion-grpc-request-shape
architecture_locks:
  - repository-analysis-e2e
  - untrusted-repository-sandbox
quality_gates:
  targeted:
    - "./gradlew :forensic-analytics-testbed:test --tests '*RepositoryAnalysisRealRepositoryEndToEndTest' --dependency-verification strict --console=plain --stacktrace"
  required:
    - "./gradlew :forensic-analytics-testbed:test --dependency-verification strict --console=plain --stacktrace"
    - "git diff --check"
documentation:
  arc42: check runtime and quality views
  adr: none
stop_conditions:
  - real repository fixture would require network access in the default gate
  - fixture setup would execute target repository build scripts
  - checkout result cannot preserve resolved commit and cleanup evidence
  - E2E test collapses missing evidence into a successful analysis fact
```

Purpose: replace the current purely synthetic mini E2E signal with a
deterministic local real repository fixture that exercises repository checkout,
source-root detection, ingestion, session storage and cleanup.

Done criteria:

- The real repository fixture has stable source files, Git metadata and a
  deterministic commit.
- The E2E test verifies session creation, checkout status, resolved commit,
  detected source roots, cleanup and absence of private path leakage in public
  outputs.
- No external network access is required for the default test.

### Slice 02 - WildFly Hardening Preparation

```yaml
slice_id: S02
profile: FULL_PATH
owner: senior-performance-engineer
secondary_reviewers:
  - git-large-repository-specialist
  - senior-devops
  - senior-tester
  - security-sandbox-specialist
affected_files:
  - forensic-analytics-testbed/src/test/java/de/burger/forensics/analytics/testbed/WildFlyRepositoryHardeningTest.java
  - docs/testing/wildfly-hardening.md
  - docs/architecture/current-build-and-test-map.md
  - docs/workflow/execution-report.md
affected_modules:
  - forensic-analytics-testbed
  - forensic-analytics-adapter-repository-source
affected_contracts: []
dependencies:
  - S01
parallel_group: G02
file_locks:
  - forensic-analytics-testbed/src/test/java/de/burger/forensics/analytics/testbed/WildFlyRepositoryHardeningTest.java
  - docs/testing/wildfly-hardening.md
contract_locks: []
architecture_locks:
  - large-repository-checkout
  - external-git-hardening
quality_gates:
  targeted:
    - "./gradlew :forensic-analytics-testbed:test --tests '*WildFlyRepositoryHardeningTest' --dependency-verification strict --console=plain --stacktrace"
  required:
    - "./gradlew :forensic-analytics-testbed:test --tests '*WildFlyRepositoryHardeningTest' --dependency-verification strict --console=plain --stacktrace"
    - "git diff --check"
documentation:
  arc42: check quality and risk views
  adr: none
stop_conditions:
  - hardening run lacks explicit WildFly branch or commit
  - external network, disk or timeout prerequisites are not satisfied
  - cleanup target cannot be proven inside the temporary workspace
  - large-repository optimization would make commit resolution unverifiable
```

Purpose: make the existing opt-in WildFly hardening scenario operationally
usable, measurable and safe without adding it to the default test suite.

Done criteria:

- Documentation records required environment variables, branch or commit input,
  disk budget, timeout, report path and expected skip behavior.
- The default targeted command proves the test remains opt-in when hardening is
  not enabled.
- Optional execution command is documented with `SKIPPED` reporting when local
  prerequisites are missing.

### Slice 03 - CLI Gateway Contract

```yaml
slice_id: S03
profile: FULL_PATH
owner: contract-governance-expert
secondary_reviewers:
  - senior-java-backend
  - senior-system-architect
  - senior-tester
  - senior-react-frontend
affected_files:
  - contracts/openapi/gateway-api.yaml
  - contracts/README.md
  - contracts/cli/gateway-cli-contract.md
  - forensic-analytics-rest/src/test/java/de/burger/forensics/analytics/rest/GatewayOpenApiContractTest.java
  - forensic-analytics-cli/src/test/java/de/burger/forensics/analytics/cli/ForensicAnalyticsCliTest.java
  - docs/architecture/service-migration-map.md
  - docs/workflow/execution-report.md
affected_modules:
  - forensic-analytics-rest
  - forensic-analytics-cli
  - services:forensic-gateway-service
affected_contracts:
  - contracts/openapi/gateway-api.yaml
  - contracts/cli/gateway-cli-contract.md
dependencies:
  - S00
parallel_group: G01
file_locks:
  - contracts/**
  - forensic-analytics-rest/src/test/java/de/burger/forensics/analytics/rest/GatewayOpenApiContractTest.java
  - forensic-analytics-cli/src/test/java/de/burger/forensics/analytics/cli/**
contract_locks:
  - gateway-openapi-v1
  - cli-gateway-contract-v1
architecture_locks:
  - gateway-facade
  - cli-public-adapter
quality_gates:
  targeted:
    - "./gradlew :forensic-analytics-rest:test --tests '*GatewayOpenApiContractTest' --dependency-verification strict --console=plain --stacktrace"
    - "./gradlew :forensic-analytics-cli:test --tests '*ForensicAnalyticsCliTest' --dependency-verification strict --console=plain --stacktrace"
  required:
    - "./gradlew :forensic-analytics-rest:test --dependency-verification strict --console=plain --stacktrace"
    - "./gradlew :forensic-analytics-cli:test --dependency-verification strict --console=plain --stacktrace"
    - "git diff --check"
documentation:
  arc42: check scope, runtime and building-block views
  adr: check ADR-0018
stop_conditions:
  - CLI route, request field or response field is not defined in a contract
  - contract marks planned behavior as implemented
  - generated or transport DTOs would leak into CLI domain logic
  - public CLI or Gateway output exposes workspace paths or sensitive values
```

Purpose: define the CLI-to-Gateway boundary before implementation. The
contract must map CLI commands and output expectations to public Gateway API
behavior without using shared Java DTOs or hidden compatibility wrappers.

Done criteria:

- CLI-to-Gateway contract exists and names producer, consumer, protocol,
  request model, response model, idempotency, retry, timeout, error and
  redaction behavior.
- OpenAPI and CLI contract tests verify the public contract.
- Frontend impact is checked and recorded.

### Slice 04 - Separate Swarm And Kubernetes Workflow Handoff

```yaml
slice_id: S04
profile: FULL_PATH
owner: senior-devops
secondary_reviewers:
  - microservice-senior-expert
  - senior-system-architect
  - senior-tester
affected_files:
  - docs/workflow/deployment-workflow-request.md
  - deployment/docker-swarm/README.md
  - deployment/kubernetes/README.md
  - docs/architecture/current-build-and-test-map.md
  - docs/workflow/execution-report.md
affected_modules: []
affected_contracts: []
dependencies:
  - S00
parallel_group: G01
file_locks:
  - docs/workflow/deployment-workflow-request.md
  - deployment/docker-swarm/README.md
  - deployment/kubernetes/README.md
contract_locks: []
architecture_locks:
  - deployment-readiness-claims
  - service-runtime-readiness
quality_gates:
  targeted:
    - "git diff --check"
  required:
    - "git diff --check"
documentation:
  arc42: check deployment view
  adr: check ADR-0017 and ADR-0018
stop_conditions:
  - slice attempts to add Swarm stack or Kubernetes manifests
  - deployment readiness is claimed without validation commands
  - service health checks, resource policies or image ownership are unclear
```

Purpose: create a separate workflow request for Docker Swarm and Kubernetes
deployment readiness without mixing deployment implementation into this
workflow.

Done criteria:

- `docs/workflow/deployment-workflow-request.md` contains a ready-to-run future
  workflow create request.
- Current Swarm and Kubernetes README files continue to state planned status
  unless a separate workflow later adds manifests and validates them.
- No stack file, manifest, chart or readiness claim is added here.

### Slice 05 - Legacy Monolith Caller Inventory And Retirement Gates

```yaml
slice_id: S05
profile: FULL_PATH
owner: microservice-senior-expert
secondary_reviewers:
  - senior-system-architect
  - senior-java-backend
  - senior-tester
affected_files:
  - docs/architecture/service-migration-map.md
  - docs/architecture/monolith-caller-retirement-plan.md
  - docs/workflow/execution-report.md
affected_modules:
  - forensic-analytics-cli
  - forensic-analytics-rest
  - forensic-analytics-bootstrap
  - forensic-analytics-boot-app
  - forensic-analytics-engine
  - forensic-analytics-ingestion-request
  - forensic-analytics-testbed
affected_contracts: []
dependencies:
  - S03
parallel_group: G03
file_locks:
  - docs/architecture/service-migration-map.md
  - docs/architecture/monolith-caller-retirement-plan.md
contract_locks: []
architecture_locks:
  - monolith-retirement
  - no-shared-java-implementation
quality_gates:
  targeted:
    - "rg -n 'RunRepositoryAnalysisUseCase|DefaultRepositoryAnalysisIngestionUseCase|forensic-analytics-application|forensic-analytics-domain' forensic-analytics-* services docs"
    - "git diff --check"
  required:
    - "git diff --check"
documentation:
  arc42: check building-block and runtime views
  adr: check ADR-0017
stop_conditions:
  - caller inventory cannot prove current callers
  - target owner for a legacy path is unclear
  - replacement parity test is missing
  - rollback or deprecation strategy is missing
```

Purpose: produce the caller map and retirement rules before changing or
removing any legacy in-process path.

Done criteria:

- Every legacy path has a current caller status: active caller, candidate for
  migration, caller-free candidate, or blocked.
- Each candidate names target service owner, contract, parity test, rollback
  strategy and forbidden changes.
- No module or path is removed in this slice.

### Slice 06 - CLI First Caller-Free Migration

```yaml
slice_id: S06
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - contract-governance-expert
  - senior-system-architect
  - senior-tester
  - microservice-senior-expert
affected_files:
  - forensic-analytics-cli/src/main/java/de/burger/forensics/analytics/cli/**
  - forensic-analytics-cli/src/test/java/de/burger/forensics/analytics/cli/**
  - contracts/cli/gateway-cli-contract.md
  - docs/architecture/monolith-caller-retirement-plan.md
  - docs/workflow/execution-report.md
affected_modules:
  - forensic-analytics-cli
  - services:forensic-gateway-service
affected_contracts:
  - contracts/cli/gateway-cli-contract.md
  - contracts/openapi/gateway-api.yaml
dependencies:
  - S03
  - S05
parallel_group: G04
file_locks:
  - forensic-analytics-cli/src/main/java/de/burger/forensics/analytics/cli/**
  - forensic-analytics-cli/src/test/java/de/burger/forensics/analytics/cli/**
contract_locks:
  - cli-gateway-contract-v1
  - gateway-openapi-v1
architecture_locks:
  - cli-gateway-adapter
  - monolith-caller-removal
quality_gates:
  targeted:
    - "./gradlew :forensic-analytics-cli:test --dependency-verification strict --console=plain --stacktrace"
    - "./gradlew :services:forensic-gateway-service:test --dependency-verification strict --console=plain --stacktrace"
  required:
    - "./gradlew test --dependency-verification strict --console=plain --stacktrace"
    - "git diff --check"
documentation:
  arc42: check runtime and building-block views
  adr: check ADR-0018
stop_conditions:
  - CLI migration requires undocumented backward compatibility
  - Gateway contract lacks required CLI behavior
  - CLI still depends on monolith application use cases after migration
  - redaction or idempotency behavior differs from the contract
```

Purpose: make the CLI the first caller-free legacy path by routing repository
analysis submission through the Gateway contract or stopping with explicit
missing-contract evidence.

Done criteria:

- CLI tests prove command behavior against the CLI/Gateway contract.
- The CLI no longer uses in-process monolith analysis use cases for the
  migrated repository-to-BTM path.
- Any remaining CLI legacy command is explicitly documented with caller status
  and non-removal reason.

### Slice 07 - Conditional Legacy Runtime Path Retirement

```yaml
slice_id: S07
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - senior-java-backend
  - microservice-senior-expert
  - senior-devops
  - senior-tester
affected_files:
  - settings.gradle.kts
  - forensic-analytics-engine/**
  - forensic-analytics-rest/**
  - forensic-analytics-bootstrap/**
  - forensic-analytics-boot-app/**
  - forensic-analytics-ingestion-request/**
  - forensic-analytics-testbed/**
  - docs/architecture/monolith-caller-retirement-plan.md
  - docs/architecture/service-migration-map.md
  - docs/workflow/execution-report.md
affected_modules:
  - forensic-analytics-engine
  - forensic-analytics-rest
  - forensic-analytics-bootstrap
  - forensic-analytics-boot-app
  - forensic-analytics-ingestion-request
  - forensic-analytics-testbed
affected_contracts:
  - contracts/openapi/gateway-api.yaml
  - contracts/grpc/forensic-ingestion.proto
dependencies:
  - S05
  - S06
parallel_group: G05
file_locks:
  - settings.gradle.kts
  - forensic-analytics-engine/**
  - forensic-analytics-rest/**
  - forensic-analytics-bootstrap/**
  - forensic-analytics-boot-app/**
  - forensic-analytics-ingestion-request/**
  - forensic-analytics-testbed/**
contract_locks:
  - gateway-openapi-v1
  - ingestion-grpc-v1
architecture_locks:
  - monolith-module-retirement
  - service-autonomy
  - rollback-readiness
quality_gates:
  targeted:
    - "./gradlew test --dependency-verification strict --console=plain --stacktrace"
  required:
    - "./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace"
    - "git diff --check"
documentation:
  arc42: update if runtime or deployment views change
  adr: create or update ADR if module removal changes accepted architecture
stop_conditions:
  - any candidate path still has production or test callers
  - replacement parity is not proven
  - rollback strategy is missing
  - module removal would weaken the default quality gate
  - removal would delete required rollback or test evidence
```

Purpose: retire only those legacy runtime paths that S05 proves are both
caller-free and covered by replacement parity. This slice may complete as
`NO_REMOVAL_SAFE` if caller evidence still blocks removal.

Done criteria:

- Either a narrowly scoped caller-free path is removed with full quality gate
  evidence, or the execution report records why removal is blocked.
- `settings.gradle.kts` changes are limited to verified caller-free modules.
- arc42 and architecture docs are synchronized when runtime composition
  changes.

### Slice 08 - Final Documentation And Quality Gate

```yaml
slice_id: S08
profile: FULL_PATH
owner: senior-documentation-engineer
secondary_reviewers:
  - senior-workflow-architect
  - senior-system-architect
  - senior-tester
affected_files:
  - docs/workflow/execution-report.md
  - docs/workflow/arc42-check-status.md
  - docs/architecture/current-build-and-test-map.md
  - docs/architecture/service-migration-map.md
  - docs/arc42/**
affected_modules: []
affected_contracts: []
dependencies:
  - S01
  - S02
  - S03
  - S04
  - S05
  - S06
  - S07
parallel_group: G06
file_locks:
  - docs/workflow/**
  - docs/architecture/**
  - docs/arc42/**
contract_locks: []
architecture_locks:
  - documentation-synchronization
  - quality-gate-reporting
quality_gates:
  targeted:
    - "git diff --check"
  required:
    - "./gradlew test --dependency-verification strict --console=plain --stacktrace"
    - "git diff --check"
documentation:
  arc42: checked or updated
  adr: checked
stop_conditions:
  - workflow result lacks exact commands and outcomes
  - arc42 drift remains unresolved
  - quality gate failure is not classified
  - deployment readiness is overstated
```

Purpose: synchronize workflow results, architecture documentation and quality
evidence before the workflow is considered complete.

Done criteria:

- Execution report lists changed files, exact commands, pass/fail/skip results
  and blockers.
- arc42 check status is complete.
- Remaining deployment work is clearly assigned to the separate workflow.

## Slice Dependency Graph

```text
S00
|-- S01
|   `-- S02
|-- S03
|   `-- S06
|-- S04
|-- S05
|   |-- S06
|   `-- S07
`-- S08 after S01-S07
```

## Parallelization Opportunities

- S01 and S03 may run in parallel after S00 because their write locks do not
  overlap.
- S04 may run in parallel with S01 or S03 because it is a deployment handoff
  documentation slice only.
- S02 depends on S01 because WildFly hardening should reuse the real E2E
  repository metrics vocabulary where practical.
- S06 depends on S03 and S05 because CLI migration needs both contract and
  caller inventory.
- S07 depends on S05 and S06 because removal must follow caller proof and first
  CLI migration evidence.

## Commit And Push Plan

Workflow creation does not commit or push unless the user explicitly requests
it. During `workflow execute`, commits and pushes are allowed only when the
active workflow executor and slice checkpoint rules permit them. Slice
checkpoint pushes target only the workflow branch and do not create or merge a
pull request.

## Stop Conditions

Stop the workflow when:

- a required file, route, field, Gradle task, module, service owner or contract
  cannot be verified exactly;
- a slice would execute external Git, Docker, Swarm or Kubernetes behavior
  without documented prerequisites;
- Gateway or CLI output could leak workspace paths, credentials, raw stdout,
  raw stderr or private repository metadata;
- a service would depend on another service's Java implementation classes;
- a monolith path still has callers but a slice attempts to remove it;
- quality commands fail and the failure cannot be classified;
- continuing would require guessing implementation or contract details.

## Definition Of Done

- `docs/workflow/workflow.md` and context pack are complete and checked.
- arc42 impact is checked and recorded.
- Each executed slice records changed files, exact verification commands and
  pass/fail/skip status.
- Real repository E2E coverage exists and is deterministic by default.
- WildFly hardening is opt-in, documented and measurable.
- CLI Gateway contract exists before CLI migration.
- Swarm/Kubernetes deployment remains a separate workflow with no readiness
  overclaim here.
- Legacy monolith paths are removed or isolated only after caller-free proof.

## Handoff To `workflow execute`

`workflow execute` may start from S00 on branch
`feature/workflow-e2e-wildfly-cli-deploy-20260521`.

Before implementation, the executor must read this complete workflow,
`docs/workflow/context-pack.json`, `QUALITY.md`, root `AGENTS.md`, routing
rules and the role files named by the slice.

## arc42 Check Status

See `docs/workflow/arc42-check-status.md`. The workflow creation review found
the existing arc42 scope, building-block, runtime, deployment, quality and risk
views already describe the relevant baseline. No direct arc42 edit is required
for workflow creation. Future execution slices must update arc42 if runtime,
contract, deployment or monolith-retirement behavior changes.
