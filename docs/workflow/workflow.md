# Workflow: FA-MSA-001-LMR Legacy Module Retirement

## Workflow Version

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-legacy-module-retirement-20260522-v1` |
| Requirement ID | `FA-MSA-001-LMR` |
| Parent requirement | `FA-MSA-001` |
| Workflow branch | `architecture/workflow-legacy-module-retirement-20260522` |
| Creation status | Created by `workflow create`; execution requires `workflow execute`. |
| Process strand | `workflow create` now; later `workflow execute` for slices. |
| Execution profile | `FULL_PATH` |

## Executive Summary

The user asked whether all remaining `forensic-analytics-*` modules can be
deleted because the platform is being converted into microservices. Read-only
verification shows that direct deletion is not safe yet: the legacy modules are
still registered in `settings.gradle.kts`, still referenced from Gradle build
files and still contain production or regression-test behavior.

This workflow is the executable retirement plan for those legacy modules. It is
not a big-bang deletion plan. Each module or module group is retired only after
service-local parity, explicit contract ownership, caller-free evidence,
rollback or deprecation notes and the required `QUALITY.md` gate exist.

## Target Picture

Mandatory FA-MSA-001 target services:

```text
services/
  repository-source-service/
  ingestion-service/
  java-parser-analysis-service/
  joern-analysis-service/
  analysis-orchestrator-service/
  query-report-api-service/
  cli-client/
  observability-stack/
  testbed/
