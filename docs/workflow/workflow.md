# Workflow: FA-MSA-001-LMR Final Legacy Source Tree Retirement

## Workflow Version

| Field | Value |
|---|---|
| Workflow version | `fa-msa-001-final-legacy-source-retirement-20260523-v2` |
| Requirement ID | `FA-MSA-001-LMR-FINAL` |
| Parent requirement | `FA-MSA-001` |
| Workflow branch | `architecture/workflow-legacy-module-retirement-20260522` |
| Creation status | Created by `workflow create`; topology corrected during `workflow execute` after S04 preflight found stale executable legacy commands. |
| Process strand | `workflow execute` |
| Execution profile | `FULL_PATH` |

## Executive Summary

The user requested a subagent-reviewed workflow to begin final removal of the
remaining legacy monolith source trees:

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

Read-only verification and five role/subagent reviews on 2026-05-23 found that
the repository had already moved the active Gradle build to `services:*`
projects only. The legacy `forensic-analytics-*` directories were still tracked
source trees at workflow creation time, but they were no longer registered
Gradle modules and no active service build file or service Java source import
depended on them. During `workflow execute`, S05 removed all listed legacy
source trees in checkpoint `d8d9dab`.

The previous workflow was stale because it still treated the legacy source
trees as active Gradle projects and still referenced non-executable
`:forensic-analytics-*:test` gates. This regenerated workflow supersedes that
plan. Version 2 inserts a pre-deletion legacy command documentation cleanup
slice because S04 preflight found stale executable `:forensic-analytics-*`
commands in service and deployment documentation. It routes execution through
reference classification, contract and runtime documentation decisions,
service-regression coverage confirmation, command-documentation stopper cleanup,
physical source-tree deletion, architecture documentation closure and the
required `QUALITY.md` gates.

No production code or legacy source tree was removed during workflow creation.
Source-tree deletion occurred later in S05 after S02, S03 and S04 gates passed.

## Target Picture

The active build remains a service-only Gradle model under `services:*`.

After successful execution:

- no tracked source file remains under `forensic-analytics-*`;
- no active Gradle file references `project(":forensic-analytics-*")`;
- no active service Java source imports legacy monolith packages;
- stale Docker, README, testing, contract-test and architecture documentation
  references are either removed, rewritten as historical notes, or backed by an
  explicit ADR/contract decision;
- retained compatibility vocabulary in public contracts is documented as
  compatibility vocabulary, not as proof of an active legacy implementation;
- arc42 and architecture maps describe the verified service-only build state.

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
- `./gradlew projects --dependency-verification strict --console=plain --stacktrace`
  passed and listed only `services:*` subprojects.
- `settings.gradle.kts` includes only `services:*` projects.
- At workflow creation, `git ls-files "forensic-analytics-*" | wc -l`
  returned `450`; after S05 checkpoint `d8d9dab`,
  `git ls-files "forensic-analytics-*"` returns no tracked source-tree files.
- A scan of `settings.gradle.kts`, root build files and `services/**/*.gradle.kts`
  found no `project(":forensic-analytics-*")` references.
- A scan of non-legacy Java files found no imports from legacy monolith
  packages such as `de.burger.forensics.analytics.application`,
  `de.burger.forensics.analytics.domain`, `de.burger.forensics.analytics.adapter`,
  `de.burger.forensics.analytics.persistence`, `de.burger.forensics.analytics.rest`,
  `de.burger.forensics.analytics.cli`, `de.burger.forensics.analytics.engine`,
  `de.burger.forensics.analytics.logging`,
  `de.burger.forensics.analytics.observability`,
  `de.burger.forensics.analytics.bootstrap`, `de.burger.forensics.analytics.boot`,
  `de.burger.forensics.analytics.ingestion.request`, or
  `de.burger.forensics.analytics.ingestion.grpc`.