```

Retired legacy modules after successful execution:

```text
forensic-analytics-adapter-javaparser
forensic-analytics-adapter-joern-docker
forensic-analytics-adapter-repository-source
forensic-analytics-application
forensic-analytics-boot-app
forensic-analytics-bootstrap
forensic-analytics-cli
forensic-analytics-domain
forensic-analytics-engine
forensic-analytics-ingestion-grpc
forensic-analytics-ingestion-request
forensic-analytics-logging
forensic-analytics-observability
forensic-analytics-persistence
forensic-analytics-rest
forensic-analytics-testbed
```

The target state must not contain shared Java implementation modules between
productive services. Central contracts may remain only as external interface
contracts such as `.proto`, OpenAPI, JSON Schema, event contracts or documented
file contracts.

## Verified Baseline

Read-only workflow creation verification found:

- Repository root: `/mnt/d/Projects/forensic_analytics`.
- Workflow branch exists and is active:
  `architecture/workflow-legacy-module-retirement-20260522`.
- Working tree was clean before workflow regeneration.
- Quality authority is `QUALITY.md`.
- Minimum quality command:
  `./gradlew test --dependency-verification strict --console=plain --stacktrace`.
- Full local quality gate:
  `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`.
- `settings.gradle.kts` still registers all listed legacy modules.
- `git ls-files "*build.gradle.kts" | xargs rg -n "project\\(\\\":forensic-analytics-"`
  still finds direct Gradle dependencies on legacy modules.
- `docs/architecture/current-coupling-map.md` records remaining production and
  test imports into legacy packages and explicitly states that no direct module
  retirement is safe yet.
- `docs/architecture/service-migration-map.md` defines retirement gates for
  central shared modules and target service ownership.
- ADR-0017 is accepted and defines FA-MSA-001 target service names.

## Requirement Clarification Decision

| Field | Decision |
|---|---|
| Original request | Create a workflow if the listed legacy modules cannot already be deleted after microservice conversion. |
| Interpreted intent | Create an executable, slice-based workflow that safely retires all listed `forensic-analytics-*` modules after verified service-local migration and caller-free proof. |
| Change type | Microservice migration completion, module retirement, Gradle build graph cleanup, contract/data ownership verification, documentation synchronization. |
| Affected process strand | `workflow create` now; later `workflow execute`. |
| Affected architecture area | Service autonomy, no shared Java implementation modules, Gradle project registration, runtime boot paths, persistence ownership, testbed parity. |
| Explicit requirements | Remove or retire the listed legacy modules only when safe; otherwise create an executable workflow. |
| Implicit requirements | Preserve current behavior until replacement services prove parity; keep rollback/deprecation evidence; keep forensic evidence semantics explicit. |
| Assumptions | FA-MSA-001 target services are the desired replacement landscape; optional services remain optional unless later requirements make them mandatory. |
| Non-goals | No production source deletion during workflow creation; no shared compatibility wrapper; no service readiness claim without build/start/test/container evidence. |
| Risks | Removing modules before caller-free proof would break CLI, REST, boot, bootstrap, ingestion, engine and testbed behavior. |
| Open questions | None blocking workflow creation. Execution slices may stop when a service owner, contract field, test parity or rollback path cannot be verified. |
| Blocking questions | None for workflow creation. |
| Confidence | 92 percent. |
| Decision | `READY_FOR_WORKFLOW`. |

## Scope

In scope:

- Revalidate callers and Gradle dependencies for all listed legacy modules.
- Prove replacement parity or explicit deprecation for each legacy runtime path.
- Migrate remaining runtime callers to service-local boundaries and external
  contracts.
- Remove direct Gradle dependencies on legacy modules.
- Deregister and delete only verified caller-free legacy modules.
- Keep docs, ADR/arc42 and retirement maps synchronized with actual state.

Out of scope:

- Implementing new product features unrelated to legacy module retirement.
- Replacing service names outside the accepted FA-MSA-001 landscape.
- Introducing shared Java domain, application, DTO, utility, persistence,
  logging, fixture or error-model modules.
- Claiming Kubernetes, Swarm, external Joern, LLM, Jenkins or Artifactory
  readiness unless repository manifests and commands exist.
- Push, PR creation, PR merge, branch cleanup or `push auto`.

## Architecture Constraints

- Current `forensic-analytics-*` modules are legacy modular-monolith modules,
  not microservices.
- Services must not depend on other services as Gradle projects.
- Services must not depend on central `forensic-analytics-*` Java modules after
  their migration slice is complete.
- Service communication must use REST/OpenAPI, gRPC/protobuf, messaging or
  documented file contracts.
- Domain and application packages inside services must remain framework-free.
- Runtime evidence, static facts, replay output, reports and LLM-generated
  text must remain separated by evidence category.
- Missing evidence must be represented as missing, unresolved or incomplete.
- No module may be removed while it is the only regression coverage for a
  behavior.

## Backend Assessment

Backend impact is high. The workflow touches Gradle build structure, Java
service ownership, legacy ports/adapters, REST, gRPC, CLI, in-memory
persistence, bootstraps, observability and testbed coverage. Every production
slice requires the responsible backend or service role to verify exact symbols,
ports, contracts and module dependencies before editing.

## Frontend Assessment

No frontend module is in direct scope. The Senior React Frontend role remains
an impact reviewer for `query-report-api-service` and `cli-client` public API
changes. A frontend-impact stop condition applies if any OpenAPI field,
endpoint or client-facing status shape is changed without verified consumers.

## Test Strategy

Default verification follows `QUALITY.md`:

1. Run the narrowest targeted tests for the slice.
2. Run affected service or module tests.
3. Run dependency and import scans proving the intended caller removal.
4. Run `./gradlew test --dependency-verification strict --console=plain --stacktrace`
   for production Java, tests, Gradle, contracts or runtime wiring changes.
5. Run the full local quality gate before any final legacy module deregistration
   or workflow closure:
   `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`.
6. Always run `git diff --check`.

## Resilience And Security Requirements

- Repository checkout and source snapshot handling must remain sandbox-aware and
  must not execute untrusted repository code without an approved decision.
- Joern execution remains bounded by explicit timeout, cleanup and diagnostics
  behavior.
- Service migrations must preserve correlation IDs, evidence provenance,
  deterministic identifiers and explicit missing-data representation.
- Diagnostics must not leak secrets, credentials, private workspace paths, raw
  source content, raw stderr or raw runtime values.
- Rollback or operator-visible deprecation instructions are required before a
  legacy path is removed.

## Ordered Slices

### Slice 00 - Execution Preflight And Evidence Freeze

```yaml
slice_id: S00
profile: FULL_PATH
owner: senior-execution-orchestrator
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
    - 'git status --short --branch'
    - 'git diff --check'
  required:
    - 'git status --short'
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - active branch is not architecture/workflow-legacy-module-retirement-20260522
  - working tree has unrelated or unclear changes
  - context pack hash drift is not reviewed
```

Purpose: freeze branch, context, current module registration and quality
commands before any execution slice modifies files.

### Slice 01 - Current Caller And Dependency Revalidation

```yaml
slice_id: S01
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - microservice-senior-expert
  - senior-java-backend
  - senior-tester
affected_files:
  - docs/architecture/current-coupling-map.md
  - docs/architecture/service-migration-map.md
  - docs/workflow/execution-report.md
affected_modules:
  - all-listed-legacy-modules
affected_contracts: []
dependencies:
  - S00
parallel_group: G01
file_locks:
  - docs/architecture/current-coupling-map.md
  - docs/architecture/service-migration-map.md
  - docs/workflow/execution-report.md
contract_locks: []
architecture_locks:
  - legacy-module-inventory
quality_gates:
  targeted:
    - 'git ls-files "*build.gradle.kts" | xargs rg -n "project\\(\\\":forensic-analytics-"'
    - 'rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|ingestion\\.request|ingestion\\.grpc)\\b" services forensic-analytics-* -S -g "*.java"'
    - 'git diff --check'
  required:
    - 'git diff --check'
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - caller inventory cannot be reproduced
  - a module is marked caller-free while scans still find callers
  - documentation contradicts verified build or source state
```

Purpose: establish the current truth before retirement work. This slice may
mark candidates as blocked; it must not remove code.

### Slice 02 - Contract And Runtime Parity Gate

```yaml
slice_id: S02
profile: FULL_PATH
owner: contract-first-api-steward
secondary_reviewers:
  - senior-grpc-proto-specialist
  - senior-system-architect
  - senior-tester
  - senior-devops
affected_files:
  - contracts/**
  - docs/architecture/service-communication-matrix.md
  - docs/architecture/target-microservices-architecture.md
  - docs/workflow/execution-report.md
affected_modules:
  - services:repository-source-service
  - services:ingestion-service
  - services:java-parser-analysis-service
  - services:joern-analysis-service
  - services:analysis-orchestrator-service
  - services:query-report-api-service
  - services:cli-client
affected_contracts:
  - contracts/grpc/**
  - contracts/openapi/**
  - contracts/events/**
dependencies:
  - S01
parallel_group: G02
file_locks:
  - contracts/**
  - docs/architecture/service-communication-matrix.md
  - docs/architecture/target-microservices-architecture.md
  - docs/workflow/execution-report.md
contract_locks:
  - all-service-contracts
architecture_locks:
  - contract-first-service-communication
quality_gates:
  targeted:
    - './gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:ingestion-service:test --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:java-parser-analysis-service:test --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:joern-analysis-service:test --dependency-verification strict --console=plain --stacktrace'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update communication and runtime views when contracts change
  adr: new ADR required for behavior-changing contract strategy changes
stop_conditions:
  - a service interaction has no verified external contract
  - generated Java DTOs are shared as implementation modules
  - runtime parity is claimed without tests
```

Purpose: prove the service communication surface that will replace in-process
legacy callers.

### Slice 03 - Repository Source Legacy Retirement

```yaml
slice_id: S03
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - senior-git-workspace-specialist
  - microservice-senior-expert
  - senior-security-sandbox-engineer
  - senior-tester
affected_files:
  - services/repository-source-service/**
  - forensic-analytics-adapter-repository-source/**
  - forensic-analytics-bootstrap/**
  - forensic-analytics-boot-app/**
  - settings.gradle.kts
  - docs/architecture/**
  - docs/workflow/execution-report.md
affected_modules:
  - services:repository-source-service
  - forensic-analytics-adapter-repository-source
affected_contracts:
  - contracts/grpc/**
dependencies:
  - S02
parallel_group: G03
file_locks:
  - services/repository-source-service/**
  - forensic-analytics-adapter-repository-source/**
  - docs/workflow/execution-report.md
contract_locks:
  - repository-source-contract
architecture_locks:
  - repository-source-ownership
quality_gates:
  targeted:
    - './gradlew :services:repository-source-service:test --dependency-verification strict --console=plain --stacktrace'
    - 'rg -n "forensic-analytics-adapter-repository-source|de\\.burger\\.forensics\\.analytics\\.adapter\\.repository\\.source" -g "!**/build/**" .'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update repository source ownership and deployment notes
  adr: checked
stop_conditions:
  - repository checkout behavior has no service-local replacement
  - private workspace access crosses a service boundary
  - legacy adapter callers remain after proposed removal
```

Purpose: move remaining repository-source behavior and callers to the target
service, then remove the legacy adapter only if caller-free proof is empty.

### Slice 04 - Ingestion Legacy Retirement

```yaml
slice_id: S04
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - senior-grpc-proto-specialist
  - ingestion-handoff-review
  - microservice-senior-expert
  - senior-tester
affected_files:
  - services/ingestion-service/**
  - forensic-analytics-ingestion-grpc/**
  - forensic-analytics-ingestion-request/**
  - forensic-analytics-cli/**
  - forensic-analytics-bootstrap/**
  - settings.gradle.kts
  - docs/architecture/**
  - docs/workflow/execution-report.md
affected_modules:
  - services:ingestion-service
  - forensic-analytics-ingestion-grpc
  - forensic-analytics-ingestion-request
affected_contracts:
  - contracts/grpc/**
dependencies:
  - S02
parallel_group: G03
file_locks:
  - services/ingestion-service/**
  - forensic-analytics-ingestion-grpc/**
  - forensic-analytics-ingestion-request/**
  - docs/workflow/execution-report.md
contract_locks:
  - ingestion-contract
architecture_locks:
  - ingestion-service-ownership
quality_gates:
  targeted:
    - './gradlew :services:ingestion-service:test --dependency-verification strict --console=plain --stacktrace'
    - 'rg -n "forensic-analytics-ingestion-grpc|forensic-analytics-ingestion-request|de\\.burger\\.forensics\\.analytics\\.ingestion\\.(grpc|request)" -g "!**/build/**" .'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update ingestion and handoff views
  adr: new ADR required if ingestion contract semantics change
stop_conditions:
  - runtime or analysis payload custody is unclear
  - missing fields are silently filled
  - legacy ingestion callers remain after proposed removal
```

Purpose: retire legacy ingestion transport/request modules only after the
target ingestion service owns intake and validation behavior.

### Slice 05 - JavaParser Legacy Retirement

```yaml
slice_id: S05
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - source-analysis-pipeline
  - microservice-senior-expert
  - senior-tester
affected_files:
  - services/java-parser-analysis-service/**
  - forensic-analytics-adapter-javaparser/**
  - settings.gradle.kts
  - docs/architecture/**
  - docs/workflow/execution-report.md
affected_modules:
  - services:java-parser-analysis-service
  - forensic-analytics-adapter-javaparser
affected_contracts:
  - contracts/grpc/**
dependencies:
  - S02
parallel_group: G03
file_locks:
  - services/java-parser-analysis-service/**
  - forensic-analytics-adapter-javaparser/**
  - docs/workflow/execution-report.md
contract_locks:
  - java-parser-analysis-contract
architecture_locks:
  - static-analysis-evidence
quality_gates:
  targeted:
    - './gradlew :services:java-parser-analysis-service:test --dependency-verification strict --console=plain --stacktrace'
    - 'rg -n "forensic-analytics-adapter-javaparser|de\\.burger\\.forensics\\.analytics\\.adapter\\.javaparser" -g "!**/build/**" .'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update static analysis ownership
  adr: checked
stop_conditions:
  - unresolved symbols are dropped or converted to false relationships
  - static facts are presented as runtime execution
  - legacy JavaParser callers remain after proposed removal
```

Purpose: retire the legacy JavaParser adapter after service-local AST/source
fact parity is verified.

### Slice 06 - Joern Legacy Retirement

```yaml
slice_id: S06
profile: FULL_PATH
owner: senior-joern-cpg-specialist
secondary_reviewers:
  - senior-java-backend
  - senior-devops
  - microservice-senior-expert
  - senior-tester
affected_files:
  - services/joern-analysis-service/**
  - forensic-analytics-adapter-joern-docker/**
  - docker/joern/**
  - settings.gradle.kts
  - docs/architecture/**
  - docs/workflow/execution-report.md
affected_modules:
  - services:joern-analysis-service
  - forensic-analytics-adapter-joern-docker
affected_contracts:
  - contracts/grpc/**
dependencies:
  - S02
parallel_group: G03
file_locks:
  - services/joern-analysis-service/**
  - forensic-analytics-adapter-joern-docker/**
  - docker/joern/**
  - docs/workflow/execution-report.md
contract_locks:
  - joern-analysis-contract
architecture_locks:
  - joern-cpg-boundary
quality_gates:
  targeted:
    - './gradlew :services:joern-analysis-service:test --dependency-verification strict --console=plain --stacktrace'
    - 'rg -n "forensic-analytics-adapter-joern-docker|de\\.burger\\.forensics\\.analytics\\.adapter\\.joern" -g "!**/build/**" .'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update Joern runtime and deployment notes
  adr: new ADR required if Joern isolation strategy changes
stop_conditions:
  - Joern CPG files are shared through private filesystem coupling
  - timeouts or unavailable-Joern diagnostics are lost
  - legacy Joern adapter callers remain after proposed removal
```

Purpose: retire legacy Joern Docker adapter behavior after service-local Joern
execution and artifact diagnostics are verified.

### Slice 07 - Orchestration Engine And Application Split

```yaml
slice_id: S07
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - distributed-systems-architect
  - data-ownership-persistence-steward
  - microservice-senior-expert
  - senior-tester
affected_files:
  - services/analysis-orchestrator-service/**
  - forensic-analytics-engine/**
  - forensic-analytics-application/**
  - forensic-analytics-domain/**
  - settings.gradle.kts
  - docs/architecture/**
  - docs/workflow/execution-report.md
affected_modules:
  - services:analysis-orchestrator-service
  - forensic-analytics-engine
  - forensic-analytics-application
  - forensic-analytics-domain
affected_contracts:
  - contracts/grpc/**
  - contracts/events/**
dependencies:
  - S03
  - S04
  - S05
  - S06
parallel_group: G04
file_locks:
  - services/analysis-orchestrator-service/**
  - forensic-analytics-engine/**
  - forensic-analytics-application/**
  - forensic-analytics-domain/**
  - docs/workflow/execution-report.md
contract_locks:
  - orchestration-contract
architecture_locks:
  - orchestration-ownership
  - domain-application-disassembly
quality_gates:
  targeted:
    - './gradlew :services:analysis-orchestrator-service:test --dependency-verification strict --console=plain --stacktrace'
    - 'rg -n "forensic-analytics-engine|de\\.burger\\.forensics\\.analytics\\.(engine|application|domain)" -g "!**/build/**" .'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update orchestration, domain and application ownership
  adr: new ADR required for service-state ownership changes
stop_conditions:
  - orchestrator absorbs repository checkout, parser, Joern or report ownership
  - shared domain/application modules remain required by target services
  - evidence categories are collapsed into ambiguous DTOs
```

Purpose: move orchestration ownership to the target service and begin verified
disassembly of shared domain/application code.

### Slice 08 - Query Report API, REST, Bootstrap And Boot Retirement

```yaml
slice_id: S08
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - contract-governance-expert
  - senior-devops
  - senior-react-frontend
  - microservice-senior-expert
  - senior-tester
affected_files:
  - services/query-report-api-service/**
  - forensic-analytics-rest/**
  - forensic-analytics-bootstrap/**
  - forensic-analytics-boot-app/**
  - contracts/openapi/**
  - settings.gradle.kts
  - docs/architecture/**
  - docs/workflow/execution-report.md
affected_modules:
  - services:query-report-api-service
  - forensic-analytics-rest
  - forensic-analytics-bootstrap
  - forensic-analytics-boot-app
affected_contracts:
  - contracts/openapi/**
dependencies:
  - S07
parallel_group: G05
file_locks:
  - services/query-report-api-service/**
  - forensic-analytics-rest/**
  - forensic-analytics-bootstrap/**
  - forensic-analytics-boot-app/**
  - contracts/openapi/**
  - docs/workflow/execution-report.md
contract_locks:
  - query-report-api-contract
architecture_locks:
  - public-api-facade
  - runtime-bootstrap-retirement
quality_gates:
  targeted:
    - './gradlew :services:query-report-api-service:test --dependency-verification strict --console=plain --stacktrace'
    - 'rg -n "forensic-analytics-rest|forensic-analytics-bootstrap|forensic-analytics-boot-app|de\\.burger\\.forensics\\.analytics\\.(rest|bootstrap|boot)" -g "!**/build/**" .'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update public API and runtime/deployment views
  adr: new ADR required if REST contract compatibility changes
stop_conditions:
  - frontend or CLI-visible API shape changes without contract tests
  - boot/bootstrap paths are removed without replacement start evidence
  - REST callers remain after proposed removal
```

Purpose: replace in-process REST and combined runtime boot paths with verified
service-local public API and startup evidence.

### Slice 09 - CLI Client Decoupling

```yaml
slice_id: S09
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - contract-governance-expert
  - senior-ux-designer
  - senior-tester
affected_files:
  - services/cli-client/**
  - forensic-analytics-cli/**
  - contracts/cli/**
  - contracts/openapi/**
  - settings.gradle.kts
  - docs/architecture/**
  - docs/workflow/execution-report.md
affected_modules:
  - services:cli-client
  - forensic-analytics-cli
affected_contracts:
  - contracts/cli/**
  - contracts/openapi/**
dependencies:
  - S08
parallel_group: G06
file_locks:
  - services/cli-client/**
  - forensic-analytics-cli/**
  - contracts/cli/**
  - docs/workflow/execution-report.md
contract_locks:
  - cli-public-api-contract
architecture_locks:
  - cli-client-only
quality_gates:
  targeted:
    - './gradlew :services:cli-client:test --dependency-verification strict --console=plain --stacktrace'
    - 'rg -n "forensic-analytics-cli|de\\.burger\\.forensics\\.analytics\\.cli" -g "!**/build/**" .'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update CLI client boundary
  adr: checked
stop_conditions:
  - CLI still instantiates domain, application, persistence, parser or Joern logic
  - command behavior is removed without parity or explicit deprecation
  - output redaction changes are untested
```

Purpose: make CLI behavior a public API client and retire local in-process
business logic.

### Slice 10 - Observability And Logging Decoupling

```yaml
slice_id: S10
profile: FULL_PATH
owner: senior-devops
secondary_reviewers:
  - observability-runtime-diagnostics
  - senior-java-backend
  - security-threat-modeling
  - senior-tester
affected_files:
  - services/observability-stack/**
  - forensic-analytics-logging/**
  - forensic-analytics-observability/**
  - deployment/**
  - settings.gradle.kts
  - docs/architecture/**
  - docs/workflow/execution-report.md
affected_modules:
  - services:observability-stack
  - forensic-analytics-logging
  - forensic-analytics-observability
affected_contracts: []
dependencies:
  - S02
parallel_group: G04
file_locks:
  - services/observability-stack/**
  - forensic-analytics-logging/**
  - forensic-analytics-observability/**
  - deployment/**
  - docs/workflow/execution-report.md
contract_locks: []
architecture_locks:
  - service-local-observability
  - no-shared-logging-module
quality_gates:
  targeted:
    - './gradlew :services:observability-stack:test --dependency-verification strict --console=plain --stacktrace'
    - 'rg -n "forensic-analytics-logging|forensic-analytics-observability|de\\.burger\\.forensics\\.analytics\\.(logging|observability)" -g "!**/build/**" .'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update crosscutting logging and observability
  adr: new ADR required if logging boundary strategy changes
stop_conditions:
  - shared Java logging or observability module remains required by target services
  - correlation or redaction semantics are lost
  - diagnostics are treated as forensic evidence
```

Purpose: replace shared observability/logging Java modules with service-local
diagnostics and deployment observability material.

### Slice 11 - Persistence Ownership Finalization

```yaml
slice_id: S11
profile: FULL_PATH
owner: senior-analysis-storage-architect
secondary_reviewers:
  - data-ownership-persistence-steward
  - senior-java-backend
  - microservice-senior-expert
  - senior-tester
affected_files:
  - services/**
  - forensic-analytics-persistence/**
  - docs/architecture/data-ownership.md
  - docs/architecture/service-migration-map.md
  - settings.gradle.kts
  - docs/workflow/execution-report.md
affected_modules:
  - forensic-analytics-persistence
  - services:analysis-orchestrator-service
  - services:query-report-api-service
  - services:ingestion-service
affected_contracts:
  - contracts/events/**
dependencies:
  - S07
  - S08
  - S10
parallel_group: G07
file_locks:
  - forensic-analytics-persistence/**
  - docs/architecture/data-ownership.md
  - docs/architecture/service-migration-map.md
  - docs/workflow/execution-report.md
contract_locks:
  - persistence-events
architecture_locks:
  - one-writer-data-ownership
  - persistence-retirement
quality_gates:
  targeted:
    - 'rg -n "forensic-analytics-persistence|de\\.burger\\.forensics\\.analytics\\.persistence" -g "!**/build/**" .'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update data ownership and persistence sections
  adr: new ADR required for durable store or ownership changes
stop_conditions:
  - canonical data owner is unclear
  - direct cross-service database access is introduced
  - in-memory persistence removal would delete the only tested behavior
```

Purpose: retire central persistence only after every stored category has a
service owner and verified replacement behavior or explicit deprecation.

### Slice 12 - Shared Domain And Application Module Removal

```yaml
slice_id: S12
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - senior-java-backend
  - microservice-senior-expert
  - quality-archunit-review
  - senior-tester
affected_files:
  - forensic-analytics-domain/**
  - forensic-analytics-application/**
  - services/**
  - settings.gradle.kts
  - build.gradle.kts
  - docs/architecture/**
  - docs/arc42/**
  - docs/workflow/execution-report.md
affected_modules:
  - forensic-analytics-domain
  - forensic-analytics-application
affected_contracts: []
dependencies:
  - S03
  - S04
  - S05
  - S06
  - S07
  - S08
  - S09
  - S10
  - S11
parallel_group: G08
file_locks:
  - forensic-analytics-domain/**
  - forensic-analytics-application/**
  - services/**
  - docs/workflow/execution-report.md
contract_locks: []
architecture_locks:
  - no-shared-domain-application
  - service-local-hexagonal-boundaries
quality_gates:
  targeted:
    - 'rg -n "forensic-analytics-domain|forensic-analytics-application|de\\.burger\\.forensics\\.analytics\\.(domain|application)" -g "!**/build/**" .'
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
    - 'git diff --check'
  required:
    - './gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: final service-local domain/application ownership update
  adr: checked; new ADR required if shared module exception is proposed
stop_conditions:
  - any productive service still depends on shared domain/application code
  - a shared DTO, utility, fixture or error model is introduced
  - architecture tests cannot prove forbidden dependencies
```

Purpose: remove the central shared domain/application modules only after all
service-local replacements and architecture tests exist.

### Slice 13 - Testbed Monolith Coupling Removal

```yaml
slice_id: S13
profile: FULL_PATH
owner: senior-tester
secondary_reviewers:
  - microservice-senior-expert
  - senior-devops
  - senior-java-backend
affected_files:
  - services/testbed/**
  - forensic-analytics-testbed/**
  - deployment/**
  - docs/testing/**
  - settings.gradle.kts
  - docs/architecture/**
  - docs/workflow/execution-report.md
affected_modules:
  - services:testbed
  - forensic-analytics-testbed
affected_contracts: []
dependencies:
  - S03
  - S04
  - S05
  - S06
  - S08
  - S09
  - S12
parallel_group: G09
file_locks:
  - services/testbed/**
  - forensic-analytics-testbed/**
  - docs/testing/**
  - docs/workflow/execution-report.md
contract_locks: []
architecture_locks:
  - testbed-non-production-only
  - regression-parity
quality_gates:
  targeted:
    - './gradlew :services:testbed:test --dependency-verification strict --console=plain --stacktrace'
    - 'rg -n "forensic-analytics-testbed|de\\.burger\\.forensics\\.analytics\\.testbed" -g "!**/build/**" .'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: update testbed and deployment views
  adr: checked
stop_conditions:
  - service E2E coverage is weaker than removed monolith regression coverage
  - production service depends on testbed code or fixtures
  - test data is confused with forensic evidence
```

Purpose: retire the legacy testbed only after service-root testbed coverage
proves at least equal regression value.

### Slice 14 - Gradle Deregistration And Source Tree Removal

```yaml
slice_id: S14
profile: FULL_PATH
owner: senior-devops
secondary_reviewers:
  - senior-system-architect
  - senior-java-backend
  - microservice-senior-expert
  - senior-tester
affected_files:
  - settings.gradle.kts
  - build.gradle.kts
  - gradle/**
  - forensic-analytics-adapter-javaparser/**
  - forensic-analytics-adapter-joern-docker/**
  - forensic-analytics-adapter-repository-source/**
  - forensic-analytics-application/**
  - forensic-analytics-boot-app/**
  - forensic-analytics-bootstrap/**
  - forensic-analytics-cli/**
  - forensic-analytics-domain/**
  - forensic-analytics-engine/**
  - forensic-analytics-ingestion-grpc/**
  - forensic-analytics-ingestion-request/**
  - forensic-analytics-logging/**
  - forensic-analytics-observability/**
  - forensic-analytics-persistence/**
  - forensic-analytics-rest/**
  - forensic-analytics-testbed/**
  - docs/architecture/**
  - docs/arc42/**
  - docs/workflow/execution-report.md
affected_modules:
  - all-listed-legacy-modules
affected_contracts: []
dependencies:
  - S12
  - S13
parallel_group: G10
file_locks:
  - settings.gradle.kts
  - build.gradle.kts
  - forensic-analytics-*/**
  - docs/workflow/execution-report.md