- `docker/boot-app`, `.dockerignore`, contract docs, README/testing docs,
  architecture maps and arc42 still contain legacy module references that must
  be classified before deletion is claimed complete.
- A React/Vite frontend exists under `forensic-ui`, but no direct frontend
  references to the legacy module names were found.

## Subagent Review Evidence

Five read-only subagent reviews were used because the user explicitly requested
subagent workflow creation:

| Role | Finding |
|---|---|
| Senior Requirement Engineer | READY_FOR_WORKFLOW. The requirement is final retirement of tracked legacy source trees after proving they are not active build, runtime, test, rollback or documentation evidence. |
| Senior System Architect | READY_WITH_DOC_GATES. The main blocker is stale workflow/architecture documentation. The Gradle deregistration step is already complete; remaining work is source-tree deletion plus docs/contracts/runtime-doc cleanup. |
| Senior Java Backend Developer | READY_WITH_TARGETED_GATES. The active build lists only services and service scans found no direct legacy Java imports. Delete in a source-tree removal slice after docs and coverage checks. |
| Senior React Frontend Developer | NO_DIRECT_FRONTEND_IMPACT. `forensic-ui` consumes public Gateway API endpoints only. React review is required only if OpenAPI fields, endpoints or client-visible status shapes change. |
| Senior Tester | READY_WITH_QUALITY_GATES. Replace stale `:forensic-analytics-*` test gates with service/root gates. Run `git diff --check`, targeted service tests, minimum gate and full local gate before closure. |

## Requirement Clarification Decision

| Field | Decision |
|---|---|
| Original request | Begin final removal of the listed monolith modules using `workflow create with subagent`. |
| Interpreted intent | Create an executable, subagent-reviewed workflow for deleting the remaining tracked legacy `forensic-analytics-*` source trees and closing stale documentation/runtime references. |
| Change type | Microservice migration completion, source-tree deletion, documentation reconciliation, quality-gate closure. |
| Affected process strand | `workflow execute` for v2 topology correction and remaining slices. |
| Affected architecture area | Service autonomy, no shared Java implementation modules, build topology, public contract wording, runtime/deployment documentation, regression coverage. |
| Explicit requirements | Final removal of the listed legacy directories. |
| Implicit requirements | Do not re-register legacy modules; preserve evidence semantics; avoid deleting the only regression coverage; keep docs/arc42 truthful. |
| Assumptions | The current `services:*` Gradle model is the intended FA-MSA-001 target build. Compatibility labels may remain only when classified as contract vocabulary. |
| Non-goals | No new product features, no service behavior rewrite, no contract shape change without contract governance, no frontend change unless public API shape changes. |
| Risks | Stale Docker and documentation references can become broken after deletion; module-local tests are removed with the source trees; docs may overclaim runtime readiness. |
| Open questions | None blocking workflow creation. Execution may stop on unclear contract vocabulary, missing regression owner, or unresolved persistence ownership. |
| Blocking questions | None for workflow creation. |
| Confidence | 93 percent. |
| Decision | `READY_FOR_WORKFLOW`. |

## Scope

In scope:

- Revalidate the current services-only Gradle model.
- Classify remaining legacy references outside the legacy source trees.
- Remove or rewrite stale Docker, README, testing, contract-test, service
  README, workflow, architecture and arc42 references.
- Clear stale executable `:forensic-analytics-*` commands from active service
  and deployment documentation before deleting source trees.
- Delete the 16 tracked `forensic-analytics-*` source trees.
- Prove `git ls-files "forensic-analytics-*"` is empty after deletion.
- Run targeted service tests, the minimum quality command and the full local
  quality gate before closure.

Out of scope:

- Re-registering any legacy module.
- Moving legacy Java code into shared service modules.
- Creating shared Java domain, DTO, utility, persistence, fixture, logging or
  error-model modules.
- Changing REST/OpenAPI, gRPC/protobuf, CLI or file contracts without a
  contract-governance review.
- Changing frontend behavior unless public API shape changes.
- Claiming Docker, Swarm, Kubernetes or healthcheck readiness without verified
  repository commands and manifests.
- Push, PR creation, PR merge, branch cleanup or `push auto`.

## Architecture Constraints

- ADR-0017 remains authoritative for the FA-MSA-001 target service landscape.
- ADR-0009 forbids shared Java implementation modules between services.
- ADR-0010 and ADR-0018 require contract-first service communication and do not
  make planned contracts proof of runtime behavior.
- ADR-0013 requires one data owner and one write path per persisted data type.
- ADR-0019 keeps Spring service-local and forbids framework leakage into
  service domain/application packages.
- Static source facts, runtime trace facts, replay output, reports, generated
  LLM text and human findings remain distinct evidence categories.
- Missing evidence remains explicit as missing, incomplete, unresolved or not
  available.
- Stale documentation is not evidence that a legacy source tree is still active.
- A deleted legacy source tree must not be replaced with a compatibility alias,
  wrapper or shared code module unless a later task explicitly requests and
  tests that compatibility behavior.

## Backend Assessment

Backend compile risk is low because the active Gradle model no longer includes
the legacy source trees and service scans found no direct legacy imports.
Deletion risk remains documentation and regression risk: deleting the trees also
deletes their historical tests, examples and local runtime boot code. Execution
therefore must confirm service-local tests and public contract tests cover or
explicitly deprecate the retired behavior before source-tree deletion is
accepted.

## Frontend Assessment

The React frontend lives in `forensic-ui`. It has no exact legacy module-name
references in implementation, README, Dockerfile, nginx configuration or built
assets. Frontend work is not in direct scope. Senior React Frontend review is
required only when a slice changes OpenAPI fields, endpoints, response status
shapes, or `forensic-ui` API mappers.

## Test Strategy

Default verification follows `QUALITY.md`:

1. Run branch/status and project-model checks first.
2. Run targeted service tests named in the slice.
3. Run reference and leakage scans before and after source-tree deletion.
4. Run `git diff --check` for every slice.
5. Run the minimum quality command for code, test, Gradle, contract, runtime or
   deletion changes:
   `./gradlew test --dependency-verification strict --console=plain --stacktrace`.
6. Run the full local quality gate before accepting final deletion closure:
   `./gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace`.
7. If frontend-impacting API shape changes occur, run `cd forensic-ui && npm ci`
   followed by `npm run test` and `npm run build`.

## Ordered Slices

### Slice 00 - Execution Preflight And Current Baseline Freeze

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
    - './gradlew projects --dependency-verification strict --console=plain --stacktrace'
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
  - Gradle project model lists any forensic-analytics-* project
```

Purpose: freeze branch, workflow context, Gradle project model and quality
commands before deletion work starts.

### Slice 01 - Legacy Reference Classification

```yaml
slice_id: S01
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - senior-requirement-engineer
  - contract-first-api-steward
  - senior-devops
  - senior-tester