contract_locks: []
architecture_locks:
  - final-legacy-module-removal
quality_gates:
  targeted:
    - 'git ls-files "*build.gradle.kts" settings.gradle.kts | xargs rg -n "forensic-analytics-(adapter-javaparser|adapter-joern-docker|adapter-repository-source|application|boot-app|bootstrap|cli|domain|engine|ingestion-grpc|ingestion-request|logging|observability|persistence|rest|testbed)"'
    - 'rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|boot|ingestion\\.request|ingestion\\.grpc)\\b" services -S -g "*.java"'
    - 'git diff --check'
  required:
    - './gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: final module-removal state required
  adr: checked; new ADR required if any central module remains intentionally
stop_conditions:
  - any legacy module reference remains in build files or service production code
  - required quality gate fails
  - deletion includes unrelated modules or generated evidence
  - line-ending-only churn pollutes the diff
```

Purpose: remove the registered legacy modules and source trees only after all
previous slices prove they are no longer needed.

### Slice 15 - Closure, Rollback Notes And Release Readiness

```yaml
slice_id: S15
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - senior-devops
  - senior-tester
  - microservice-runtime-readiness-expert
  - senior-documentation-engineer
affected_files:
  - docs/architecture/**
  - docs/arc42/**
  - docs/README.md
  - services/README.md
  - docs/workflow/execution-report.md
affected_modules:
  - services:repository-source-service
  - services:ingestion-service
  - services:java-parser-analysis-service
  - services:joern-analysis-service
  - services:analysis-orchestrator-service
  - services:query-report-api-service
  - services:cli-client
  - services:observability-stack
  - services:testbed
affected_contracts:
  - contracts/**
dependencies:
  - S14
parallel_group: G11
file_locks:
  - docs/architecture/**
  - docs/arc42/**
  - docs/README.md
  - services/README.md
  - docs/workflow/execution-report.md
contract_locks:
  - final-contract-readiness
architecture_locks:
  - final-microservice-readiness
quality_gates:
  targeted:
    - './gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace'
    - 'git diff --check'
  required:
    - './gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: final check required
  adr: final check required
stop_conditions:
  - any mandatory service cannot be built, started or health-checked as documented
  - rollback or deprecation notes are missing
  - docs claim readiness not proven by repository files and commands
```

Purpose: close the workflow only after service readiness, documentation,
rollback/deprecation notes and the full local quality gate are complete.

## Dependency Graph

```text
S00
  -> S01
    -> S02
      -> S03
      -> S04
      -> S05
      -> S06
        -> S07
          -> S08
            -> S09
          -> S10
            -> S11
              -> S12
                -> S13
                  -> S14
                    -> S15
```

## Parallelization Opportunities

S03 through S06 may be executed in parallel only when the workflow executor
confirms disjoint file locks and stable contracts. The default execution mode
is one slice at a time. S10 may run after S02 in parallel with orchestration
work only when observability file locks do not overlap with service slices.

## Role Or Subagent Ownership Map

| Area | Owner |
|---|---|
| Workflow planning and S3/S3D execution ordering | Senior Execution Orchestrator |
| Requirement and EPIC traceability | Senior Requirement Engineer |
| Architecture and arc42/ADR governance | Senior System Architect |
| Java service migration | Senior Java Backend Developer |
| Service autonomy and no shared Java modules | Microservice Senior Expert |
| Contracts | Contract-First API Steward and Senior gRPC/Proto Specialist |
| Persistence and data ownership | Senior Analysis Storage Architect and Data Ownership Steward |
| Gradle, Docker and runtime readiness | Senior DevOps Engineer |
| Regression, JUnit 6, ArchUnit and quality gate | Senior Tester |
| Repository sandboxing and leakage risks | Senior Security/Sandbox Engineer |
| Documentation synchronization | Senior Documentation Engineer |
| Frontend impact check | Senior React Frontend Developer |

Callable subagents are used during `workflow execute` only when explicitly
authorized by the runtime and current user request. Otherwise the matching role
files are used as explicit local review checklists.

## Quality-Gate Expectations

For documentation-only workflow creation, run `git diff --check`.

For execution slices that touch product Java, tests, Gradle, contracts,
runtime wiring or deployment material, run the slice-targeted command and the
repository minimum gate:

```bash
./gradlew test --dependency-verification strict --console=plain --stacktrace
```

Before final legacy module deregistration and closure, run:

```bash
./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace
```

## Documentation Synchronization Points

- Update `docs/architecture/current-coupling-map.md` whenever caller evidence
  changes.
- Update `docs/architecture/service-migration-map.md` whenever a legacy module
  candidate moves from blocked to caller-free.
- Update `docs/architecture/data-ownership.md` for persistence ownership.
- Update `docs/arc42/**` for service boundaries, runtime view, deployment view,
  crosscutting concepts and risks.
- Update ADRs only for changed decisions, not for execution-status notes.

## Stop Conditions

Stop workflow execution when:

- exact symbol, task, package, contract, schema, service or module names cannot
  be verified;
- a legacy module still has production or test callers;
- service-local replacement parity is not tested;
- a shared Java module or compatibility bridge is proposed without explicit
  ADR and tests;
- a quality command fails;
- deleting a path would remove the only regression coverage for a behavior;
- continuing would require guessing evidence semantics, data ownership,
  runtime behavior, API fields or rollback steps.

## Uncertainty Escalation

Automatic correction loops are capped at `maxRetries = 3`. After the third
failed or unresolved attempt, stop and escalate to the Root Architect with:

- attempted loop;
- unresolved blocker;
- affected files, modules or contracts;
- reason automatic continuation is unsafe.

## Commit And Push Plan

Workflow creation does not push. During `workflow execute`, each successful
slice may checkpoint commit and push only when the active workflow executor and
repository rules allow it. Slice checkpoint push is not `push auto`, does not
create or merge a PR and must not push to `main`.

## Definition Of Done

The workflow is complete only when:

- all listed legacy modules are removed from `settings.gradle.kts`;
- source trees for all listed legacy modules are removed or explicitly retained
  by an ADR with a non-production purpose;
- target services build independently and have documented start/healthcheck
  paths;
- no productive service depends on central shared Java implementation modules;
- service interactions use approved external contracts;
- regression coverage exists in service-local or networked tests;
- docs/ADR/arc42 reflect the verified state;
- the full local quality gate passes.

## Handoff To `workflow execute`

Run `workflow execute` only from branch
`architecture/workflow-legacy-module-retirement-20260522`. The first executable
slice is S00. S14 must not execute until S03 through S13 have passed and their
caller-free evidence is recorded.

## arc42 Check Status

Current status: checked for workflow creation. Execution slices must update
arc42 documents when actual service ownership, runtime, deployment or
crosscutting behavior changes. This workflow does not claim module removal
during creation.