affected_files:
  - docs/architecture/**
  - docs/contracts/**
  - docs/testing/**
  - docs/README.md
  - docker/boot-app/**
  - .dockerignore
  - contracts/**
affected_modules: []
affected_contracts:
  - contracts/openapi/gateway-api.yaml
  - contracts/cli/gateway-cli-contract.md
  - contracts/grpc/forensic-ingestion.proto
dependencies:
  - S00
parallel_group: G01
file_locks:
  - docs/architecture/**
  - docs/contracts/**
  - docs/testing/**
  - docs/README.md
  - docker/boot-app/**
  - .dockerignore
  - contracts/**
contract_locks:
  - public-api-compatibility-vocabulary
  - grpc-source-path-comments
architecture_locks:
  - legacy-reference-classification
quality_gates:
  targeted:
    - 'rg -n "forensic-analytics-" docker .dockerignore contracts docs --glob "!docs/workflow/**"'
    - 'git diff --check'
  required:
    - 'git diff --check'
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - a reference cannot be classified as removable, historical, compatibility vocabulary, or active blocker
  - contract wording would change behavior without contract governance
  - deletion would remove the only documented rollback or regression evidence
```

Purpose: classify all remaining legacy references outside `docs/workflow` and
outside the source trees before any deletion is attempted.

### Slice 02 - Runtime, Docker And Contract Documentation Cleanup

```yaml
slice_id: S02
profile: FULL_PATH
owner: senior-devops
secondary_reviewers:
  - senior-system-architect
  - contract-governance-expert
  - senior-react-frontend
  - senior-tester
affected_files:
  - docker/boot-app/**
  - .dockerignore
  - docs/README.md
  - docs/testing/**
  - docs/contracts/**
  - contracts/**
  - services/testbed/README.md
  - contracts/**
affected_modules:
  - services:cli-client
  - services:query-report-api-service
  - services:forensic-gateway-service
affected_contracts:
  - contracts/openapi/gateway-api.yaml
  - contracts/cli/gateway-cli-contract.md
  - contracts/grpc/forensic-ingestion.proto
dependencies:
  - S01
parallel_group: G02
file_locks:
  - docker/boot-app/**
  - .dockerignore
  - docs/README.md
  - docs/testing/**
  - docs/contracts/**
  - contracts/**
  - services/testbed/README.md
  - contracts/**
contract_locks:
  - public-rest-contract
  - cli-contract
  - ingestion-grpc-contract
architecture_locks:
  - runtime-deployment-docs
quality_gates:
  targeted:
    - './gradlew :services:query-report-api-service:test :services:forensic-gateway-service:test :services:cli-client:test --dependency-verification strict --console=plain --stacktrace'
    - 'git diff --check'
  required:
    - 'git diff --check'
documentation:
  arc42: pending S06
  adr: create or supersede final-retirement ADR if contract/runtime ownership changes
stop_conditions:
  - public API field, endpoint, error shape or status shape changes without contract and frontend review
  - Docker documentation still points to a deleted or non-buildable legacy boot jar
  - docs keep a non-existent :forensic-analytics-* Gradle command as executable
```

Purpose: remove or reword stale executable runtime, Docker and public contract
documentation before the source trees disappear.

### Slice 03 - Service Regression Coverage Confirmation

```yaml
slice_id: S03
profile: FULL_PATH
owner: senior-tester
secondary_reviewers:
  - senior-java-backend
  - microservice-senior-expert
  - senior-devops
  - senior-system-architect
affected_files:
  - services/**
  - docs/testing/**
  - docs/architecture/**
affected_modules:
  - services:repository-source-service
  - services:repository-analysis-service
  - services:ingestion-service
  - services:forensic-ingestion-service
  - services:java-parser-analysis-service
  - services:java-ast-analysis-service
  - services:joern-analysis-service
  - services:joern-cpg-analysis-service
  - services:analysis-orchestrator-service
  - services:analysis-store-service
  - services:btm-generation-service
  - services:query-report-api-service
  - services:forensic-gateway-service
  - services:cli-client
  - services:observability-stack
  - services:testbed
affected_contracts: []
dependencies:
  - S01
parallel_group: G03
file_locks:
  - services/**
  - docs/testing/**
  - docs/architecture/**
contract_locks: []
architecture_locks:
  - regression-coverage-ownership
quality_gates:
  targeted:
    - './gradlew :services:repository-source-service:test :services:repository-analysis-service:test --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:ingestion-service:test :services:forensic-ingestion-service:test --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:java-parser-analysis-service:test :services:java-ast-analysis-service:test --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:joern-analysis-service:test :services:joern-cpg-analysis-service:test --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:analysis-orchestrator-service:test :services:analysis-store-service:test :services:btm-generation-service:test --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:query-report-api-service:test :services:forensic-gateway-service:test :services:cli-client:test --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:observability-stack:test :services:testbed:test --dependency-verification strict --console=plain --stacktrace'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: pending S06
  adr: checked
stop_conditions:
  - a legacy module-local test is the only known coverage for behavior still claimed as supported
  - a service owner for retained behavior cannot be verified
  - targeted service tests fail for reasons related to current changes
```

Purpose: prove service-local tests and explicit deprecation notes are enough to
replace the legacy module-local tests that will be deleted with the source
trees.

### Slice 04 - Legacy Command Documentation Stopper Cleanup

```yaml
slice_id: S04
profile: FULL_PATH
owner: senior-documentation-engineer
secondary_reviewers:
  - senior-devops
  - senior-system-architect
  - senior-tester
  - microservice-runtime-readiness-expert
affected_files:
  - services/analysis-orchestrator-service/README.md
  - services/joern-analysis-service/README.md
  - services/README.md
  - docs/architecture/current-build-and-test-map.md
  - docs/architecture/current-coupling-map.md
  - docs/architecture/legacy-reference-classification.md
  - docs/architecture/monolith-caller-retirement-plan.md
  - docs/architecture/monolith-runtime-isolation.md
  - docs/architecture/service-boundaries.md
  - docs/architecture/service-migration-map.md
  - docs/arc42/05-building-block-view.md
  - docs/arc42/07-deployment-view.md
  - docs/arc42/08-crosscutting-concepts.md
  - docs/skill-audit/README.md
  - docs/workflow/workflow.md
  - docs/workflow/quality-and-leakage-gates.md
  - docs/workflow/execution-report.md
  - docs/workflow/context-pack.md
  - docs/workflow/context-pack.json
affected_modules: []
affected_contracts: []
dependencies:
  - S02
  - S03
parallel_group: G04
file_locks:
  - services/analysis-orchestrator-service/README.md
  - services/joern-analysis-service/README.md
  - services/README.md
  - docs/architecture/current-build-and-test-map.md
  - docs/architecture/current-coupling-map.md
  - docs/architecture/legacy-reference-classification.md
  - docs/architecture/monolith-caller-retirement-plan.md
  - docs/architecture/monolith-runtime-isolation.md
  - docs/architecture/service-boundaries.md
  - docs/architecture/service-migration-map.md
  - docs/arc42/05-building-block-view.md
  - docs/arc42/07-deployment-view.md
  - docs/arc42/08-crosscutting-concepts.md
  - docs/skill-audit/README.md
  - docs/workflow/workflow.md
  - docs/workflow/quality-and-leakage-gates.md
  - docs/workflow/execution-report.md
  - docs/workflow/context-pack.md
  - docs/workflow/context-pack.json
contract_locks: []
architecture_locks:
  - pre-delete-legacy-command-docs
  - arc42-deployment-command-truthfulness
quality_gates:
  targeted:
    - 'rg -n "^\s*\./gradlew\s+:forensic-analytics-|:forensic-analytics-(boot-app|adapter-joern-docker|engine|application|domain)|bootstrap module can start|existing bootstrap module remains available|current-state evidence|current quality-gate evidence|current multi-project build includes|current implementation baseline|current workflow state|current repository state|verified current behavior|active as legacy quality-gate|active as rollback|remain active|remains active|retained active|active rollback|remain registered|active legacy callers|S15 through S18|S13 through S18|S19|S20|72 direct|653 production|628 test|13 test dependencies" services/analysis-orchestrator-service/README.md services/joern-analysis-service/README.md services/README.md docs/architecture/current-build-and-test-map.md docs/architecture/current-coupling-map.md docs/architecture/legacy-reference-classification.md docs/architecture/monolith-caller-retirement-plan.md docs/architecture/monolith-runtime-isolation.md docs/architecture/service-boundaries.md docs/architecture/service-migration-map.md docs/arc42/05-building-block-view.md docs/arc42/07-deployment-view.md docs/arc42/08-crosscutting-concepts.md docs/skill-audit/README.md'
    - './gradlew projects --dependency-verification strict --console=plain --stacktrace'
    - './gradlew :services:analysis-orchestrator-service:test :services:analysis-orchestrator-service:bootJar :services:analysis-orchestrator-service:bootRun :services:joern-analysis-service:test :services:joern-analysis-service:bootJar :services:joern-analysis-service:bootRun --dry-run --dependency-verification strict --console=plain --stacktrace'
    - 'python3 -m json.tool docs/workflow/context-pack.json'
    - 'git diff --check'
  required:
    - 'git diff --check'
documentation:
  arc42: limited pre-delete deployment-command correction; final closure pending S06
  adr: checked
stop_conditions:
  - an active service, deployment, architecture or audit document still contains runnable :forensic-analytics-* commands
  - docs claim legacy modules are active current-state, current implementation baseline or quality-gate evidence before deletion
  - a replacement service command cannot be verified from the current Gradle project model
  - cleanup would change public contract shape, runtime behavior or source code
```

Purpose: clear the S04 preflight stopper without deleting source trees. This
slice removes or reclassifies stale executable legacy Gradle commands and
current-state claims in active service, deployment, architecture and audit
documentation so physical deletion can proceed on verified documentation
ground.

### Slice 05 - Physical Legacy Source Tree Removal

```yaml
slice_id: S05
profile: FULL_PATH
owner: senior-java-backend
secondary_reviewers:
  - senior-devops
  - senior-system-architect
  - microservice-senior-expert
  - senior-tester
affected_files:
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
affected_modules: []
affected_contracts: []
dependencies:
  - S04
parallel_group: G05
file_locks:
  - forensic-analytics-*/**
contract_locks: []
architecture_locks:
  - final-source-tree-retirement
quality_gates:
  targeted:
    - 'git ls-files "forensic-analytics-*"'
    - './gradlew projects --dependency-verification strict --console=plain --stacktrace'
    - 'git ls-files "*build.gradle.kts" | grep -v "^forensic-analytics-" | xargs -r rg -n "project\\(\\\":forensic-analytics-"'
    - 'git ls-files "*.java" | grep -v "^forensic-analytics-" | xargs -r rg -n -P "^import\\s+de\\.burger\\.forensics\\.analytics\\.(application|domain|adapter|persistence|rest|cli|engine|logging|observability|bootstrap|boot|ingestion\\.request|ingestion\\.grpc)\\b"'
    - 'git diff --check'
  required:
    - './gradlew test --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: pending S06
  adr: final-retirement ADR required when source deletion is accepted
stop_conditions:
  - any legacy source tree is still an active Gradle project
  - any active service build or source file depends on a legacy tree
  - any source tree is removed before S02, S03 and S04 pass
  - git ls-files after deletion still lists a removed candidate
```

Purpose: delete only the verified legacy source trees. This slice must not
modify service behavior or introduce shared Java replacement modules.

### Slice 06 - Architecture Documentation And ADR Closure

```yaml
slice_id: S06
profile: FULL_PATH
owner: senior-system-architect
secondary_reviewers:
  - adr-steward
  - senior-documentation-engineer
  - senior-requirement-engineer
  - senior-tester
affected_files:
  - docs/adr/**
  - docs/arc42/**
  - docs/architecture/**
  - docs/workflow/**
  - docs/skill-audit/**
  - docs/README.md
  - docs/testing/**
  - docs/contracts/**
  - contracts/**
  - services/testbed/README.md
affected_modules: []
affected_contracts:
  - contract-provenance-wording-only
dependencies:
  - S05
parallel_group: G06
file_locks:
  - docs/adr/**
  - docs/arc42/**
  - docs/architecture/**
  - docs/workflow/**
  - docs/skill-audit/**
  - docs/README.md
  - docs/testing/**
  - docs/contracts/**
  - contracts/**
  - services/testbed/README.md
contract_locks:
  - contracts/provenance-wording-only
architecture_locks:
  - arc42-legacy-retirement-closure
  - adr-final-retirement
  - skill-audit-baseline-wording
  - contract-provenance-wording
  - testbed-predecessor-wording
quality_gates:
  targeted:
    - 'rg -n ":forensic-analytics-" docs docker contracts .dockerignore --glob "!docs/workflow/**"'
    - 'rg -n "forensic-analytics-" docs docker contracts .dockerignore --glob "!docs/workflow/**"'
    - 'rg -n "forensic-analytics-" services/testbed/README.md'
    - 'python3 -m json.tool docs/workflow/context-pack.json'
    - 'git diff --check'
  required:
    - 'git diff --check'
documentation:
  arc42: update sections 05, 06, 07, 08, 09, 10 and 11
  adr: add or supersede final modular-monolith retirement decision
stop_conditions:
  - docs claim legacy modules are active after deletion
  - docs claim service runtime, Docker, healthcheck, Swarm or Kubernetes readiness without verified evidence
  - ADR history is rewritten instead of superseded
  - compatibility vocabulary is removed without contract approval
```

Purpose: align architecture, ADR, README, testing and workflow documents with
the verified post-deletion state.

### Slice 07 - Quality Gate And Release Readiness

```yaml
slice_id: S07
profile: FULL_PATH
owner: senior-devops
secondary_reviewers:
  - senior-tester
  - senior-system-architect
  - microservice-runtime-readiness-expert
  - senior-documentation-engineer
affected_files:
  - docs/workflow/execution-report.md
affected_modules:
  - services
affected_contracts: []
dependencies:
  - S06
parallel_group: G07
file_locks:
  - docs/workflow/execution-report.md
contract_locks: []
architecture_locks:
  - final-quality-closure
quality_gates:
  targeted:
    - './gradlew projects --dependency-verification strict --console=plain --stacktrace'
    - 'git ls-files "forensic-analytics-*"'
    - 'git diff --check'
  required:
    - './gradlew clean test jacocoTestReport jacocoTestCoverageVerification checkPackageCoverage --dependency-verification strict --console=plain --stacktrace'
documentation:
  arc42: checked
  adr: checked
stop_conditions:
  - git ls-files "forensic-analytics-*" returns tracked files without ADR-backed retained purpose
  - full local quality gate fails
  - docs/workflow and arc42 disagree about the final build topology
```

Purpose: record final verification evidence and prepare handoff for commit,
push or PR only after the full local quality gate passes.

## Dependency Graph

```text
S00 -> S01
S01 -> S02
S01 -> S03
S02 + S03 -> S04
S04 -> S05
S05 -> S06
S06 -> S07
```

S02 and S03 may run in parallel after S01 only if S3D confirms disjoint file
locks. S04, S05, S06 and S07 are sequential.

## Role Ownership Map

Detailed role routing is recorded in `docs/workflow/role-ownership.md`.

## Commit And Push Plan

The v2 topology correction may be checkpointed as workflow documentation after
review and verification. Remaining commits and pushes are allowed only when the
active workflow execution protocol and user approval allow them. `push auto` is
out of scope.

## Definition Of Done

- All workflow files are regenerated for the current services-only baseline.
- S00 through S07 are executable, acyclic and have explicit locks, owners,
  quality gates and stop conditions.
- `docs/workflow/context-pack.md` and `docs/workflow/context-pack.json` are
  present and valid.
- arc42 impact is checked and closure obligations are recorded.
- V2 workflow documentation/topology correction verification passes
  `git diff --check`.

## Handoff To Workflow Execute

The next command is `workflow execute`. Execution must start at S00, use this
workflow as the active workflow, route each slice through the configured
subagent or role workflow, and stop instead of guessing when a reference,
contract, service owner, regression owner or quality command cannot be verified.
